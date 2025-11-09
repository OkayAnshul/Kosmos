package com.example.kosmos.features.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kosmos.shared.ui.components.IconButtonStandard
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import com.example.kosmos.shared.ui.features.gestures.messageGesture

/**
 * Enhanced Message Components for Chat
 *
 * Redesigned message UI with:
 * - Message grouping (consecutive messages from same sender)
 * - Inline reactions
 * - Read receipts
 * - Edit indicators
 * - Quick actions on long-press
 * - Double-tap to quick-react
 *
 * Power user features:
 * - Swipe to reply
 * - Long-press for context menu
 * - Double-tap for quick reaction
 */

/**
 * Message Bubble - Enhanced
 *
 * Displays a single message with all metadata
 * Supports grouping, reactions, read receipts
 *
 * @param text Message text content
 * @param isSentByCurrentUser Whether message is from current user
 * @param timestamp Message timestamp (formatted string)
 * @param senderName Sender's display name (null for current user)
 * @param modifier Modifier
 * @param isFirstInGroup Whether this is first message in group
 * @param isLastInGroup Whether this is last message in group
 * @param isEdited Whether message has been edited
 * @param reactions List of reactions with counts
 * @param readBy Number of users who read this (for sent messages)
 * @param totalRecipients Total recipients (for read receipt calculation)
 * @param onMessageClick Message tap handler
 * @param onQuickReact Quick react handler (double-tap)
 * @param onShowMenu Show context menu handler (long-press)
 * @param onReactionClick Reaction click handler
 */
@Composable
fun EnhancedMessageBubble(
    text: String,
    isSentByCurrentUser: Boolean,
    timestamp: String,
    modifier: Modifier = Modifier,
    senderName: String? = null,
    isFirstInGroup: Boolean = true,
    isLastInGroup: Boolean = true,
    isEdited: Boolean = false,
    reactions: List<MessageReaction> = emptyList(),
    readBy: Int = 0,
    totalRecipients: Int = 1,
    replyToSenderName: String? = null,
    onMessageClick: () -> Unit = {},
    onQuickReact: () -> Unit = {},
    onShowMenu: () -> Unit = {},
    onReactionClick: (String) -> Unit = {},
    onReplyClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isSentByCurrentUser) Tokens.Spacing.xl else Tokens.Spacing.md,
                end = if (isSentByCurrentUser) Tokens.Spacing.md else Tokens.Spacing.xl,
                top = if (isFirstInGroup) Tokens.Spacing.sm else Tokens.Spacing.xxs,
                bottom = if (isLastInGroup) Tokens.Spacing.sm else Tokens.Spacing.xxs
            ),
        horizontalAlignment = if (isSentByCurrentUser) Alignment.End else Alignment.Start
    ) {
        // Sender name (only for first message in group from others)
        if (isFirstInGroup && !isSentByCurrentUser && senderName != null) {
            Text(
                text = senderName,
                style = TypographyTokens.Custom.messageSender,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = Tokens.Spacing.md,
                    bottom = Tokens.Spacing.xxs
                )
            )
        }

        // Message bubble
        Surface(
            modifier = Modifier.messageGesture(
                onClick = onMessageClick,
                onQuickReact = onQuickReact,
                onShowMenu = onShowMenu
            ),
            shape = getMessageShape(isSentByCurrentUser, isFirstInGroup, isLastInGroup),
            color = if (isSentByCurrentUser)
                ColorTokens.Message.sentLight
            else
                ColorTokens.Message.receivedLight,
            tonalElevation = if (isSentByCurrentUser) Tokens.Elevation.level1 else Tokens.Elevation.level0
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = Tokens.Spacing.md,
                    vertical = Tokens.Spacing.sm
                )
            ) {
                // Thread indicator (if replying)
                if (replyToSenderName != null) {
                    ThreadIndicator(
                        replyToSenderName = replyToSenderName,
                        onClick = onReplyClick,
                        modifier = Modifier.padding(bottom = Tokens.Spacing.xs)
                    )
                }

                // Message text
                Text(
                    text = text,
                    style = TypographyTokens.Custom.messageBubbleText,
                    color = if (isSentByCurrentUser)
                        ColorTokens.Message.onSentLight
                    else
                        ColorTokens.Message.onReceivedLight
                )

                // Metadata row (only on last message in group)
                if (isLastInGroup) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Tokens.Spacing.xxs),
                        horizontalArrangement = if (isSentByCurrentUser)
                            Arrangement.End
                        else
                            Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Edited indicator
                        if (isEdited) {
                            Text(
                                text = "Edited",
                                style = TypographyTokens.Custom.messageTimestamp,
                                color = if (isSentByCurrentUser)
                                    ColorTokens.Message.onSentLight.copy(alpha = 0.7f)
                                else
                                    ColorTokens.Message.onReceivedLight.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(Tokens.Spacing.xs))
                        }

                        // Timestamp
                        Text(
                            text = timestamp,
                            style = TypographyTokens.Custom.messageTimestamp,
                            color = if (isSentByCurrentUser)
                                ColorTokens.Message.onSentLight.copy(alpha = 0.7f)
                            else
                                ColorTokens.Message.onReceivedLight.copy(alpha = 0.7f)
                        )

                        // Read receipts (for sent messages)
                        if (isSentByCurrentUser) {
                            Spacer(modifier = Modifier.width(Tokens.Spacing.xxs))
                            ReadReceiptIndicator(
                                readBy = readBy,
                                totalRecipients = totalRecipients
                            )
                        }
                    }
                }
            }
        }

        // Reactions (if any)
        if (reactions.isNotEmpty()) {
            ReactionBar(
                reactions = reactions,
                onReactionClick = onReactionClick,
                modifier = Modifier.padding(top = Tokens.Spacing.xxs)
            )
        }
    }
}

