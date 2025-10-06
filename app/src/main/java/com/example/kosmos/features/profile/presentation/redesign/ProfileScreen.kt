package com.example.kosmos.features.profile.presentation.redesign

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.kosmos.core.models.User
import com.example.kosmos.shared.ui.designsystem.ColorTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User?,
    activeProjectCount: Int = 0,
    onTimeRate: Int = 0,
    onNavigateBack: () -> Unit,
    onEditProfileClick: () -> Unit,
    onPrivacySettingsClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        TopAppBar(
            title = { Text("Profile", color = ColorTokens.ReactTheme.foreground, fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = ColorTokens.ReactTheme.foreground)
                }
            },
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, "Settings", tint = ColorTokens.ReactTheme.foreground)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorTokens.ReactTheme.card)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Avatar + Name + Role ──────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(ColorTokens.ReactTheme.primary)
                        .border(3.dp, ColorTokens.ReactTheme.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user?.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(user!!.photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = user?.displayName?.take(2)?.uppercase()
                                ?: user?.email?.take(2)?.uppercase() ?: "?",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Online dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3ECF8E))
                    )
                    Text(
                        text = "Online",
                        fontSize = 12.sp,
                        color = Color(0xFF3ECF8E)
                    )
                }

                // Display name
                Text(
                    text = user?.displayName?.ifBlank { null } ?: user?.email?.substringBefore("@") ?: "—",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTokens.ReactTheme.foreground,
                    textAlign = TextAlign.Center
                )

                // Username
                Text(
                    text = if (!user?.username.isNullOrBlank()) "@${user!!.username}" else "No username set",
                    fontSize = 14.sp,
                    color = ColorTokens.ReactTheme.mutedForeground,
                    textAlign = TextAlign.Center
                )

                // Role + Location on same row (if either exists)
                val roleText = user?.role?.ifBlank { null }
                val locationText = user?.location?.ifBlank { null }
                if (roleText != null || locationText != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (roleText != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Work, null, tint = ColorTokens.ReactTheme.mutedForeground, modifier = Modifier.size(14.dp))
                                Text(roleText, fontSize = 13.sp, color = ColorTokens.ReactTheme.mutedForeground)
                            }
                        }
                        if (roleText != null && locationText != null) {
                            Text("·", fontSize = 13.sp, color = ColorTokens.ReactTheme.mutedForeground)
                        }
                        if (locationText != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = ColorTokens.ReactTheme.mutedForeground, modifier = Modifier.size(14.dp))
                                Text(locationText, fontSize = 13.sp, color = ColorTokens.ReactTheme.mutedForeground)
                            }
                        }
                    }
                }

                // Edit Profile button
                OutlinedButton(
                    onClick = onEditProfileClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(ColorTokens.ReactTheme.border)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ColorTokens.ReactTheme.card,
                        contentColor = ColorTokens.ReactTheme.foreground
                    )
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit Profile", fontWeight = FontWeight.Medium)
                }
            }

            // ── Stats ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Active Projects", activeProjectCount.toString(), Modifier.weight(1f))
                StatCard("Age", user?.age?.toString() ?: "—", Modifier.weight(1f))
                StatCard("On-time", "$onTimeRate%", Modifier.weight(1f), Color(0xFF3ECF8E))
            }

            // ── Bio ───────────────────────────────────────────────────────
            ProfileSection(title = "About") {
                val bio = user?.bio?.ifBlank { null }
                if (bio != null) {
                    Text(
                        text = bio,
                        fontSize = 14.sp,
                        color = ColorTokens.ReactTheme.foreground,
                        lineHeight = 20.sp
                    )
                } else {
                    EmptyFieldRow(
                        label = "Add a bio",
                        icon = Icons.Default.Person,
                        onClick = onEditProfileClick
                    )
                }
            }

            // ── Contact ───────────────────────────────────────────────────
            ProfileSection(title = "Contact") {
                // Email (always shown)
                InfoRow(
                    icon = Icons.Default.Email,
                    iconTint = ColorTokens.ReactTheme.primary,
                    label = "Email",
                    value = user?.email?.ifBlank { null } ?: "—"
                )
            }

            // ── Work & Identity ───────────────────────────────────────────
            ProfileSection(title = "Work & Identity") {
                val role = user?.role?.ifBlank { null }
                val location = user?.location?.ifBlank { null }

                if (role != null) {
                    InfoRow(Icons.Default.Work, ColorTokens.ReactTheme.primary, "Role / Title", role)
                } else {
                    EmptyFieldRow("Add your job title", Icons.Default.Work, onEditProfileClick)
                }

                HorizontalDivider(color = ColorTokens.ReactTheme.border.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                if (location != null) {
                    InfoRow(Icons.Default.LocationOn, Color(0xFF3ECF8E), "Location", location)
                } else {
                    EmptyFieldRow("Add your location", Icons.Default.LocationOn, onEditProfileClick)
                }
            }

            // ── Social Links ──────────────────────────────────────────────
            ProfileSection(title = "Links") {
                data class LinkEntry(val icon: ImageVector, val tint: Color, val label: String, val url: String?)
                val links = listOf(
                    LinkEntry(Icons.Default.Code,    Color(0xFFE8E8ED), "GitHub",      user?.githubUrl?.ifBlank { null }),
                    LinkEntry(Icons.Default.Share,   Color(0xFF1DA1F2), "Twitter / X", user?.twitterUrl?.ifBlank { null }),
                    LinkEntry(Icons.Default.Business,Color(0xFF0A66C2), "LinkedIn",    user?.linkedinUrl?.ifBlank { null }),
                    LinkEntry(Icons.Default.Language,Color(0xFF7C3AED), "Website",     user?.websiteUrl?.ifBlank { null }),
                    LinkEntry(Icons.Default.Web,     Color(0xFFE8E8ED), "Portfolio",   user?.portfolioUrl?.ifBlank { null }),
                )

                val populated = links.filter { it.url != null }
                if (populated.isEmpty()) {
                    EmptyFieldRow("Add social links", Icons.Default.Link, onEditProfileClick)
                } else {
                    populated.forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider(color = ColorTokens.ReactTheme.border.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                        LinkRow(entry.icon, entry.tint, entry.label, entry.url!!) {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.url))) }
                        }
                    }
                }
            }

            // ── Settings ──────────────────────────────────────────────────
            ProfileSection(title = "Settings") {
                SettingsRow(Icons.Default.Lock, "Privacy Settings", onPrivacySettingsClick)
                HorizontalDivider(color = ColorTokens.ReactTheme.border.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                SettingsRow(Icons.Default.Notifications, "Notifications", onNotificationSettingsClick)
                HorizontalDivider(color = ColorTokens.ReactTheme.border.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                SettingsRow(Icons.Default.ExitToApp, "Logout", onLogoutClick, tint = ColorTokens.ReactTheme.destructive)
            }

            // Footer
            Text(
                "Kosmos · by Aravya",
                fontSize = 11.sp,
                color = ColorTokens.ReactTheme.mutedForeground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.ReactTheme.mutedForeground,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            color = ColorTokens.ReactTheme.card,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ColorTokens.ReactTheme.border, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier, valueColor: Color = ColorTokens.ReactTheme.primary) {
    Surface(
        color = ColorTokens.ReactTheme.card,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, ColorTokens.ReactTheme.border, RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Text(label, fontSize = 10.sp, color = ColorTokens.ReactTheme.mutedForeground, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, iconTint: Color, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = ColorTokens.ReactTheme.mutedForeground)
            Text(value, fontSize = 14.sp, color = ColorTokens.ReactTheme.foreground, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LinkRow(icon: ImageVector, iconTint: Color, label: String, url: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = ColorTokens.ReactTheme.mutedForeground)
            Text(
                text = url.removePrefix("https://").removePrefix("http://").trimEnd('/'),
                fontSize = 13.sp,
                color = ColorTokens.ReactTheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(Icons.Default.OpenInNew, null, tint = ColorTokens.ReactTheme.mutedForeground, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun EmptyFieldRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Icon(icon, null, tint = ColorTokens.ReactTheme.mutedForeground, modifier = Modifier.size(16.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = ColorTokens.ReactTheme.mutedForeground,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.Add, null, tint = ColorTokens.ReactTheme.primary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = ColorTokens.ReactTheme.foreground) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 14.sp, color = tint, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Icon(Icons.Default.ChevronRight, null, tint = ColorTokens.ReactTheme.mutedForeground, modifier = Modifier.size(18.dp))
    }
}
