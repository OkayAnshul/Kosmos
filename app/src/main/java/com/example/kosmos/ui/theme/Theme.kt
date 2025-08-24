// Color.kt
package com.example.kosmos.ui.theme

import androidx.compose.ui.graphics.Color

import android.app.Activity
import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.kosmos.R
import com.example.kosmos.models.Task
import com.example.kosmos.models.TaskPriority
import com.example.kosmos.models.TaskStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// Primary colors - Modern blue with good contrast
val Primary = Color(0xFF2196F3)
val PrimaryVariant = Color(0xFF1976D2)
val Secondary = Color(0xFF03DAC6)
val SecondaryVariant = Color(0xFF018786)

// Surface colors
val Surface = Color(0xFFFFFBFE)
val SurfaceVariant = Color(0xFFF3F4F6)
val Background = Color(0xFFFFFBFE)

// Error colors
val Error = Color(0xFFB00020)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF410002)

// Text colors
val OnPrimary = Color(0xFFFFFFFF)
val OnSecondary = Color(0xFF000000)
val OnSurface = Color(0xFF1C1B1F)
val OnBackground = Color(0xFF1C1B1F)

// Dark theme colors
val PrimaryDark = Color(0xFF90CAF9)
val PrimaryVariantDark = Color(0xFF42A5F5)
val SecondaryDark = Color(0xFF03DAC6)
val SecondaryVariantDark = Color(0xFF03DAC6)

val SurfaceDark = Color(0xFF121212)
val SurfaceVariantDark = Color(0xFF2C2C2C)
val BackgroundDark = Color(0xFF121212)

val OnPrimaryDark = Color(0xFF000000)
val OnSecondaryDark = Color(0xFF000000)
val OnSurfaceDark = Color(0xFFE6E1E5)
val OnBackgroundDark = Color(0xFFE6E1E5)


private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    primaryContainer = PrimaryVariantDark,
    secondary = SecondaryDark,
    secondaryContainer = SecondaryVariantDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    error = Error,
    errorContainer = ErrorContainer,
    onPrimary = OnPrimaryDark,
    onSecondary = OnSecondaryDark,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    primaryContainer = PrimaryVariant,
    secondary = Secondary,
    secondaryContainer = SecondaryVariant,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    error = Error,
    errorContainer = ErrorContainer,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = OnBackground,
    onSurface = OnSurface,
    onErrorContainer = OnErrorContainer
)

@Composable
fun KosmosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

