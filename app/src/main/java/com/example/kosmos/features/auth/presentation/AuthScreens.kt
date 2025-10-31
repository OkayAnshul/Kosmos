package com.example.kosmos.features.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
                CircularProgressIndicator()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    uiState: AuthUiState,
    onSignUp: (SignUpData) -> Unit,
    onCheckUsernameAvailability: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Required fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    // Optional fields
    var age by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var githubUrl by remember { mutableStateOf("") }
    var twitterUrl by remember { mutableStateOf("") }
    var linkedinUrl by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("") }
    var portfolioUrl by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var showOptionalFields by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onSignUpSuccess()
        }
    }

    // Check username availability when it changes
    LaunchedEffect(username) {
        if (username.length >= 3) {
            onCheckUsernameAvailability(username)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo/Title
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Join Kosmos",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Create your developer profile",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Required Fields Section
        Text(
            text = "Required Information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // Display name field
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name *") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            supportingText = { Text("Your full name") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            enabled = !uiState.isLoading
        )

        // Username field
        OutlinedTextField(
            value = username,
            onValueChange = {
                // Only allow alphanumeric and underscores
                if (it.matches(Regex("^[a-zA-Z0-9_]*$"))) {
                    username = it.lowercase()
                }
            },
            label = { Text("Username *") },
            leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
            prefix = { Text("@") },
            supportingText = {
                Text(
                    if (username.length < 3) "At least 3 characters (letters, numbers, underscore)"
                    else if (uiState.isUsernameAvailable == false) "Username already taken"
                    else if (uiState.isUsernameAvailable == true) "Username available!"
                    else "Checking availability..."
                )
            },
            isError = username.length >= 3 && uiState.isUsernameAvailable == false,
            trailingIcon = {
                when {
                    username.length >= 3 && uiState.isCheckingUsername -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                    username.length >= 3 && uiState.isUsernameAvailable == true -> {
                        Icon(Icons.Default.CheckCircle, "Available", tint = MaterialTheme.colorScheme.primary)
                    }
                    username.length >= 3 && uiState.isUsernameAvailable == false -> {
                        Icon(Icons.Default.Error, "Not available", tint = MaterialTheme.colorScheme.error)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            enabled = !uiState.isLoading
        )

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email *") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            enabled = !uiState.isLoading
        )

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password *") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            supportingText = { Text("At least 6 characters") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            enabled = !uiState.isLoading
        )

        // Confirm password field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password *") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                    Icon(
                        if (isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isConfirmPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = confirmPassword.isNotEmpty() && password != confirmPassword,
            supportingText = {
                if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                    Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (showOptionalFields) ImeAction.Next else ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (!showOptionalFields) {
                        keyboardController?.hide()
                    }
                }
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !uiState.isLoading
        )

        // Toggle optional fields
        OutlinedButton(
            onClick = { showOptionalFields = !showOptionalFields },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Icon(
                if (showOptionalFields) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(if (showOptionalFields) "Hide Optional Fields" else "Add Optional Profile Info")
        }

        // Optional Fields Section
        if (showOptionalFields) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "Optional Profile Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Text(
                text = "Help others learn more about you",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Age
            OutlinedTextField(
                value = age,
                onValueChange = { if (it.all { char -> char.isDigit() }) age = it },
                label = { Text("Age") },
                leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                enabled = !uiState.isLoading
            )

            // Role/Title
            OutlinedTextField(
                value = role,
                onValueChange = { role = it },
                label = { Text("Role/Title") },
                leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                placeholder = { Text("e.g., Android Developer") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                enabled = !uiState.isLoading
            )

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                placeholder = { Text("e.g., San Francisco, CA") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                enabled = !uiState.isLoading
            )

            // Bio
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 500) bio = it },
                label = { Text("Bio") },
                placeholder = { Text("Tell us about yourself...") },
                supportingText = { Text("${bio.length}/500") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = !uiState.isLoading
            )

            Text(
                text = "Social Links",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // GitHub URL
            OutlinedTextField(
                value = githubUrl,
                onValueChange = { githubUrl = it },
                label = { Text("GitHub") },
                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                placeholder = { Text("github.com/username") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                enabled = !uiState.isLoading
            )

            // Twitter URL
            OutlinedTextField(
                value = twitterUrl,
                onValueChange = { twitterUrl = it },
                label = { Text("Twitter/X") },
                leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                placeholder = { Text("x.com/username") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                enabled = !uiState.isLoading
            )

            // LinkedIn URL
            OutlinedTextField(
                value = linkedinUrl,
                onValueChange = { linkedinUrl = it },
                label = { Text("LinkedIn") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                placeholder = { Text("linkedin.com/in/username") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                enabled = !uiState.isLoading
            )

            // Website URL
            OutlinedTextField(
                value = websiteUrl,
                onValueChange = { websiteUrl = it },
                label = { Text("Website") },
                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                placeholder = { Text("yourwebsite.com") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                enabled = !uiState.isLoading
            )

            // Portfolio URL
            OutlinedTextField(
                value = portfolioUrl,
                onValueChange = { portfolioUrl = it },
                label = { Text("Portfolio") },
                leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                placeholder = { Text("portfolio.com") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = !uiState.isLoading
            )
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
            onClick = {
                val signUpData = SignUpData(
                    email = email,
                    password = password,
                    displayName = displayName,
                    username = username,
                    age = age.toIntOrNull(),
                    role = role.ifBlank { null },
                    bio = bio.ifBlank { null },
                    location = location.ifBlank { null },
                    githubUrl = githubUrl.ifBlank { null },
                    twitterUrl = twitterUrl.ifBlank { null },
                    linkedinUrl = linkedinUrl.ifBlank { null },
                    websiteUrl = websiteUrl.ifBlank { null },
                    portfolioUrl = portfolioUrl.ifBlank { null }
                )
                onSignUp(signUpData)
            },
            enabled = !uiState.isLoading &&
                    email.isNotBlank() &&
                    password.isNotBlank() &&
                    displayName.isNotBlank() &&
                    username.length >= 3 &&
                    uiState.isUsernameAvailable == true &&
                    password == confirmPassword &&
                    password.length >= 6,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
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

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Data class for sign up information
 */
data class SignUpData(
    val email: String,
    val password: String,
    val displayName: String,
    val username: String,
    val age: Int? = null,
    val role: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val githubUrl: String? = null,
    val twitterUrl: String? = null,
    val linkedinUrl: String? = null,
    val websiteUrl: String? = null,
    val portfolioUrl: String? = null
)