/**
 * Get message bubble shape based on position in group
 */
private fun getMessageShape(
    isSentByUser: Boolean,
    isFirst: Boolean,
    isLast: Boolean
): RoundedCornerShape {
    val defaultRadius = 16.dp
    val tailRadius = 4.dp

    return when {
        isFirst && isLast -> {
            // Single message (not grouped)
            if (isSentByUser) {
                RoundedCornerShape(
                    topStart = defaultRadius,
                    topEnd = defaultRadius,
                    bottomStart = defaultRadius,
                    bottomEnd = tailRadius
                )
            } else {
                RoundedCornerShape(
                    topStart = defaultRadius,
                    topEnd = defaultRadius,
                    bottomStart = tailRadius,
                    bottomEnd = defaultRadius
                )
            }
        }
        isFirst -> {
            // First in group
            RoundedCornerShape(
                topStart = defaultRadius,
                topEnd = defaultRadius,
                bottomStart = if (isSentByUser) defaultRadius else Tokens.CornerRadius.xs,
                bottomEnd = if (isSentByUser) Tokens.CornerRadius.xs else defaultRadius
            )
        }
        isLast -> {
            // Last in group
            if (isSentByUser) {
                RoundedCornerShape(
                    topStart = defaultRadius,
                    topEnd = Tokens.CornerRadius.xs,
                    bottomStart = defaultRadius,
                    bottomEnd = tailRadius
                )
            } else {
                RoundedCornerShape(
                    topStart = Tokens.CornerRadius.xs,
                    topEnd = defaultRadius,
                    bottomStart = tailRadius,
                    bottomEnd = defaultRadius
                )
            }
        }
        else -> {
            // Middle of group
            RoundedCornerShape(
                topStart = if (isSentByUser) defaultRadius else Tokens.CornerRadius.xs,
                topEnd = if (isSentByUser) Tokens.CornerRadius.xs else defaultRadius,
                bottomStart = if (isSentByUser) defaultRadius else Tokens.CornerRadius.xs,
                bottomEnd = if (isSentByUser) Tokens.CornerRadius.xs else defaultRadius
            )
        }
    }
}

/**
 * Message Reaction Data Class
 */
data class MessageReaction(
    val emoji: String,
    val count: Int,
    val reactedByCurrentUser: Boolean
)

/**
 * Reaction Bar
 *
 * Displays emoji reactions below message
 *
 * @param reactions List of reactions
 * @param onReactionClick Reaction click handler
 * @param modifier Modifier
 */
@Composable
fun ReactionBar(
    reactions: List<MessageReaction>,
    onReactionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
    ) {
        reactions.forEach { reaction ->
            ReactionPill(
                emoji = reaction.emoji,
                count = reaction.count,
                isSelected = reaction.reactedByCurrentUser,
                onClick = { onReactionClick(reaction.emoji) }
            )
        }
    }
}

/**
 * Reaction Pill
 *
 * Single reaction with emoji and count
 *
 * @param emoji Emoji character
 * @param count Reaction count
 * @param isSelected Whether current user reacted
 * @param onClick Click handler
 */
