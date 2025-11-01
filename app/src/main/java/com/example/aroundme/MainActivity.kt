package com.example.aroundme

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.aroundme.ui.theme.ARoundMeTheme
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request microphone permission if not granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )
        }

        // Initialize Text-to-Speech
        tts = TextToSpeech(this, this)

        // Initialize Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        setContent {
            ARoundMeTheme {
                var recognizedText by remember { mutableStateOf("Say something...") }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VoiceScreen(
                        modifier = Modifier.padding(innerPadding),
                        recognizedText = recognizedText,
                        onSpeakClick = {
                            speakOut("Hello! How are you?")
                        },
                        onListenClick = {
                            startListening { result ->
                                recognizedText = result
                                speakOut("You said: $result")
                            }
                        }
                    )
                }
            }
        }
    }

    // ---------- TTS Setup ----------
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.ENGLISH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show()
            } else {
                speakOut("Welcome to ARoundMe")
            }
        }
    }

    private fun speakOut(text: String) {
        if (::tts.isInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // ---------- Speech Recognition ----------
    private fun startListening(onResult: (String) -> Unit) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(applicationContext, "Listening...", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.get(0) ?: "Didn't catch that"
                onResult(text)
            }

            override fun onError(error: Int) {
                Toast.makeText(applicationContext, "Error recognizing speech", Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })

        speechRecognizer.startListening(intent)
    }

    // ---------- Cleanup ----------
    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        if (this::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }
}

@Composable
fun VoiceScreen(
    modifier: Modifier = Modifier,
    recognizedText: String,
    onSpeakClick: () -> Unit,
    onListenClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = recognizedText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(onClick = onSpeakClick, modifier = Modifier.fillMaxWidth()) {
            Text("🔊 Speak")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onListenClick, modifier = Modifier.fillMaxWidth()) {
            Text("🎙 Listen")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VoiceScreenPreview() {
    ARoundMeTheme {
        VoiceScreen(
            recognizedText = "Preview Mode",
            onSpeakClick = {},
            onListenClick = {}
        )
    }
}