package com.example.aroundme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.aroundme.audio.TextToSpeechManager
import com.example.aroundme.camera.CameraManager
import com.example.aroundme.detection.ObjectDetectionAnalyzer
import com.example.aroundme.ui.theme.ARoundMeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var cameraManager: CameraManager
    private lateinit var textToSpeechManager: TextToSpeechManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startApp()
        } else {
            Toast.makeText(
                this,
                "Camera and audio permissions are required for this app",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        cameraManager = CameraManager(this)
        textToSpeechManager = TextToSpeechManager(this) {
            // Speak welcome message when TTS is initialized
            textToSpeechManager.speak("Welcome to ARound Me. I will help you detect objects around you.")
        }

        // Check permissions
        if (hasRequiredPermissions()) {
            startApp()
        } else {
            requestPermissions()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun startApp() {
        setContent {
            ARoundMeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        cameraManager = cameraManager,
                        textToSpeechManager = textToSpeechManager
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.stopCamera()
        textToSpeechManager.shutdown()
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}

@Composable
fun MainScreen(
    cameraManager: CameraManager,
    textToSpeechManager: TextToSpeechManager
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(previewView) {
        val analyzer = ObjectDetectionAnalyzer(context) { detectedObjects ->
            if (detectedObjects.isNotEmpty()) {
                // Get the closest object with highest confidence
                val mostRelevantObject = detectedObjects.maxByOrNull { it.confidence }
                mostRelevantObject?.let { obj ->
                    // Only announce if confidence is above 70%
                    if (obj.confidence >= 0.7f) {
                        val description = "${obj.label} ${obj.distanceDescription}"
                        textToSpeechManager.speak(description)
                    }
                }
            }
        }

        scope.launch {
            cameraManager.startCamera(
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                imageAnalyzer = analyzer
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Accessibility hint text
        Text(
            text = "Camera view active. Objects will be announced automatically.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}