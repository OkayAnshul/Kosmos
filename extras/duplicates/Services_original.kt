package com.example.kosmos

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import com.example.kosmos.database.ActionItemDao
import com.example.kosmos.database.VoiceMessageDao
import com.example.kosmos.models.ActionItem
import com.example.kosmos.models.ActionType
import com.example.kosmos.models.Message
import com.example.kosmos.models.SmartReply
import com.example.kosmos.models.SmartReplyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

interface SpeechToTextService {
    @POST("v1/speech:recognize")
    suspend fun recognizeSpeech(
        @Header("Authorization") authorization: String,
        @Body request: SpeechRecognitionRequest
    ): Response<SpeechRecognitionResponse>
}

data class SpeechRecognitionRequest(
    val config: RecognitionConfig,
    val audio: RecognitionAudio
)

data class RecognitionConfig(
    val encoding: String = "WEBM_OPUS",
    val sampleRateHertz: Int = 16000,
    val languageCode: String = "en-US",
    val enableAutomaticPunctuation: Boolean = true,
    val enableWordTimeOffsets: Boolean = true
)

data class RecognitionAudio(
    val content: String // Base64 encoded audio
)

data class SpeechRecognitionResponse(
    val results: List<SpeechRecognitionResult>
)

data class SpeechRecognitionResult(
    val alternatives: List<SpeechRecognitionAlternative>
)

data class SpeechRecognitionAlternative(
    val transcript: String,
    val confidence: Float
)

@Singleton
class TranscriptionService @Inject constructor(
    private val speechToTextService: SpeechToTextService,
    private val voiceMessageDao: VoiceMessageDao,
    private val context: Context
) {

    private val apiKey = BuildConfig.GOOGLE_CLOUD_API_KEY

    suspend fun transcribeVoiceMessage(voiceMessageId: String, audioFilePath: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Mark as transcribing
                val voiceMessage = voiceMessageDao.getVoiceMessageById(voiceMessageId)
                    ?: return@withContext Result.failure(Exception("Voice message not found"))

                voiceMessageDao.updateVoiceMessage(voiceMessage.copy(isTranscribing = true))

                // Read and encode audio file
                val audioFile = File(audioFilePath)
                if (!audioFile.exists()) {
                    return@withContext Result.failure(Exception("Audio file not found"))
                }

                val audioBytes = audioFile.readBytes()
                val encodedAudio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

                // Prepare request
                val request = SpeechRecognitionRequest(
                    config = RecognitionConfig(
                        encoding = "WEBM_OPUS",
                        sampleRateHertz = 48000,
                        languageCode = "en-US",
                        enableAutomaticPunctuation = true
                    ),
                    audio = RecognitionAudio(content = encodedAudio)
                )

                // Call API
                val response = speechToTextService.recognizeSpeech(
                    authorization = "Bearer $apiKey",
                    request = request
                )

                if (response.isSuccessful) {
                    val results = response.body()?.results
                    if (!results.isNullOrEmpty() && results[0].alternatives.isNotEmpty()) {
                        val transcription = results[0].alternatives[0].transcript
                        val confidence = results[0].alternatives[0].confidence

                        // Update voice message with transcription
                        voiceMessageDao.updateVoiceMessage(
                            voiceMessage.copy(
                                transcription = transcription,
                                transcriptionConfidence = confidence,
                                isTranscribing = false
                            )
                        )

                        Result.success(transcription)
                    } else {
                        voiceMessageDao.updateVoiceMessage(
                            voiceMessage.copy(
                                transcription = "",
                                transcriptionError = "No speech detected",
                                isTranscribing = false
                            )
                        )
                        Result.failure(Exception("No speech detected"))
                    }
                } else {
                    val error = "API Error: ${response.code()}"
                    voiceMessageDao.updateVoiceMessage(
                        voiceMessage.copy(
                            transcriptionError = error,
                            isTranscribing = false
                        )
                    )
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                // Update with error
                voiceMessageDao.getVoiceMessageById(voiceMessageId)?.let { voiceMessage ->
                    voiceMessageDao.updateVoiceMessage(
                        voiceMessage.copy(
                            transcriptionError = e.message ?: "Transcription failed",
                            isTranscribing = false
                        )
                    )
                }
                Result.failure(e)
            }
        }
    }

    suspend fun processPendingTranscriptions() {
        withContext(Dispatchers.IO) {
            try {
                val pendingVoiceMessages = voiceMessageDao.getPendingTranscriptions()
                pendingVoiceMessages.forEach { voiceMessage ->
                    if (voiceMessage.audioUrl.isNotEmpty()) {
                        transcribeVoiceMessage(voiceMessage.id, voiceMessage.audioUrl)
                    }
                }
            } catch (e: Exception) {
                // Log error or handle appropriately
            }
        }
    }
}

