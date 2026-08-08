package com.example.kosmos.features.auth.presentation.redesign

import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.kosmos.BuildConfig
import com.example.kosmos.features.auth.presentation.AuthUiState
import com.example.kosmos.shared.ui.components.LoadingButton
import com.example.kosmos.shared.ui.components.TextButtonStandard
import com.example.kosmos.shared.ui.components.TextFieldPassword
import com.example.kosmos.shared.ui.components.TextFieldStandard
import com.example.kosmos.shared.ui.designsystem.ColorTokens
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    uiState: AuthUiState,
    onLogin: (String, String, Boolean) -> Unit,
    onClearError: () -> Unit,
    onSendPasswordReset: (String) -> Unit = {},
    onClearPasswordResetState: () -> Unit = {},
    getSavedEmail: () -> String = { "" },
    isRememberMeEnabled: () -> Boolean = { false },
    // Called with the Google ID token once Credential Manager resolves
    onGoogleIdToken: (idToken: String, rawNonce: String?) -> Unit = { _, _ -> },
    onEnterDemoMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf(getSavedEmail()) }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(isRememberMeEnabled()) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var googleSignInError by remember { mutableStateOf<String?>(null) }

    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    // Credential Manager Google Sign-In launcher
    fun launchGoogleSignIn() {
        val activityRef = activity ?: return
        scope.launch {
            val credentialManager = CredentialManager.create(activityRef)
            val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID

            // Generate a nonce to protect against replay attacks
            val rawNonce = UUID.randomUUID().toString()
            val hashedNonce = MessageDigest.getInstance("SHA-256")
                .digest(rawNonce.toByteArray())
                .joinToString("") { "%02x".format(it) }

            // Primary option: show accounts already signed into the device
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setNonce(hashedNonce)
                .setAutoSelectEnabled(false)
                .build()

            // Fallback: full account picker if no accounts are found
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .addCredentialOption(signInWithGoogleOption)
                .build()

            try {
                val result = credentialManager.getCredential(activityRef, request)
                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                onGoogleIdToken(googleIdTokenCredential.idToken, rawNonce)
            } catch (e: GetCredentialCancellationException) {
                // User dismissed the picker — no action needed
                Log.d("LoginScreen", "Google sign-in cancelled by user")
            } catch (e: NoCredentialException) {
                Log.w("LoginScreen", "No Google credentials available: ${e.message}")
                googleSignInError = "No Google account found on this device. Add a Google account in Settings and try again."
            } catch (e: Exception) {
                Log.e("LoginScreen", "Google sign-in error: ${e.message}", e)
                googleSignInError = "Google sign-in failed: ${e.message}"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorTokens.ReactTheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Logo / top section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Kosmos",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.ReactTheme.foreground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sign in to continue",
                    fontSize = 14.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
            }

            // Auth error banner
            if (uiState.error != null) {
                Surface(
                    color = ColorTokens.ReactTheme.destructive.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.error,
                        color = ColorTokens.ReactTheme.destructive,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Google sign-in error banner
            if (googleSignInError != null) {
                Surface(
                    color = ColorTokens.ReactTheme.destructive.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = googleSignInError!!,
                            color = ColorTokens.ReactTheme.destructive,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { googleSignInError = null },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("✕", color = ColorTokens.ReactTheme.destructive, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Form card
            Surface(
                color = ColorTokens.ReactTheme.card,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ColorTokens.ReactTheme.border, RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextFieldStandard(
                        value = email,
                        onValueChange = {
                            email = it
                            if (uiState.error != null) onClearError()
                        },
                        label = "Email",
                        keyboardType = KeyboardType.Email,
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextFieldPassword(
                        value = password,
                        onValueChange = {
                            password = it
                            if (uiState.error != null) onClearError()
                        },
                        label = "Password",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ColorTokens.ReactTheme.primary,
                                uncheckedColor = ColorTokens.ReactTheme.mutedForeground
                            )
                        )
                        Text(
                            text = "Remember me",
                            fontSize = 14.sp,
                            color = ColorTokens.ReactTheme.foreground
                        )
                    }

                    LoadingButton(
                        text = "Sign In",
                        onClick = { onLogin(email, password, rememberMe) },
                        isLoading = uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = email.isNotBlank() && password.isNotBlank()
                    )

                    TextButtonStandard(
                        text = "Forgot password?",
                        onClick = { showForgotPassword = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            // Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = ColorTokens.ReactTheme.border
                )
                Text(
                    text = "  or  ",
                    fontSize = 12.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = ColorTokens.ReactTheme.border
                )
            }

            // Google Sign-In button — triggers native Credential Manager bottom sheet
            OutlinedButton(
                onClick = { launchGoogleSignIn() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                border = ButtonDefaults.outlinedButtonBorder().copy(
                    brush = SolidColor(ColorTokens.ReactTheme.border)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ColorTokens.ReactTheme.card,
                    contentColor = ColorTokens.ReactTheme.foreground
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = ColorTokens.ReactTheme.foreground,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Signing in…", fontSize = 14.sp)
                } else {
                    // Google "G" icon — coloured using inline Canvas or text
                    Text(
                        text = "G",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4285F4) // Google blue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continue with Google",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Sign up link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Don't have an account?",
                    fontSize = 14.sp,
                    color = ColorTokens.ReactTheme.mutedForeground
                )
                TextButtonStandard(
                    text = "Sign up",
                    onClick = onNavigateToSignUp
                )
            }

            // Demo mode entry — explore the app fully offline, no account needed
            if (BuildConfig.DEMO_MODE_ENABLED) {
                OutlinedButton(
                    onClick = onEnterDemoMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = SolidColor(ColorTokens.ReactTheme.primary.copy(alpha = 0.5f))
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ColorTokens.ReactTheme.card,
                        contentColor = ColorTokens.ReactTheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !uiState.isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = ColorTokens.ReactTheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Explore Demo Mode",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Footer
            Text(
                text = "Powered by Supabase · by Aravya",
                fontSize = 11.sp,
                color = ColorTokens.ReactTheme.mutedForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Forgot Password Dialog
    if (showForgotPassword) {
        ForgotPasswordDialog(
            uiState = uiState,
            onDismiss = {
                showForgotPassword = false
                onClearPasswordResetState()
            },
            onSendReset = onSendPasswordReset
        )
    }
}

@Composable
private fun ForgotPasswordDialog(
    uiState: AuthUiState,
    onDismiss: () -> Unit,
    onSendReset: (String) -> Unit
) {
    var resetEmail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorTokens.ReactTheme.card,
        title = {
            Text(
                text = "Reset Password",
                color = ColorTokens.ReactTheme.foreground,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (uiState.passwordResetSent) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF3ECF8E),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Reset email sent! Check your inbox.",
                            color = Color(0xFF3ECF8E),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Text(
                        text = "Enter your email address and we'll send you a link to reset your password.",
                        color = ColorTokens.ReactTheme.mutedForeground,
                        fontSize = 14.sp
                    )
                    TextFieldStandard(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = "Email",
                        keyboardType = KeyboardType.Email,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.passwordResetError != null) {
                        Text(
                            text = uiState.passwordResetError,
                            color = ColorTokens.ReactTheme.destructive,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!uiState.passwordResetSent) {
                LoadingButton(
                    text = "Send reset email",
                    onClick = { onSendReset(resetEmail) },
                    isLoading = uiState.isLoading,
                    enabled = resetEmail.isNotBlank()
                )
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = ColorTokens.ReactTheme.primary)
                }
            }
        },
        dismissButton = {
            if (!uiState.passwordResetSent) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = ColorTokens.ReactTheme.mutedForeground)
                }
            }
        }
    )
}
