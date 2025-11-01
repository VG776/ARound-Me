package com.example.aroundme.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Aurie - Interactive Voice Assistant for ARound Me
 * Handles speech recognition, TTS, and mode management
 */
object AurieAssistant {
    
    private const val TAG = "AurieAssistant"
    
    // State management
    private var context: Context? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = AtomicBoolean(false)
    private var isSpeaking = AtomicBoolean(false)
    private var isListening = AtomicBoolean(false)
    
    // Mode management
    var isContinuousMode = false
        private set
    
    // Callbacks
    private var onModeChanged: ((Boolean) -> Unit)? = null
    private var onFlashlightToggle: (() -> Unit)? = null
    private var onOcrRequest: (() -> Unit)? = null
    private var onDescribeEnvironment: (() -> Unit)? = null
    
    // Speech throttling
    private var lastSpokenText = ""
    private var lastSpeakTime = 0L
    private val minTimeBetweenSimilarUtterances = 3000L
    private val speechQueue = mutableListOf<String>()
    private var isProcessingQueue = false
    
    // Auto-restart handler
    private val handler = Handler(Looper.getMainLooper())
    private val restartListeningRunnable = Runnable { startListening() }
    
    /**
     * Initialize Aurie with context
     */
    fun initialize(
        appContext: Context,
        onModeChanged: (Boolean) -> Unit = {},
        onFlashlightToggle: () -> Unit = {},
        onOcrRequest: () -> Unit = {},
        onDescribeEnvironment: () -> Unit = {}
    ) {
        if (isInitialized.get()) {
            Log.d(TAG, "Already initialized")
            return
        }
        
        context = appContext.applicationContext
        this.onModeChanged = onModeChanged
        this.onFlashlightToggle = onFlashlightToggle
        this.onOcrRequest = onOcrRequest
        this.onDescribeEnvironment = onDescribeEnvironment
        
        initializeTTS()
        initializeSpeechRecognizer()
        
        isInitialized.set(true)
        Log.d(TAG, "✅ Aurie initialized successfully")
    }
    
    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.apply {
                    setLanguage(Locale.US)
                    setSpeechRate(0.95f)  // Calm, clear pace
                    setPitch(1.0f)
                    
                    setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isSpeaking.set(true)
                        }
                        
                        override fun onDone(utteranceId: String?) {
                            isSpeaking.set(false)
                            processNextInQueue()
                            
                            // Resume listening after speaking
                            handler.postDelayed({
                                if (!isListening.get() && isInitialized.get()) {
                                    startListening()
                                }
                            }, 500)
                        }
                        
