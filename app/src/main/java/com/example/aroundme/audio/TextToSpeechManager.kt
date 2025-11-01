package com.example.aroundme.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

class TextToSpeechManager(
    private val context: Context,
    private val onInitialized: () -> Unit = {}
) {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var isSpeaking = false
    private var lastSpokenText = ""
    private var lastSpeakTime = 0L
    private val minTimeBetweenUtterances = 1500L // 1.5 seconds between announcements

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported")
                } else {
                    textToSpeech?.setSpeechRate(0.9f)  // Slightly slower for better clarity
                    textToSpeech?.setPitch(1.0f)       // Normal pitch
                    isInitialized = true
                    speak("ARound Me is ready to help you", force = true)
                    onInitialized()
                }
            } else {
                Log.e("TTS", "Initialization failed with status: $status")
            }
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                Log.d("TTS", "Started speaking: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                Log.d("TTS", "Finished speaking: $utteranceId")
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
                Log.e("TTS", "Error speaking utterance: $utteranceId")
            }
        })
    }

    fun speak(text: String, force: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        
        // Skip if it's the same message and not enough time has passed, unless forced
        if (!force && text == lastSpokenText && currentTime - lastSpeakTime < minTimeBetweenUtterances) {
            return
        }

        // Only speak if initialized and not currently speaking or forced
        if (isInitialized && (!isSpeaking || force)) {
            val params = HashMap<String, String>().apply {
                put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1.0")
                put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ObjectDetection_${System.currentTimeMillis()}")
            }
            
            Log.d("TTS", "Speaking: $text")
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
            lastSpokenText = text
            lastSpeakTime = currentTime
        } else {
            Log.e("TTS", "Cannot speak: initialized=$isInitialized, speaking=$isSpeaking")
        }
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}