//
//// Custom font family (optional - you can remove this and use default fonts)
//val InterFontFamily = FontFamily(
//    Font(R.font.inter_regular, FontWeight.Normal),
//    Font(R.font.inter_medium, FontWeight.Medium),
//    Font(R.font.inter_semibold, FontWeight.SemiBold),
//    Font(R.font.inter_bold, FontWeight.Bold)
//)
//
//val Typography = Typography(
//    displayLarge = TextStyle(
//        fontFamily = InterFontFamily,
//        fontWeight = FontWeight.Bold,
//        fontSize = 57.sp,
//        lineHeight = 64.sp,
//        letterSpacing = (-0.25).sp
//    ),
//    displayMedium = TextStyle(
//        fontFamily = InterFontFamily,
//        fontWeight = FontWeight.Bold,
//        fontSize = 45.sp,
//        lineHeight = 52.sp,
//        letterSpacing = 0.sp
//    ),
//    displaySmall = TextStyle(
//        fontFamily = InterFontFamily,
//        fontWeight = FontWeight.Bold,
//        fontSize = 36.sp,
//        lineHeight = 44.sp,
//        letterSpacing = 0.sp
//    ),
//    headlineLarge = TextStyle(
//        fontFamily = InterFontFamily,
//        fontWeight = FontWeight.SemiBold,
//        fontSize = 32.sp,
//        lineHeight = 40.sp,
//        letterSpacing = 0.sp
//    ),
//    headlineMedium = TextStyle(
//        fontFamily = InterFontFamily,
//        fontWeight = FontWeight.SemiBold,
//        fontSize = 28.sp,
//        lineHeight = 36.sp,
//        letterSpacing = 0.sp
//    ),
//    headlineSmall = TextStyle(
//        fontFamily = InterFontFamily,
//        fontWeight = FontWeight.SemiBold,
//        fontSize = 24.sp,
//        lineHeight = 32.sp,
//        letterSpacing = 0.sp
//    ),
//    titleLarge = TextStyle(
//        fontFamily = InterFontFamily,
//        fontWeight = FontWeight.SemiBold,
//        fontSize = 22.sp,
//        lineHeight = 28.sp,
//        letterSpacing = 0.sp
//    ),
//    titleMedium = TextStyle(
//        fontFamily = InterFontFamily,
//        fontWeight = FontWeight.Medium,
//        fontSize = 16.sp,
//        lineHeight = 24.sp,
//        letterSpacing = 0.15.sp
//    ),
//    titleSmall = TextStyle(
//        fontFamily = InterFontFamily,
//        fontWeight = FontWeight.Medium,
//        fontSize = 14.sp,
//        lineHeight = 20.sp,
//        letterSpacing = 0.1.sp
//    ),
//    bodyLarge = TextStyle(
//        fontFamily = InterFontFamily,
//        fontWeight = FontWeight.Normal,
//        fontSize = 16.sp,
//        lineHeight = 24.sp,
//        letterSpacing = 0.5.sp
//    ),
//    bodyMedium = TextStyle(
//        fontFamily = InterFontFamily,
//        fontWeight = FontWeight.Normal,
//        fontSize = 14.sp,
//        lineHeight = 20.sp,
//        letterSpacing = 0.25.sp
//    ),
//    bodySmall = TextStyle(
//        fontFamily = InterFontFamily,
//        fontWeight = FontWeight.Normal,
//        fontSize = 12.sp,
//        lineHeight = 16.sp,
//        letterSpacing = 0.4.sp
//    ),
//    labelLarge = TextStyle(
//        fontFamily = InterFontFamily,
//        fontWeight = FontWeight.Medium,
//        fontSize = 14.sp,
//        lineHeight = 20.sp,
//        letterSpacing = 0.1.sp
//    ),
//    labelMedium = TextStyle(
//        fontFamily = InterFontFamily,
//        fontWeight = FontWeight.Medium,
//        fontSize = 12.sp,
//        lineHeight = 16.sp,
//        letterSpacing = 0.5.sp
//    ),
//    labelSmall = TextStyle(
//        fontFamily = InterFontFamily,
//        fontWeight = FontWeight.Medium,
//        fontSize = 11.sp,
//        lineHeight = 16.sp,
//        letterSpacing = 0.5.sp
//    )
//)

@Composable
fun LoadingDots(
    modifier: Modifier = Modifier,
    dotColor: Color = MaterialTheme.colorScheme.primary,
    dotSize: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_dots")

    val animationValues = (0..2).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 200)
            ),
            label = "dot_$index"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        animationValues.forEach { animatedValue ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = animatedValue.value))
            )
        }
    }
}

@Composable
fun MessageShimmer(
    modifier: Modifier = Modifier,
    isFromCurrentUser: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromCurrentUser) {
            // Avatar shimmer
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Message bubble shimmer
        Column(
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .width((100..200).random().dp)
                    .height(40.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                            bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                        )
                    )
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            )
        }

        if (isFromCurrentUser) {
            Spacer(modifier = Modifier.width(40.dp))
        }
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            if (onRetry != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun NetworkErrorBanner(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Connection lost. Some features may not work properly.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Dismiss", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun PermissionRequestDialog(
    title: String,
    description: String,
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(onClick = onGrantPermission) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        },
        modifier = modifier
    )
}

@Composable
fun MicrophonePermissionCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Voice Messages",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Grant microphone access to send voice messages and use AI transcription features",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Enable Voice Messages")
            }
        }
    }
}

