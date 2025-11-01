package com.surendramaran.yolov8tflite

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class VoiceCommandHandler(
    private val context: Context,
    private val onCommand: (VoiceCommand) -> Unit
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(this)
        }
    }

    fun startListening() {
        if (isListening) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        try {
            speechRecognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recognition", e)
            isListening = false
        }
    }

    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Ready for speech")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "Speech started")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Audio level changed
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Audio buffer received
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "Speech ended")
        isListening = false
    }

    override fun onError(error: Int) {
        Log.e(TAG, "Speech recognition error: $error")
        isListening = false
        
        // Auto-restart listening after error (except for no match)
        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                startListening()
            }, 1000)
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.firstOrNull()?.let { command ->
            Log.d(TAG, "Recognized command: $command")
            parseCommand(command.lowercase())
        }
        
        // Restart listening for continuous voice commands
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            startListening()
        }, 500)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        // Handle partial results if needed
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        // Handle other events
    }

    private fun parseCommand(command: String) {
        val voiceCommand = when {
            command.contains("what") && (command.contains("see") || command.contains("ahead") || command.contains("front")) -> 
                VoiceCommand.WHAT_DO_YOU_SEE
            command.contains("describe") && (command.contains("scene") || command.contains("surroundings")) -> 
                VoiceCommand.DESCRIBE_SCENE
            command.contains("pause") || command.contains("stop") && command.contains("speech") -> 
                VoiceCommand.PAUSE_SPEECH
            command.contains("resume") || command.contains("continue") -> 
                VoiceCommand.RESUME_SPEECH
            command.contains("help") -> 
                VoiceCommand.HELP
            command.contains("scan") || command.contains("check") -> 
                VoiceCommand.SCAN_ENVIRONMENT
            else -> 
                VoiceCommand.UNKNOWN
        }
        
        if (voiceCommand != VoiceCommand.UNKNOWN) {
            onCommand(voiceCommand)
        }
    }

    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    companion object {
        private const val TAG = "VoiceCommandHandler"
    }
}

enum class VoiceCommand {
    WHAT_DO_YOU_SEE,
    DESCRIBE_SCENE,
    PAUSE_SPEECH,
    RESUME_SPEECH,
    HELP,
    SCAN_ENVIRONMENT,
    UNKNOWN
}
