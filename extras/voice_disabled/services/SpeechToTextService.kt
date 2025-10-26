package com.example.kosmos.features.voice.services

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SpeechToTextService {
    @POST("v1/speech:recognize")
    suspend fun recognizeSpeech(
        @Header("Authorization") authorization: String,
        @Body request: SpeechRecognitionRequest
    ): Response<SpeechRecognitionResponse>
}

data class SpeechRecognitionRequest(
    val config: RecognitionConfig,
    val audio: RecognitionAudio
)

data class RecognitionConfig(
    val encoding: String = "WEBM_OPUS",
    val sampleRateHertz: Int = 16000,
    val languageCode: String = "en-US",
    val enableAutomaticPunctuation: Boolean = true,
    val enableWordTimeOffsets: Boolean = true
)

data class RecognitionAudio(
    val content: String // Base64 encoded audio
)

data class SpeechRecognitionResponse(
    val results: List<SpeechRecognitionResult>
)

data class SpeechRecognitionResult(
    val alternatives: List<SpeechRecognitionAlternative>
)

data class SpeechRecognitionAlternative(
    val transcript: String,
    val confidence: Float
)