                        override fun onError(utteranceId: String?) {
                            isSpeaking.set(false)
                            processNextInQueue()
                        }
                    })
                }
                
                // Welcome message
                speak("Hello, I'm Aurie. I'm here to guide you safely.", priority = true, alert = false)
                
                // Start listening after welcome
                handler.postDelayed({ startListening() }, 3000)
                
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }
    
    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available")
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening.set(true)
                Log.d(TAG, "🎤 Ready for speech")
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "🎤 Speech detected")
            }
            
            override fun onRmsChanged(rmsdB: Float) {}
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                isListening.set(false)
                Log.d(TAG, "🎤 Speech ended")
            }
            
            override fun onError(error: Int) {
                isListening.set(false)
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown error: $error"
                }
                Log.d(TAG, "Speech recognition error: $errorMsg")
                
                // Auto-restart listening (except for permanent errors)
                if (error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    handler.postDelayed(restartListeningRunnable, 1000)
                }
            }
            
            override fun onResults(results: Bundle?) {
                isListening.set(false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0].lowercase()
                    Log.d(TAG, "🎤 Recognized: $command")
                    handleVoiceCommand(command)
                }
                
                // Restart listening
                handler.postDelayed(restartListeningRunnable, 500)
            }
            
            override fun onPartialResults(partialResults: Bundle?) {}
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    
    /**
     * Start listening for voice commands
     */
    fun startListening() {
        if (!isInitialized.get() || isListening.get() || isSpeaking.get()) {
            return
        }
        
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "👂 Listening started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
        }
    }
    
    /**
     * Stop listening
     */
    fun stopListening() {
        handler.removeCallbacks(restartListeningRunnable)
        speechRecognizer?.stopListening()
        isListening.set(false)
        Log.d(TAG, "👂 Listening stopped")
    }
    
    /**
     * Handle recognized voice commands
     */
    private fun handleVoiceCommand(command: String) {
        when {
            command.contains("continuous mode") || command.contains("switch to continuous") -> {
                if (!isContinuousMode) {
                    isContinuousMode = true
                    onModeChanged?.invoke(true)
                    speak("Switched to continuous mode. I'll describe everything around you.", priority = true)
                }
            }
            
            command.contains("normal mode") || command.contains("switch to normal") -> {
                if (isContinuousMode) {
                    isContinuousMode = false
                    onModeChanged?.invoke(false)
                    speak("Back to normal mode.", priority = true)
                }
            }
            
            command.contains("what's around") || command.contains("describe") || command.contains("what do you see") -> {
                onDescribeEnvironment?.invoke()
            }
            
            command.contains("read sign") || command.contains("read text") || command.contains("what does it say") -> {
                speak("Scanning for text...", priority = true)
                onOcrRequest?.invoke()
            }
            
            command.contains("flashlight") || command.contains("light") -> {
                when {
                    command.contains("on") || command.contains("turn on") -> {
                        onFlashlightToggle?.invoke()
                        speak("Flashlight turned on.", priority = true)
                    }
                    command.contains("off") || command.contains("turn off") -> {
                        onFlashlightToggle?.invoke()
                        speak("Flashlight turned off.", priority = true)
                    }
                    else -> {
                        onFlashlightToggle?.invoke()
                    }
                }
            }
            
            command.contains("hello") || command.contains("hey aurie") || command.contains("hi aurie") -> {
                speak("Hello! How can I help you?", priority = true)
            }
            
            command.contains("help") -> {
                speak("You can ask me to switch modes, describe surroundings, read signs, or control the flashlight.", priority = true)
            }
        }
    }
    
    /**
     * Speak text with intelligent throttling
     */
    fun speak(text: String, priority: Boolean = false, alert: Boolean = false) {
        if (!isInitialized.get() || textToSpeech == null) {
            Log.w(TAG, "Cannot speak - not initialized")
            return
        }
        
        val currentTime = System.currentTimeMillis()
        
        // Check if we should throttle this message
        if (!priority && !alert) {
            if (text == lastSpokenText && currentTime - lastSpeakTime < minTimeBetweenSimilarUtterances) {
                Log.d(TAG, "Throttling repeated message: $text")
                return
            }
        }
        
        // Priority messages skip the queue
        if (priority) {
            speakNow(text, alert)
            return
        }
        
        // Add to queue
        synchronized(speechQueue) {
            // Don't add duplicates
            if (!speechQueue.contains(text)) {
                speechQueue.add(text)
            }
        }
        
        processNextInQueue()
    }
    
    private fun speakNow(text: String, alert: Boolean = false) {
        try {
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "aurie_${System.currentTimeMillis()}")
            }
            
            // Alert tone: slightly faster and higher pitch
            if (alert) {
                textToSpeech?.setSpeechRate(1.1f)
                textToSpeech?.setPitch(1.1f)
            } else {
                textToSpeech?.setSpeechRate(0.95f)
                textToSpeech?.setPitch(1.0f)
            }
            
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))
            
            lastSpokenText = text
            lastSpeakTime = System.currentTimeMillis()
            
            Log.d(TAG, "🔊 Speaking: $text")
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking", e)
        }
    }
    
    private fun processNextInQueue() {
        if (isSpeaking.get() || isProcessingQueue) {
            return
        }
        
        synchronized(speechQueue) {
            if (speechQueue.isEmpty()) {
                return
            }
            
            isProcessingQueue = true
            val text = speechQueue.removeAt(0)
            speakNow(text, alert = false)
            isProcessingQueue = false
        }
    }
    
    /**
     * Shutdown and cleanup
     */
    fun shutdown() {
        handler.removeCallbacks(restartListeningRunnable)
        stopListening()
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        isInitialized.set(false)
        Log.d(TAG, "Aurie shut down")
    }
}