@Composable
fun ReactionPill(
    emoji: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (isSelected)
            ColorTokens.Reaction.selectedLight
        else
            ColorTokens.Reaction.backgroundLight,
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(1.dp, ColorTokens.Reaction.selectedLight)
        else null,
        modifier = Modifier.heightIn(min = 24.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Tokens.Spacing.xs,
                vertical = Tokens.Spacing.xxs
            ),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = TypographyTokens.Custom.caption
            )
            Text(
                text = count.toString(),
                style = TypographyTokens.Custom.caption,
                color = if (isSelected)
                    ColorTokens.Reaction.onSelectedLight
                else
                    ColorTokens.Reaction.onBackgroundLight
            )
        }
    }
}

/**
 * Read Receipt Indicator
 *
 * Shows read status for sent messages
 *
 * @param readBy Number of users who read
 * @param totalRecipients Total recipients
 */
@Composable
fun ReadReceiptIndicator(
    readBy: Int,
    totalRecipients: Int
) {
    val icon = when {
        readBy == 0 -> IconSet.Status.checkmark // Sent
        readBy < totalRecipients -> IconSet.Status.checkmark // Delivered
        else -> IconSet.Status.doubleCheck // Read by all
    }

    val tint = if (readBy >= totalRecipients)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Icon(
        imageVector = icon,
        contentDescription = if (readBy >= totalRecipients) "Read" else "Delivered",
        tint = tint,
        modifier = Modifier.size(14.dp)
    )
}

/**
 * Typing Indicator
 *
 * Shows when other users are typing
 *
 * @param typingUsers List of users currently typing
 * @param modifier Modifier
 */
@Composable
fun TypingIndicator(
    typingUsers: List<String>,
    modifier: Modifier = Modifier
) {
    if (typingUsers.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(Tokens.Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
    ) {
        // Animated dots
        com.example.kosmos.shared.ui.components.LoadingDots(
            dotSize = 6.dp
        )

        // Typing text
        val text = when {
            typingUsers.size == 1 -> "${typingUsers[0]} is typing..."
            typingUsers.size == 2 -> "${typingUsers[0]} and ${typingUsers[1]} are typing..."
            else -> "${typingUsers.size} people are typing..."
        }

        Text(
            text = text,
            style = TypographyTokens.Custom.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Date Divider
 *
 * Sticky header showing date for message groups
 *
 * @param date Formatted date string
 * @param modifier Modifier
 */
@Composable
fun DateDivider(
    date: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Tokens.Spacing.md),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = Tokens.Elevation.level1
        ) {
            Text(
                text = date,
                style = TypographyTokens.Custom.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = Tokens.Spacing.md,
                    vertical = Tokens.Spacing.xs
                )
            )
        }
    }
}

/**
 * Jump to Bottom FAB
 *
 * Appears when scrolled up, shows unread count
 *
 * @param unreadCount Number of unread messages
 * @param onClick Click handler
 * @param modifier Modifier
 */
@Composable
fun JumpToBottomFAB(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Tokens.Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (unreadCount > 0) {
                Badge {
                    Text(
                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                        style = TypographyTokens.Custom.badgeNumber
                    )
                }
            }
            Icon(
                imageVector = IconSet.Direction.down,
                contentDescription = "Jump to bottom"
            )
        }
    }
}

/**
 * Reply Preview Bar
 *
 * Shows when replying to a message, displays preview above input
 *
 * @param replyToSenderName Sender name of message being replied to
 * @param replyToContent Content preview of message being replied to
 * @param onCancelReply Cancel reply handler
 * @param modifier Modifier
 */
@Composable
fun ReplyPreviewBar(
    replyToSenderName: String,
    replyToContent: String,
    onCancelReply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = Tokens.Elevation.level1
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reply indicator line
            Surface(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(2.dp)
            ) {}

            Spacer(modifier = Modifier.width(Tokens.Spacing.sm))

            // Reply content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Replying to $replyToSenderName",
                    style = TypographyTokens.Custom.caption,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = replyToContent,
                    style = TypographyTokens.Custom.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Tokens.Spacing.xxs)
                )
            }

            // Cancel button
            IconButtonStandard(
                icon = IconSet.Action.clear,
                onClick = onCancelReply,
                contentDescription = "Cancel reply"
            )
        }
    }
}

/**
 * Thread Indicator
 *
 * Shows small indicator that message is a reply
 *
 * @param replyToSenderName Sender name of original message
 * @param onClick Click to view original message
 * @param modifier Modifier
 */
@Composable
fun ThreadIndicator(
    replyToSenderName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Tokens.Spacing.xs,
                vertical = Tokens.Spacing.xxs
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xxs)
        ) {
            Icon(
                imageVector = IconSet.Message.reply,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = replyToSenderName,
                style = TypographyTokens.Custom.caption,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
