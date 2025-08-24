package com.example.kosmos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.*

/**
 * A helper class for handling speech recognition using Android's built-in SpeechRecognizer
 * This avoids conflicts with Firebase and Google Cloud dependencies
 *
 * Required permissions in AndroidManifest.xml:
 * <uses-permission android:name="android.permission.RECORD_AUDIO" />
 * <uses-permission android:name="android.permission.INTERNET" />
 *
 * Optional (for better offline support):
 * <queries>
 *   <intent>
 *     <action android:name="android.speech.RecognitionService" />
 *   </intent>
 * </queries>
 */
class SpeechRecognitionHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    /**
     * Check if speech recognition is available on the device
     */
    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Start speech recognition and return results as a Flow
     */
    fun startListening(languageCode: String = Locale.getDefault().toString()): Flow<SpeechResult> = callbackFlow {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechResult.Ready)
            }

            override fun onBeginningOfSpeech() {
                trySend(SpeechResult.BeginningOfSpeech)
            }

            override fun onRmsChanged(rmsdB: Float) {
                trySend(SpeechResult.RmsChanged(rmsdB))
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Not used
            }

            override fun onEndOfSpeech() {
                trySend(SpeechResult.EndOfSpeech)
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                trySend(SpeechResult.Error(errorMessage))
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                if (!matches.isNullOrEmpty()) {
                    val transcription = matches[0]
                    val confidence = confidences?.get(0) ?: 0f
                    trySend(SpeechResult.Success(transcription, confidence))
                } else {
                    trySend(SpeechResult.Error("No results"))
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    trySend(SpeechResult.PartialResult(matches[0]))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Not used
            }
        }

        speechRecognizer?.setRecognitionListener(recognitionListener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.startListening(intent)

        awaitClose {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    /**
     * Stop the current speech recognition session
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    /**
     * Cancel the current speech recognition session
     */
    fun cancel() {
        speechRecognizer?.cancel()
    }

    /**
     * Clean up resources
     */
    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}

/**
 * Sealed class representing different speech recognition states and results
 */
sealed class SpeechResult {
    object Ready : SpeechResult()
    object BeginningOfSpeech : SpeechResult()
    object EndOfSpeech : SpeechResult()
    data class RmsChanged(val rmsdB: Float) : SpeechResult()
    data class PartialResult(val text: String) : SpeechResult()
    data class Success(val text: String, val confidence: Float) : SpeechResult()
    data class Error(val message: String) : SpeechResult()
}