@Composable
fun VoiceRecordingIndicator(
    isRecording: Boolean,
    duration: Long = 0L,
    modifier: Modifier = Modifier
) {
    if (isRecording) {
        Row(
            modifier = modifier
                .background(
                    MaterialTheme.colorScheme.errorContainer,
                    shape = CircleShape
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing recording indicator
            val infiniteTransition = rememberInfiniteTransition(label = "recording")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier
                    .scale(scale)
                    .size(16.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Animated waveform dots
            LoadingDots(
                dotColor = MaterialTheme.colorScheme.error,
                dotSize = 4.dp
            )
        }
    }
}

@Composable
fun AudioWaveform(
    waveformData: List<Float>,
    isPlaying: Boolean,
    currentPosition: Float = 0f,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
) {
    Row(
        modifier = modifier.height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        waveformData.forEachIndexed { index, amplitude ->
            val isActive = index / waveformData.size.toFloat() <= currentPosition
            val height = (amplitude * 24).dp.coerceAtLeast(4.dp)

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height)
                    .clip(CircleShape)
                    .background(if (isActive) activeColor else inactiveColor)
            )
        }
    }
}

@Composable
fun VoiceMessagePlaybackControls(
    isPlaying: Boolean,
    duration: Long,
    currentPosition: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${formatDuration(currentPosition)} / ${formatDuration(duration)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}

@Composable
fun TaskCard(
    task: Task,
    onTaskClick: (Task) -> Unit,
    onStatusToggle: (Task) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onTaskClick(task) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                if (isCompact) 12.dp else 16.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status checkbox
                IconButton(
                    onClick = { onStatusToggle(task) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (task.status == TaskStatus.DONE) Icons.Default.CheckCircle else Icons.Default.Circle,
                        contentDescription = null,
                        tint = if (task.status == TaskStatus.DONE) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Title
                Text(
                    text = task.title,
                    style = if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.status == TaskStatus.DONE) TextDecoration.LineThrough else null,
                    color = if (task.status == TaskStatus.DONE) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = if (isCompact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Priority indicator
                PriorityChip(
                    priority = task.priority,
                    size = if (isCompact) ChipSize.Small else ChipSize.Medium
                )
            }

            if (!isCompact && task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (task.dueDate != null || task.assignedToName != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Due date
                    if (task.dueDate != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatDueDate(task.dueDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (task.dueDate < System.currentTimeMillis() && task.status != TaskStatus.DONE) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                }
                            )
                        }
                    }

                    // Assignee
                    if (task.assignedToName != null) {
                        StatusChip(
                            text = task.assignedToName,
                            size = ChipSize.Small
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityChip(
    priority: TaskPriority,
    size: ChipSize = ChipSize.Medium,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (priority) {
        TaskPriority.LOW -> Color(0xFF4CAF50) to "Low"
        TaskPriority.MEDIUM -> Color(0xFFFF9800) to "Med"
        TaskPriority.HIGH -> Color(0xFFFF5722) to "High"
        TaskPriority.URGENT -> Color(0xFFD32F2F) to "!"
    }

    val chipModifier = when (size) {
        ChipSize.Small -> modifier.height(20.dp)
        ChipSize.Medium -> modifier.height(24.dp)
        ChipSize.Large -> modifier.height(32.dp)
    }

    Box(
        modifier = chipModifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = when (size) {
                ChipSize.Small -> MaterialTheme.typography.labelSmall
                ChipSize.Medium -> MaterialTheme.typography.labelMedium
                ChipSize.Large -> MaterialTheme.typography.labelLarge
            },
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusChip(
    text: String,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: ChipSize = ChipSize.Medium,
    modifier: Modifier = Modifier
) {
    val chipModifier = when (size) {
        ChipSize.Small -> modifier.height(20.dp)
        ChipSize.Medium -> modifier.height(24.dp)
        ChipSize.Large -> modifier.height(32.dp)
    }

    Box(
        modifier = chipModifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = when (size) {
                ChipSize.Small -> MaterialTheme.typography.labelSmall
                ChipSize.Medium -> MaterialTheme.typography.labelMedium
                ChipSize.Large -> MaterialTheme.typography.labelLarge
            },
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

enum class ChipSize {
    Small, Medium, Large
}

private fun formatDueDate(dueDate: Long): String {
    val now = System.currentTimeMillis()
    val diff = dueDate - now

    return when {
        diff < 0 -> "Overdue"
        diff < 24 * 60 * 60 * 1000 -> "Today"
        diff < 2 * 24 * 60 * 60 * 1000 -> "Tomorrow"
        diff < 7 * 24 * 60 * 60 * 1000 -> {
            val days = (diff / (24 * 60 * 60 * 1000)).toInt() + 1
            "${days}d"
        }
        else -> {
            SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(dueDate))
        }
    }
}