@Singleton
class ActionDetectionService @Inject constructor(
    private val actionItemDao: ActionItemDao
) {

    // Improved regex patterns for different action types
    private val taskPatterns = listOf(
        Pattern.compile("(?i)\\b(need to|should|must|have to|remember to|don't forget to)\\s+(.+?)(?=\\.|$|\\n)", Pattern.MULTILINE),
        Pattern.compile("(?i)\\b(todo|to-do|task)\\s*:?\\s*(.+?)(?=\\.|$|\\n)", Pattern.MULTILINE),
        Pattern.compile("(?i)\\b(action item|ai)\\s*:?\\s*(.+?)(?=\\.|$|\\n)", Pattern.MULTILINE),
        Pattern.compile("(?i)\\b(assignment|assign)\\s+(.+?)\\s+to\\s+(.+?)(?=\\.|$|\\n)", Pattern.MULTILINE)
    )

    private val meetingPatterns = listOf(
        Pattern.compile("(?i)\\b(meeting|call|conference|discussion)\\s+(.+?)\\s+(on|at)\\s+(.+?)(?=\\.|$|\\n)", Pattern.MULTILINE),
        Pattern.compile("(?i)\\b(schedule|book)\\s+(.+?)\\s+(for|on|at)\\s+(.+?)(?=\\.|$|\\n)", Pattern.MULTILINE),
        Pattern.compile("(?i)\\b(let's meet|meet up)\\s+(.+?)(?=\\.|$|\\n)", Pattern.MULTILINE)
    )

    private val reminderPatterns = listOf(
        Pattern.compile("(?i)\\b(remind|reminder)\\s+(.+?)\\s+(on|at|by)\\s+(.+?)(?=\\.|$|\\n)", Pattern.MULTILINE),
        Pattern.compile("(?i)\\b(don't forget|remember)\\s+(.+?)(?=\\.|$|\\n)", Pattern.MULTILINE)
    )

    private val deadlinePatterns = listOf(
        Pattern.compile("(?i)\\b(due|deadline|by)\\s+(.+?)(?=\\.|$|\\n)", Pattern.MULTILINE),
        Pattern.compile("(?i)\\b(expires?|ends?)\\s+(on|at)\\s+(.+?)(?=\\.|$|\\n)", Pattern.MULTILINE)
    )

    private val followUpPatterns = listOf(
        Pattern.compile("(?i)\\b(follow up|followup)\\s+(.+?)(?=\\.|$|\\n)", Pattern.MULTILINE),
        Pattern.compile("(?i)\\b(check back|get back to)\\s+(.+?)(?=\\.|$|\\n)", Pattern.MULTILINE),
        Pattern.compile("(?i)\\b(circle back)\\s+(.+?)(?=\\.|$|\\n)", Pattern.MULTILINE)
    )

    suspend fun detectActionsFromText(
        text: String,
        chatRoomId: String,
        messageId: String? = null,
        voiceMessageId: String? = null
    ): List<ActionItem> {
        return withContext(Dispatchers.IO) {
            val detectedActions = mutableListOf<ActionItem>()

            // Clean text for better pattern matching
            val cleanText = text.replace("\\s+".toRegex(), " ").trim()

            // Detect tasks
            taskPatterns.forEach { pattern ->
                val matcher = pattern.matcher(cleanText)
                while (matcher.find()) {
                    val actionText = matcher.group(2)?.trim() ?: continue
                    if (actionText.length > 3 && !isCommonWord(actionText)) {
                        detectedActions.add(
                            createActionItem(
                                ActionType.TASK,
                                actionText,
                                matcher.group(0) ?: actionText,
                                chatRoomId,
                                messageId,
                                voiceMessageId,
                                calculateConfidence(actionText, ActionType.TASK)
                            )
                        )
                    }
                }
            }

            // Detect meetings
            meetingPatterns.forEach { pattern ->
                val matcher = pattern.matcher(cleanText)
                while (matcher.find()) {
                    val actionText = matcher.group(2)?.trim() ?: continue
                    if (actionText.length > 3 && !isCommonWord(actionText)) {
                        detectedActions.add(
                            createActionItem(
                                ActionType.MEETING,
                                actionText,
                                matcher.group(0) ?: actionText,
                                chatRoomId,
                                messageId,
                                voiceMessageId,
                                calculateConfidence(actionText, ActionType.MEETING)
                            )
                        )
                    }
                }
            }

            // Detect reminders
            reminderPatterns.forEach { pattern ->
                val matcher = pattern.matcher(cleanText)
                while (matcher.find()) {
                    val actionText = matcher.group(2)?.trim() ?: continue
                    if (actionText.length > 3 && !isCommonWord(actionText)) {
                        detectedActions.add(
                            createActionItem(
                                ActionType.REMINDER,
                                actionText,
                                matcher.group(0) ?: actionText,
                                chatRoomId,
                                messageId,
                                voiceMessageId,
                                calculateConfidence(actionText, ActionType.REMINDER)
                            )
                        )
                    }
                }
            }

            // Detect deadlines
            deadlinePatterns.forEach { pattern ->
                val matcher = pattern.matcher(cleanText)
                while (matcher.find()) {
                    val actionText = matcher.group(2)?.trim() ?: continue
                    if (actionText.length > 3 && !isCommonWord(actionText)) {
                        detectedActions.add(
                            createActionItem(
                                ActionType.DEADLINE,
                                actionText,
                                matcher.group(0) ?: actionText,
                                chatRoomId,
                                messageId,
                                voiceMessageId,
                                calculateConfidence(actionText, ActionType.DEADLINE)
                            )
                        )
                    }
                }
            }

            // Detect follow-ups
            followUpPatterns.forEach { pattern ->
                val matcher = pattern.matcher(cleanText)
                while (matcher.find()) {
                    val actionText = matcher.group(2)?.trim() ?: continue
                    if (actionText.length > 3 && !isCommonWord(actionText)) {
                        detectedActions.add(
                            createActionItem(
                                ActionType.FOLLOW_UP,
                                actionText,
                                matcher.group(0) ?: actionText,
                                chatRoomId,
                                messageId,
                                voiceMessageId,
                                calculateConfidence(actionText, ActionType.FOLLOW_UP)
                            )
                        )
                    }
                }
            }

            // Remove duplicates and low-confidence items
            val uniqueActions = detectedActions
                .distinctBy { it.text.lowercase() }
                .filter { it.confidence > 0.3f }

            // Save detected actions
            if (uniqueActions.isNotEmpty()) {
                actionItemDao.insertActionItems(uniqueActions)
            }

            uniqueActions
        }
    }

    private fun createActionItem(
        type: ActionType,
        text: String,
        extractedText: String,
        chatRoomId: String,
        messageId: String?,
        voiceMessageId: String?,
        confidence: Float
    ): ActionItem {
        return ActionItem(
            id = UUID.randomUUID().toString(),
            messageId = messageId,
            voiceMessageId = voiceMessageId,
            chatRoomId = chatRoomId,
            type = type,
            text = text,
            extractedText = extractedText,
            confidence = confidence,
            isProcessed = false,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun calculateConfidence(text: String, type: ActionType): Float {
        var confidence = 0.4f

        // Boost confidence based on length
        confidence += when {
            text.length > 50 -> 0.3f
            text.length > 20 -> 0.2f
            text.length > 10 -> 0.1f
            else -> 0.0f
        }

        // Boost confidence based on action-specific keywords
        val keywords = when (type) {
            ActionType.TASK -> listOf("complete", "finish", "implement", "create", "build", "fix", "update", "develop")
            ActionType.MEETING -> listOf("discuss", "review", "present", "demo", "standup", "sync", "call", "zoom")
            ActionType.REMINDER -> listOf("remember", "notify", "alert", "ping", "remind")
            ActionType.DEADLINE -> listOf("urgent", "asap", "priority", "critical", "important", "due")
            ActionType.FOLLOW_UP -> listOf("update", "status", "progress", "check", "follow")
        }

        val matchingKeywords = keywords.count { text.lowercase().contains(it) }
        confidence += matchingKeywords * 0.1f

        // Reduce confidence for very common phrases
        val commonPhrases = listOf("how are you", "thank you", "see you", "talk to you")
        if (commonPhrases.any { text.lowercase().contains(it) }) {
            confidence *= 0.5f
        }

        return confidence.coerceIn(0.1f, 0.95f)
    }

    private fun isCommonWord(text: String): Boolean {
        val commonWords = setOf("it", "that", "this", "what", "when", "where", "why", "how", "the", "a", "an")
        return text.lowercase() in commonWords
    }

    suspend fun processUnprocessedActions(): List<ActionItem> {
        return withContext(Dispatchers.IO) {
            actionItemDao.getUnprocessedActionItems()
        }
    }

    suspend fun markActionAsProcessed(actionItemId: String, taskId: String? = null) {
        withContext(Dispatchers.IO) {
            val actionItem = actionItemDao.getActionItemById(actionItemId)
            actionItem?.let {
                actionItemDao.updateActionItem(
                    it.copy(
                        isProcessed = true,
                        taskId = taskId
                    )
                )
            }
        }
    }
}

@Singleton
class SmartReplyService @Inject constructor() {

    private val generalReplies = listOf(
        SmartReply("Thanks!", 0.8f, SmartReplyType.GENERAL),
        SmartReply("Got it", 0.7f, SmartReplyType.CONFIRMATION),
        SmartReply("Sounds good", 0.8f, SmartReplyType.CONFIRMATION),
        SmartReply("Ok", 0.9f, SmartReplyType.CONFIRMATION),
        SmartReply("👍", 0.7f, SmartReplyType.GENERAL),
        SmartReply("Perfect", 0.6f, SmartReplyType.CONFIRMATION)
    )

    private val questionReplies = listOf(
        SmartReply("I'll check and get back to you", 0.7f, SmartReplyType.QUESTION),
        SmartReply("Let me look into that", 0.8f, SmartReplyType.QUESTION),
        SmartReply("Good question", 0.6f, SmartReplyType.QUESTION),
        SmartReply("I'm not sure, let me find out", 0.7f, SmartReplyType.QUESTION)
    )

    private val taskReplies = listOf(
        SmartReply("I'll take care of it", 0.8f, SmartReplyType.TASK_RELATED),
        SmartReply("On it!", 0.9f, SmartReplyType.TASK_RELATED),
        SmartReply("I can handle this", 0.7f, SmartReplyType.TASK_RELATED),
        SmartReply("Will do", 0.8f, SmartReplyType.TASK_RELATED),
        SmartReply("Consider it done", 0.6f, SmartReplyType.TASK_RELATED)
    )

    private val meetingReplies = listOf(
        SmartReply("I'll be there", 0.9f, SmartReplyType.MEETING),
        SmartReply("Count me in", 0.8f, SmartReplyType.MEETING),
        SmartReply("What time works best?", 0.7f, SmartReplyType.MEETING),
        SmartReply("Can we reschedule?", 0.6f, SmartReplyType.MEETING),
        SmartReply("I have a conflict, sorry", 0.5f, SmartReplyType.MEETING)
    )

    suspend fun generateSmartReplies(
        messages: List<Message>,
        currentUserId: String
    ): List<SmartReply> {
        return withContext(Dispatchers.IO) {
            if (messages.isEmpty()) return@withContext emptyList()

            val lastMessage = messages.first() // Messages are in reverse order
            if (lastMessage.senderId == currentUserId) {
                return@withContext emptyList() // Don't suggest replies to own messages
            }

            val messageText = lastMessage.content.lowercase()
            val suggestedReplies = mutableListOf<SmartReply>()

            // Analyze message content and context
            val isQuestion = messageText.contains("?") ||
                    messageText.contains("what") ||
                    messageText.contains("when") ||
                    messageText.contains("where") ||
                    messageText.contains("who") ||
                    messageText.contains("why") ||
                    messageText.contains("how")

            val isTaskRelated = messageText.contains("task") ||
                    messageText.contains("todo") ||
                    messageText.contains("need to") ||
                    messageText.contains("should") ||
                    messageText.contains("must") ||
                    messageText.contains("have to")

            val isMeetingRelated = messageText.contains("meeting") ||
                    messageText.contains("call") ||
                    messageText.contains("schedule") ||
                    messageText.contains("discuss")

            val requiresConfirmation = messageText.contains("ok?") ||
                    messageText.contains("agree?") ||
                    messageText.contains("sound good?") ||
                    messageText.contains("works?")

            // Generate context-appropriate replies
            when {
                requiresConfirmation -> {
                    suggestedReplies.addAll(
                        generalReplies.filter { it.type == SmartReplyType.CONFIRMATION }
                            .take(2)
                    )
                }
                isQuestion -> {
                    suggestedReplies.addAll(questionReplies.take(2))
                }
                isTaskRelated -> {
                    suggestedReplies.addAll(taskReplies.take(2))
                }
                isMeetingRelated -> {
                    suggestedReplies.addAll(meetingReplies.take(2))
                }
                else -> {
                    suggestedReplies.addAll(generalReplies.take(3))
                }
            }

            // Add a general reply if we don't have enough
            if (suggestedReplies.size < 3) {
                val additionalReplies = generalReplies
                    .filter { reply -> suggestedReplies.none { it.text == reply.text } }
                    .take(3 - suggestedReplies.size)
                suggestedReplies.addAll(additionalReplies)
            }

            // Adjust confidence based on message context
            suggestedReplies.map { reply ->
                val contextBoost = when {
                    isQuestion && reply.type == SmartReplyType.QUESTION -> 0.2f
                    isTaskRelated && reply.type == SmartReplyType.TASK_RELATED -> 0.2f
                    isMeetingRelated && reply.type == SmartReplyType.MEETING -> 0.2f
                    requiresConfirmation && reply.type == SmartReplyType.CONFIRMATION -> 0.2f
                    else -> 0.0f
                }

                reply.copy(confidence = (reply.confidence + contextBoost).coerceIn(0.1f, 0.95f))
            }.sortedByDescending { it.confidence }.take(3)
        }
    }

    suspend fun generateContextualReply(
        prompt: String,
        conversationContext: List<Message>
    ): SmartReply? {
        return withContext(Dispatchers.IO) {
            // This is a simple implementation
            // In a production app, you might integrate with a more sophisticated NLP service
            // or use on-device ML models for better contextual understanding

            val promptLower = prompt.lowercase()

            when {
                promptLower.contains("thank") -> SmartReply("You're welcome!", 0.9f, SmartReplyType.GENERAL)
                promptLower.contains("sorry") -> SmartReply("No problem", 0.8f, SmartReplyType.GENERAL)
                promptLower.contains("help") -> SmartReply("I'd be happy to help", 0.8f, SmartReplyType.GENERAL)
                promptLower.contains("urgent") -> SmartReply("I'll prioritize this", 0.9f, SmartReplyType.TASK_RELATED)
                promptLower.contains("asap") -> SmartReply("Working on it now", 0.9f, SmartReplyType.TASK_RELATED)
                else -> null
            }
        }
    }
}

class VoiceRecordingHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false

    fun startRecording(): Flow<VoiceRecordingState> = callbackFlow {
        try {
            // Create output directory if it doesn't exist
            val outputDir = File(context.cacheDir, "voice_recordings")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            outputFile = File(outputDir, "recording_${System.currentTimeMillis()}.m4a")

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)

                try {
                    prepare()
                    start()
                    isRecording = true
                    trySend(VoiceRecordingState.Recording(outputFile!!))
                } catch (e: IOException) {
                    trySend(VoiceRecordingState.Error("Failed to start recording: ${e.message}"))
                    close(e)
                }
            }

        } catch (e: Exception) {
            trySend(VoiceRecordingState.Error("Recording setup failed: ${e.message}"))
            close(e)
        }

        awaitClose {
            stopRecording()
        }
    }

    fun stopRecording(): VoiceRecordingState {
        return try {
            if (isRecording && mediaRecorder != null) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                isRecording = false

                outputFile?.let { file ->
                    if (file.exists() && file.length() > 0) {
                        VoiceRecordingState.Completed(file)
                    } else {
                        VoiceRecordingState.Error("Recording file is empty or doesn't exist")
                    }
                } ?: VoiceRecordingState.Error("No output file")
            } else {
                VoiceRecordingState.Error("Not currently recording")
            }
        } catch (e: Exception) {
            try {
                mediaRecorder?.release()
            } catch (releaseException: Exception) {
                // Ignore release exceptions
            }
            mediaRecorder = null
            isRecording = false
            VoiceRecordingState.Error("Failed to stop recording: ${e.message}")
        }
    }

    fun cancelRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.stop()
            }
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false

            // Delete the recording file
            outputFile?.delete()
            outputFile = null
        } catch (e: Exception) {
            // Handle silently - just ensure cleanup
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            outputFile?.delete()
            outputFile = null
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}

sealed class VoiceRecordingState {
    data class Recording(val outputFile: File) : VoiceRecordingState()
    data class Completed(val audioFile: File) : VoiceRecordingState()
    data class Error(val message: String) : VoiceRecordingState()
}