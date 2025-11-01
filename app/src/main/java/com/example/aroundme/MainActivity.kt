package com.example.aroundme

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.aroundme.assistant.AurieAssistant
import com.example.aroundme.camera.CameraManager
import com.example.aroundme.camera.FlashlightController
import com.example.aroundme.detection.DetectedObject
import com.example.aroundme.detection.DistanceLevel
import com.example.aroundme.detection.ObjectDetectionAnalyzer
import com.example.aroundme.ocr.OcrService
import com.example.aroundme.ui.theme.ARoundMeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var cameraManager: CameraManager
    private lateinit var flashlightController: FlashlightController
    private var ocrService: OcrService? = null  // Lazy load OCR
    private var lastFrameBitmap: Bitmap? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startApp()
        } else {
            Toast.makeText(
                this,
                "Camera and audio permissions are required for Aurie to help you",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        cameraManager = CameraManager(this)
        flashlightController = FlashlightController(this)
        // OCR will be lazy-loaded only when needed

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
        // Initialize Aurie Assistant
        AurieAssistant.initialize(
            appContext = this,
            onModeChanged = { isContinuous ->
                // Handle mode change - could update UI or behavior
                android.util.Log.d("MainActivity", "Mode changed: continuous=$isContinuous")
            },
            onFlashlightToggle = {
                flashlightController.toggleFlashlight()
            },
            onOcrRequest = {
                // Lazy-load OCR service when first needed
                if (ocrService == null) {
                    ocrService = OcrService()
                    Log.d("MainActivity", "OCR service initialized on demand")
                }
                
                // Capture current frame and process OCR
                lastFrameBitmap?.let { bitmap ->
                    processOcr(bitmap)
                }
            },
            onDescribeEnvironment = {
                // Trigger environment description
                AurieAssistant.speak("Scanning your surroundings...", priority = true)
            }
        )

        setContent {
            ARoundMeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        cameraManager = cameraManager,
                        flashlightController = flashlightController,
                        onFrameCaptured = { bitmap ->
                            lastFrameBitmap = bitmap
                        }
                    )
                }
            }
        }
    }

    private fun processOcr(bitmap: Bitmap) {
        val ocr = ocrService ?: run {
            AurieAssistant.speak("OCR service not available", priority = true)
            return
        }
        
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    ocr.processImage(bitmap)
                }
                
                val speech = ocr.formatForSpeech(result)
                AurieAssistant.speak(speech, priority = true)
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "OCR error", e)
                AurieAssistant.speak("Sorry, I couldn't read the text", priority = true)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.stopCamera()
        flashlightController.cleanup()
        ocrService?.close()  // Only close if initialized
        AurieAssistant.shutdown()
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
    flashlightController: FlashlightController,
    onFrameCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    
    var modeText by remember { mutableStateOf("Normal Mode") }
    var lastDetectionText by remember { mutableStateOf("") }
    
    // Track objects for continuous mode
    val detectedObjectsState = remember { mutableStateMapOf<String, DetectedObject>() }
    
    LaunchedEffect(previewView) {
        val analyzer = ObjectDetectionAnalyzer(
            context = context,
            onDetectionResult = { detectedObjects ->
                handleDetections(detectedObjects, detectedObjectsState) { announcement ->
                    lastDetectionText = announcement
                }
            },
            onBrightnessDetected = { brightness ->
                // Auto-adjust flashlight
                flashlightController.autoAdjust(brightness)
            }
        )

        scope.launch {
            val camera = cameraManager.startCamera(
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                imageAnalyzer = analyzer
            )
            
            // Set camera for flashlight control
            flashlightController.setCamera(camera)
        }
    }
    
    // Monitor mode changes
    LaunchedEffect(Unit) {
        launch {
            while (true) {
                modeText = if (AurieAssistant.isContinuousMode) {
                    "Continuous Mode"
                } else {
                    "Normal Mode"
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Info overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.small
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Aurie Active",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = modeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            if (lastDetectionText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = lastDetectionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        // Accessibility hint
        Text(
            text = "Say 'Hey Aurie' for commands",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

/**
 * Handle detections with intelligent announcement logic
 */
private fun handleDetections(
    detectedObjects: List<DetectedObject>,
    objectsState: MutableMap<String, DetectedObject>,
    onAnnouncement: (String) -> Unit
) {
    if (detectedObjects.isEmpty()) return
    
    // Update state
    objectsState.clear()
    detectedObjects.forEach { obj ->
        objectsState[obj.label] = obj
    }
    
    // Normal mode: Only announce danger/warning objects
    if (!AurieAssistant.isContinuousMode) {
        val urgentObjects = detectedObjects.filter { 
            it.distanceLevel == DistanceLevel.DANGER || it.distanceLevel == DistanceLevel.WARNING 
        }
        
        if (urgentObjects.isNotEmpty()) {
            val obj = urgentObjects.first()
            val isAlert = obj.distanceLevel == DistanceLevel.DANGER
            val announcement = "${obj.label} ${obj.distanceDescription}"
            
            AurieAssistant.speak(announcement, priority = false, alert = isAlert)
            onAnnouncement(announcement)
        }
    } else {
        // Continuous mode: Describe environment
        val descriptions = detectedObjects.take(3).map { obj ->
            "${obj.label} ${obj.distanceDescription}"
        }
        
        if (descriptions.isNotEmpty()) {
            val announcement = descriptions.joinToString(", ")
            AurieAssistant.speak(announcement, priority = false, alert = false)
            onAnnouncement(announcement)
        }
    }
}
