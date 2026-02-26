package com.example.kosmos.features.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import com.example.kosmos.shared.ui.components.SearchBarStandard
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens

/**
 * Chat Search Dialog
 *
 * Dialog for searching messages within a chat (wired to ChatViewModel)
 *
 * @param searchResults Search results from ViewModel
 * @param isSearching Whether search is in progress
 * @param onSearchQueryChange Search query change handler (calls ViewModel)
 * @param onDismiss Dismiss handler
 * @param onMessageClick Message click handler (to jump to message)
 */
@Composable
fun ChatSearchDialog(
    searchResults: List<ChatSearchMessage>,
    isSearching: Boolean = false,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onMessageClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Notify ViewModel when search query changes
    LaunchedEffect(searchQuery) {
        onSearchQueryChange(searchQuery)
    }

    val filteredMessages = searchResults

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
            color = ColorTokens.ReactTheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Tokens.Spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Search Messages",
                        style = TypographyTokens.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = IconSet.Navigation.close,
                            contentDescription = "Close"
                        )
                    }
                }

                HorizontalDivider()

                // Search bar
                SearchBarStandard(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search in messages...",
                    modifier = Modifier.padding(Tokens.Spacing.md)
                )

                // Results
                if (searchQuery.isBlank()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Tokens.Spacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                        ) {
                            Icon(
                                imageVector = IconSet.Action.search,
                                contentDescription = "",
                                modifier = Modifier.size(Tokens.Size.iconLarge),
                                tint = ColorTokens.ReactTheme.mutedForeground
                            )
                            Text(
                                text = "Search for messages",
                                style = TypographyTokens.typography.bodyLarge,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        }
                    }
                } else if (filteredMessages.isEmpty()) {
                    // No results
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Tokens.Spacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
                        ) {
                            Icon(
                                imageVector = IconSet.Action.search,
                                contentDescription = "",
                                modifier = Modifier.size(Tokens.Size.iconLarge),
                                tint = ColorTokens.ReactTheme.mutedForeground
                            )
                            Text(
                                text = "No messages found",
                                style = TypographyTokens.typography.bodyLarge,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                            Text(
                                text = "Try a different search term",
                                style = TypographyTokens.typography.bodyMedium,
                                color = ColorTokens.ReactTheme.mutedForeground
                            )
                        }
                    }
                } else {
                    // Results list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Tokens.Spacing.md)
                    ) {
                        items(
                            items = filteredMessages,
                            key = { it.id }
                        ) { message ->
                            ChatSearchResultItem(
                                message = message,
                                searchQuery = searchQuery,
                                onClick = {
                                    onMessageClick(message.id)
                                    onDismiss()
                                }
                            )
                            Spacer(modifier = Modifier.height(Tokens.Spacing.xs))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chat Search Result Item
 */
@Composable
private fun ChatSearchResultItem(
    message: ChatSearchMessage,
    searchQuery: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = ColorTokens.ReactTheme.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Tokens.Spacing.md)
        ) {
            // Sender and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.senderName,
                    style = TypographyTokens.typography.labelLarge,
                    color = ColorTokens.ReactTheme.foreground
                )
                Text(
                    text = message.timestamp,
                    style = TypographyTokens.typography.labelSmall,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }

            Spacer(modifier = Modifier.height(Tokens.Spacing.xs))

            // Message content
            Text(
                text = message.content,
                style = TypographyTokens.typography.bodyMedium,
                color = ColorTokens.ReactTheme.mutedForeground,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Data class for chat search messages
 */
data class ChatSearchMessage(
    val id: String,
    val content: String,
    val senderName: String,
    val timestamp: String
)
