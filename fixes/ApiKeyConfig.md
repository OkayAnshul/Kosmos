# Google Cloud Speech API Key Configuration

## Problem
`BuildConfig.GOOGLE_CLOUD_API_KEY` is empty, causing all transcription to fail.

## Solution

### Step 1: Get Google Cloud Speech API Key
1. Go to Google Cloud Console
2. Enable Speech-to-Text API
3. Create credentials (API Key)
4. Restrict key to Speech-to-Text API only

### Step 2: Update build.gradle.kts
```kotlin
buildTypes {
    debug {
        buildConfigField("String", "GOOGLE_CLOUD_API_KEY", "\"YOUR_DEV_API_KEY_HERE\"")
        buildConfigField("boolean", "ENABLE_LOGGING", "true")
    }

    release {
        buildConfigField("String", "GOOGLE_CLOUD_API_KEY", "\"YOUR_PROD_API_KEY_HERE\"")
        buildConfigField("boolean", "ENABLE_LOGGING", "false")
    }
}
```

### Step 3: Update TranscriptionService.kt
```kotlin
class TranscriptionService @Inject constructor(
    private val speechToTextService: SpeechToTextService,
    private val voiceMessageDao: VoiceMessageDao,
    private val context: Context
) {
    private val apiKey = BuildConfig.GOOGLE_CLOUD_API_KEY

    suspend fun transcribeVoiceMessage(voiceMessageId: String, audioFilePath: String): Result<String> {
        // Add API key validation
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Google Cloud API key not configured"))
        }

        // ... rest of existing code

        // Fix authorization header
        val response = speechToTextService.recognizeSpeech(
            authorization = "Bearer $apiKey",  // or "X-Goog-Api-Key: $apiKey"
            request = request
        )
    }
}
```

### Step 4: Security Best Practices
- Never commit API keys to version control
- Use environment variables in CI/CD
- Consider using Google Cloud SDK authentication instead of API keys