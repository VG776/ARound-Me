package com.surendramaran.yolov8tflite

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.surendramaran.yolov8tflite.Constants.LABELS_PATH
import com.surendramaran.yolov8tflite.Constants.MODEL_PATH
import com.surendramaran.yolov8tflite.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var detector: Detector? = null

    private lateinit var cameraExecutor: ExecutorService
    
    // Accessibility features
    private lateinit var navigationManager: NavigationManager
    private var voiceCommandHandler: VoiceCommandHandler? = null
    private var isNavigationEnabled = true
    private var isVoiceCommandEnabled = false
    private var currentBoundingBoxes: List<BoundingBox> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize navigation manager with TTS
        navigationManager = NavigationManager(this) {
            runOnUiThread {
                Toast.makeText(this, "Voice guidance ready", Toast.LENGTH_SHORT).show()
                navigationManager.speak("Navigation system ready")
            }
        }

        cameraExecutor.execute {
            detector = Detector(baseContext, MODEL_PATH, LABELS_PATH, this)
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        bindListeners()
        setupAccessibilityFeatures()
    }

    private fun bindListeners() {
        binding.apply {
            // GPU toggle
            isGpu.setOnCheckedChangeListener { buttonView, isChecked ->
                cameraExecutor.submit {
                    detector?.restart(isGpu = isChecked)
                }
                if (isChecked) {
                    buttonView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.orange))
                } else {
                    buttonView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.gray))
                }
            }
            
            // Navigation toggle
            navigationToggle.setOnCheckedChangeListener { _, isChecked ->
                isNavigationEnabled = isChecked
                val message = if (isChecked) "Navigation enabled" else "Navigation disabled"
                navigationManager.speakImmediate(message)
            }
            
            // Voice command toggle
            voiceCommandToggle.setOnCheckedChangeListener { _, isChecked ->
                isVoiceCommandEnabled = isChecked
                handleVoiceCommandToggle(isChecked)
            }
            
            // Manual scan button for accessibility
            scanButton.setOnClickListener {
                performManualScan()
            }
            
            // Describe scene button
            describeButton.setOnClickListener {
                describeCurrentScene()
            }
        }
    }
    
    private fun setupAccessibilityFeatures() {
        // Set content descriptions for accessibility
        binding.apply {
            navigationToggle.contentDescription = "Toggle navigation guidance"
            voiceCommandToggle.contentDescription = "Toggle voice commands"
            scanButton.contentDescription = "Scan environment"
            describeButton.contentDescription = "Describe current scene"
            isGpu.contentDescription = "Toggle GPU acceleration"
        }
        
        // Make the app more accessible
        binding.root.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    
    private fun handleVoiceCommandToggle(enabled: Boolean) {
        if (enabled) {
            // Check for audio permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                binding.voiceCommandToggle.isChecked = false
                return
            }
            
            voiceCommandHandler = VoiceCommandHandler(this) { command ->
                handleVoiceCommand(command)
            }
            voiceCommandHandler?.startListening()
            navigationManager.speak("Voice commands enabled")
        } else {
            voiceCommandHandler?.destroy()
            voiceCommandHandler = null
            navigationManager.speak("Voice commands disabled")
        }
    }
    
    private fun handleVoiceCommand(command: VoiceCommand) {
        when (command) {
            VoiceCommand.WHAT_DO_YOU_SEE -> {
                describeCurrentScene()
            }
            VoiceCommand.DESCRIBE_SCENE -> {
                describeDetailedScene()
            }
            VoiceCommand.PAUSE_SPEECH -> {
                navigationManager.stopSpeaking()
                isNavigationEnabled = false
                binding.navigationToggle.isChecked = false
            }
            VoiceCommand.RESUME_SPEECH -> {
                isNavigationEnabled = true
                binding.navigationToggle.isChecked = true
                navigationManager.speak("Navigation resumed")
            }
            VoiceCommand.HELP -> {
                provideHelp()
            }
            VoiceCommand.SCAN_ENVIRONMENT -> {
                performManualScan()
            }
            VoiceCommand.UNKNOWN -> {
                // Do nothing
            }
        }
    }
    
    private fun performManualScan() {
        if (currentBoundingBoxes.isEmpty()) {
            navigationManager.speakImmediate("No objects detected")
        } else {
            describeCurrentScene()
        }
    }
    
    private fun describeCurrentScene() {
        if (currentBoundingBoxes.isEmpty()) {
            navigationManager.speakImmediate("No objects detected")
            return
        }
        
        val objectCounts = currentBoundingBoxes.groupingBy { it.clsName }.eachCount()
        val description = buildString {
            append("I can see ")
            objectCounts.entries.forEachIndexed { index, entry ->
                if (index > 0) append(", ")
                append("${entry.value} ${entry.key}")
                if (entry.value > 1) append("s")
            }
        }
        
        navigationManager.speakImmediate(description)
    }
    
    private fun describeDetailedScene() {
        if (currentBoundingBoxes.isEmpty()) {
            navigationManager.speakImmediate("No objects detected")
            return
        }
        
        val description = buildString {
            append("Detailed scan. ")
            currentBoundingBoxes.take(5).forEach { box ->
                val position = when {
                    box.cx < 0.35f -> "left"
                    box.cx > 0.65f -> "right"
                    else -> "center"
                }
                val distance = when {
                    box.h > 0.6f -> "very close"
                    box.h > 0.4f -> "close"
                    box.h > 0.2f -> "medium distance"
                    else -> "far"
                }
                append("${box.clsName} $distance on $position. ")
            }
        }
        
        navigationManager.speakImmediate(description)
    }
    
    private fun provideHelp() {
        val helpMessage = "Say what do you see to describe objects. " +
                         "Say pause speech to stop navigation. " +
                         "Say resume to continue. " +
                         "Say scan to check environment. " +
                         "Say describe scene for detailed information."
        navigationManager.speakImmediate(helpMessage)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider  = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview =  Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer =
                Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            detector?.detect(rotatedBitmap)
        }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        if (it[Manifest.permission.CAMERA] == true) { startCamera() }
    }
    
    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            handleVoiceCommandToggle(true)
            binding.voiceCommandToggle.isChecked = true
        } else {
            Toast.makeText(this, "Audio permission required for voice commands", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector?.close()
        cameraExecutor.shutdown()
        navigationManager.shutdown()
        voiceCommandHandler?.destroy()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()){
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA
        ).toTypedArray()
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            binding.overlay.clear()
            currentBoundingBoxes = emptyList()
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            binding.inferenceTime.text = "${inferenceTime}ms"
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
            
            // Update current detections
            currentBoundingBoxes = boundingBoxes
            
            // Announce navigation guidance if enabled
            if (isNavigationEnabled) {
                navigationManager.analyzeAndAnnounce(boundingBoxes)
            }
            
            // Update detection count
            binding.detectionCount.text = "${boundingBoxes.size} objects"
        }
    }
}
