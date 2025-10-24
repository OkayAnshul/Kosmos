package com.example.kosmos.features.smart.services

import com.example.kosmos.core.database.dao.ActionItemDao
import com.example.kosmos.core.models.ActionItem
import com.example.kosmos.core.models.ActionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

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