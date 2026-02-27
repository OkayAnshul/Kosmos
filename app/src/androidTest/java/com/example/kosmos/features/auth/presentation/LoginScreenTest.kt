package com.example.kosmos.features.auth.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kosmos.shared.ui.theme.KosmosTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the LoginScreen composable.
 *
 * These are fully hermetic — LoginScreen takes all state as plain parameters
 * (no ViewModel injection), so we drive it directly without any Hilt setup.
 *
 * Coverage:
 *  - Static elements (title, subtitle, button) are visible on initial render
 *  - Email and password fields accept input
 *  - Login button triggers onLogin callback with typed credentials
 *  - Progress indicator shown while loading, button hidden
 *  - Error card shown when uiState.error is set
 *  - Navigate-to-signup button triggers callback
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private fun launchScreen(
        uiState: AuthUiState = AuthUiState(),
        onLogin: (String, String) -> Unit = { _, _ -> },
        onLoginSuccess: () -> Unit = {},
        onNavigateToSignUp: () -> Unit = {},
        onClearError: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            KosmosTheme {
                LoginScreen(
                    onLoginSuccess     = onLoginSuccess,
                    onNavigateToSignUp = onNavigateToSignUp,
                    uiState            = uiState,
                    onLogin            = onLogin,
                    onClearError       = onClearError
                )
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  Tests                                                               //
    // ------------------------------------------------------------------ //

    @Test
    fun loginScreen_initialState_coreElementsVisible() {
        launchScreen()

        composeTestRule.onNodeWithText("Kosmos").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_email_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_password_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_navigate_to_signup_button").assertIsDisplayed()
    }

    @Test
    fun loginScreen_typingCredentials_fieldsAcceptInput() {
        launchScreen()

        composeTestRule.onNodeWithTag("login_email_field").performTextInput("user@test.com")
        composeTestRule.onNodeWithTag("login_password_field").performTextInput("secret123")

        // Both fields should still be visible / interactable
        composeTestRule.onNodeWithTag("login_email_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_password_field").assertIsDisplayed()
    }

    @Test
    fun loginScreen_clickLoginButton_callsOnLoginWithTypedCredentials() {
        var capturedEmail    = ""
        var capturedPassword = ""

        launchScreen(onLogin = { e, p ->
            capturedEmail    = e
            capturedPassword = p
        })

        composeTestRule.onNodeWithTag("login_email_field").performTextInput("alice@example.com")
        composeTestRule.onNodeWithTag("login_password_field").performTextInput("password123")
        composeTestRule.onNodeWithTag("login_button").performClick()

        assertEquals("alice@example.com", capturedEmail)
        assertEquals("password123",       capturedPassword)
    }

    @Test
    fun loginScreen_loadingState_showsProgressAndDisablesButton() {
        launchScreen(uiState = AuthUiState(isLoading = true))

        // Progress indicator appears inside the button while loading
        composeTestRule.onNodeWithTag("login_progress_indicator").assertIsDisplayed()
        // Button is disabled (not hidden) while loading
        composeTestRule.onNodeWithTag("login_button").assertIsNotEnabled()
    }

    @Test
    fun loginScreen_errorState_showsErrorCard() {
        val errorMessage = "Invalid credentials"
        launchScreen(uiState = AuthUiState(error = errorMessage))

        composeTestRule.onNodeWithTag("login_error_card").assertIsDisplayed()
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun loginScreen_noError_errorCardNotPresent() {
        launchScreen(uiState = AuthUiState(error = null))

        composeTestRule.onNodeWithTag("login_error_card").assertDoesNotExist()
    }

    @Test
    fun loginScreen_navigateToSignupButton_triggersCallback() {
        var signupClicked = false
        launchScreen(onNavigateToSignUp = { signupClicked = true })

        composeTestRule.onNodeWithTag("login_navigate_to_signup_button").performClick()

        assertTrue("Expected onNavigateToSignUp to be called", signupClicked)
    }
}
