package com.example.kosmos.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

/**
 * Custom serializer for reactions field that handles both array and object formats from Supabase
 */
object ReactionsSerializer : KSerializer<Map<String, String>> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Reactions", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Map<String, String> {
        return try {
            val jsonDecoder = decoder as? JsonDecoder
                ?: return emptyMap()

            val element = jsonDecoder.decodeJsonElement()

            when {
                element is JsonArray -> emptyMap() // Empty array [] -> empty map
                element is JsonObject -> {
                    element.entries.associate { (key, value) ->
                        key to value.jsonPrimitive.content
                    }
                }
                else -> emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, String>) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This serializer can only be used with Json format")

        // Always serialize as object for consistency
        val jsonObject = buildJsonObject {
            value.forEach { (key, v) -> put(key, JsonPrimitive(v)) }
        }
        jsonEncoder.encodeJsonElement(jsonObject)
    }
}

@Serializable
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val id: String = "",

    @SerialName("chat_room_id")
    val chatRoomId: String = "",

    @SerialName("sender_id")
    val senderId: String = "",

    @SerialName("sender_name")
    val senderName: String = "",

    @SerialName("sender_photo_url")
    val senderPhotoUrl: String? = null,

    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,

    @SerialName("voice_message_id")
    val voiceMessageId: String? = null,

    @SerialName("task_ids")
    val taskIds: List<String> = emptyList(), // Tasks created from this message

    @SerialName("reply_to_message_id")
    val replyToMessageId: String? = null,

    @SerialName("is_edited")
    val isEdited: Boolean = false,

    @SerialName("edited_at")
    val editedAt: Long? = null,

    @Serializable(with = ReactionsSerializer::class)
    val reactions: Map<String, String> = emptyMap(), // userId -> emoji

    @SerialName("read_by")
    val readBy: List<String> = emptyList() // userIds who read this message
)

@Serializable
enum class MessageType {
    TEXT, VOICE, IMAGE, FILE, SYSTEM, TASK_CREATED
}