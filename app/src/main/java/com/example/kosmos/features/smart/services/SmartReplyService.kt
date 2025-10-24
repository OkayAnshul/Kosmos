package com.example.kosmos.features.smart.services

import com.example.kosmos.core.models.Message
import com.example.kosmos.core.models.SmartReply
import com.example.kosmos.core.models.SmartReplyType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

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