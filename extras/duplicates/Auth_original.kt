package com.example.kosmos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kosmos.data.repository.AuthRepository
import com.example.kosmos.core.models.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        _uiState.value = _uiState.value.copy(
            isLoggedIn = authRepository.isUserLoggedIn()
        )
    }

    fun login(email: String, password: String) {
        if (!isValidInput(email, password)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = authRepository.signInWithEmailAndPassword(email, password)
                result.fold(
                    onSuccess = { user ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            currentUser = user
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Login failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Login failed"
                )
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        if (!isValidSignUpInput(email, password, displayName)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = authRepository.createUserWithEmailAndPassword(email, password, displayName)
                result.fold(
                    onSuccess = { user ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            currentUser = user
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Sign up failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Sign up failed"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _uiState.value = AuthUiState()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Logout failed"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun isValidInput(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Email cannot be empty")
                false
            }
            password.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Password cannot be empty")
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _uiState.value = _uiState.value.copy(error = "Please enter a valid email")
                false
            }
            else -> true
        }
    }

    private fun isValidSignUpInput(email: String, password: String, displayName: String): Boolean {
        return when {
            displayName.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Display name cannot be empty")
                false
            }
            password.length < 6 -> {
                _uiState.value = _uiState.value.copy(error = "Password must be at least 6 characters")
                false
            }
            else -> isValidInput(email, password)
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    uiState: AuthUiState,
    onLogin: (String, String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val passwordFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo/Title
        Text(
            text = "Kosmos",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Connect & Collaborate",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (uiState.error != null) onClearError()
            },
            label = { Text("Email") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocusRequester.requestFocus() }
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !uiState.isLoading
        )

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (uiState.error != null) onClearError()
            },
            label = { Text("Password") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    onLogin(email, password)
                }
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester)
                .padding(bottom = 24.dp),
            enabled = !uiState.isLoading
        )

        // Error message
        if (uiState.error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Login button
        Button(
            onClick = { onLogin(email, password) },
            enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
//                    size = 20.dp,
//                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Login", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign up button
        TextButton(
            onClick = onNavigateToSignUp,
            enabled = !uiState.isLoading
        ) {
            Text("Don't have an account? Sign Up")
        }
    }
}

// SignUpScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    uiState: AuthUiState,
    onSignUp: (String, String, String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onSignUpSuccess()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo/Title
        Text(
            text = "Join Kosmos",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Create your account",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Display name field
        OutlinedTextField(
            value = displayName,
            onValueChange = {
                displayName = it
                if (uiState.error != null) onClearError()
            },
            label = { Text("Display Name") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { emailFocusRequester.requestFocus() }
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !uiState.isLoading
        )

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (uiState.error != null) onClearError()
            },
            label = { Text("Email") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocusRequester.requestFocus() }
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusRequester)
                .padding(bottom = 16.dp),
            enabled = !uiState.isLoading
        )

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (uiState.error != null) onClearError()
            },
            label = { Text("Password") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { confirmPasswordFocusRequester.requestFocus() }
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester)
                .padding(bottom = 16.dp),
            enabled = !uiState.isLoading
        )

        // Confirm password field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                if (uiState.error != null) onClearError()
            },
            label = { Text("Confirm Password") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                    Icon(
                        if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    if (password == confirmPassword) {
                        onSignUp(email, password, displayName)
                    }
                }
            ),
            singleLine = true,
            isError = confirmPassword.isNotEmpty() && password != confirmPassword,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(confirmPasswordFocusRequester)
                .padding(bottom = 8.dp),
            enabled = !uiState.isLoading
        )

        // Password match indicator
        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
            Text(
                text = "Passwords do not match",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Error message
        if (uiState.error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = uiState.error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Sign up button
        Button(
            onClick = { onSignUp(email, password, displayName) },
            enabled = !uiState.isLoading &&
                    email.isNotBlank() &&
                    password.isNotBlank() &&
                    displayName.isNotBlank() &&
                    password == confirmPassword,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
//                    size = 20.dp,
//                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Create Account", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Login button
        TextButton(
            onClick = onNavigateToLogin,
            enabled = !uiState.isLoading
        ) {
            Text("Already have an account? Login")
        }
    }
}