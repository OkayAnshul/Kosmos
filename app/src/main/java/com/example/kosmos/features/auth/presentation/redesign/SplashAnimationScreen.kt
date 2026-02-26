package com.example.kosmos.features.auth.presentation.redesign

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.example.kosmos.core.config.AppConfig
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashAnimationScreen(
    isAuthReady: Boolean,
    onFinished: () -> Unit,
    appConfig: AppConfig = AppConfig()
) {
    val wordmarkAlpha = remember { Animatable(0f) }
    val wordmarkOffset = remember { Animatable(20f) }
    val taglineAlpha = remember { Animatable(0f) }
    val dividerFraction = remember { Animatable(0f) }
    val attributionAlpha = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    // Tracks whether the branded animation itself is complete
    var animationDone by remember { mutableStateOf(false) }

    // Run once auth is ready AND animation is done → fade out and finish
    LaunchedEffect(isAuthReady, animationDone) {
        if (isAuthReady && animationDone) {
            screenAlpha.animateTo(0f, tween(300))
            onFinished()
        }
    }

    LaunchedEffect(Unit) {
        // Step 1: Wordmark / logo (0–400ms)
        coroutineScope {
            launch { wordmarkAlpha.animateTo(1f, tween(400)) }
            launch { wordmarkOffset.animateTo(0f, tween(400)) }
        }

        // Step 2: Tagline (400–700ms)
        taglineAlpha.animateTo(1f, tween(300))

        // Step 3: Divider (700–1000ms)
        dividerFraction.animateTo(1f, tween(300))

        // Step 4: Attribution (1000–1200ms)
        attributionAlpha.animateTo(1f, tween(200))

        // Minimum hold (ensures splash is visible even if auth was instant)
        delay(200)

        animationDone = true
        // Fade-out is handled by the LaunchedEffect above once isAuthReady is also true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
            .alpha(screenAlpha.value),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.offset(y = wordmarkOffset.value.dp)
        ) {
            // Show remote logo image if configured, otherwise fall back to text wordmark
            if (appConfig.logoUrl.isNotEmpty()) {
                AsyncImage(
                    model = appConfig.logoUrl,
                    contentDescription = appConfig.appName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(72.dp)
                        .alpha(wordmarkAlpha.value)
                )
            } else {
                Text(
                    text = appConfig.appName,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground,
                    modifier = Modifier.alpha(wordmarkAlpha.value)
                )
            }

            Text(
                text = appConfig.tagline,
                fontSize = 14.sp,
                color = ColorTokens.ReactTheme.mutedForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha.value)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .alpha(dividerFraction.value)
                    .width((48 * dividerFraction.value).dp)
                    .height(1.dp)
                    .background(ColorTokens.ReactTheme.primary)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(attributionAlpha.value)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFF3ECF8E), shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Powered by Supabase",
                    fontSize = 12.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }
            Text(
                text = "by Aravya",
                fontSize = 12.sp,
                color = ColorTokens.ReactTheme.mutedForeground
            )
        }
    }
}
