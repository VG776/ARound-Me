package com.example.aroundme.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.aroundme.detection.ImageUtils.rotateBitmap
import com.example.aroundme.detection.ImageUtils.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DetectedObject(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF,
    val distanceDescription: String,
    val distanceLevel: DistanceLevel,
    val position: Position
)

enum class DistanceLevel {
    DANGER,      // Very close - immediate action needed
    WARNING,     // Close - caution advised
    SAFE,        // Safe distance
    FAR          // Far away
}

enum class Position {
    LEFT, CENTER, RIGHT
}

/**
 * Enhanced Object Detection Analyzer with YOLO
 * Features:
 * - Low-latency real-time detection
 * - Safe distance detection with vibration alerts
 * - Async processing on IO dispatcher
 * - Intelligent throttling
 */
class ObjectDetectionAnalyzer(
    private val context: Context,
    private val onDetectionResult: (List<DetectedObject>) -> Unit,
    private val onBrightnessDetected: (Float) -> Unit = {}
) : ImageAnalysis.Analyzer {
    
    private val yoloDetector = YoloObjectDetector(context)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val scope = CoroutineScope(Dispatchers.Default) // Use Default for better performance
    
    private var lastVibrationTime = 0L
    private val vibrationCooldown = 2000L
    
    // Frame skipping for performance
    private var frameCount = 0
    private val processEveryNFrames = 2 // Process every 2nd frame for better performance
    
    // Temporal filtering to reduce false positives
    private val detectionHistory = mutableMapOf<String, Int>() // label -> consecutive detection count
    private val DETECTION_CONFIRMATION_THRESHOLD = 2 // Must see object 2 times before announcing
    
    companion object {
        private const val TAG = "ObjectDetection"
        
        // Safe distance thresholds (based on bounding box area ratio)
        private const val DANGER_THRESHOLD = 0.35f   // > 35% of screen
        private const val WARNING_THRESHOLD = 0.15f  // 15-35% of screen
        private const val SAFE_THRESHOLD = 0.05f     // 5-15% of screen
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        // Skip frames for better performance
        frameCount++
        if (frameCount % processEveryNFrames != 0) {
            imageProxy.close()
            return
        }
        
        // Process asynchronously to avoid blocking camera thread
        scope.launch {
            try {
                processImage(imageProxy)
            } catch (e: Exception) {
                Log.e(TAG, "Detection error: ${e.message}", e)
            } finally {
                imageProxy.close()
            }
        }
    }
    
    private suspend fun processImage(imageProxy: ImageProxy) {
        // Convert ImageProxy to Bitmap
        val bitmap = withContext(Dispatchers.Default) {
            val rawBitmap = imageProxy.toBitmap()
            rotateBitmap(rawBitmap, imageProxy.imageInfo.rotationDegrees)
        }
        
        // Estimate brightness for flashlight control (sample only, not every frame)
        if (frameCount % 10 == 0) {
            val brightness = estimateBrightness(bitmap)
            Log.d(TAG, "💡 Brightness: ${brightness.toInt()}/255")
            withContext(Dispatchers.Main) {
                onBrightnessDetected(brightness)
            }
        }
        
        // Run YOLO detection
        val detections = yoloDetector.detect(bitmap)
        
        if (detections.isEmpty()) {
            // Clear history if no detections
            detectionHistory.clear()
            return
        }
        
        // Update detection history for temporal filtering
        val currentLabels = detections.map { it.label }.toSet()
        
        // Increment count for detected objects
        currentLabels.forEach { label ->
            detectionHistory[label] = (detectionHistory[label] ?: 0) + 1
        }
        
        // Decrement count for objects no longer detected
        val labelsToRemove = mutableListOf<String>()
        detectionHistory.keys.forEach { label ->
            if (label !in currentLabels) {
                detectionHistory[label] = (detectionHistory[label] ?: 0) - 1
                if (detectionHistory[label]!! <= 0) {
                    labelsToRemove.add(label)
                }
            }
        }
        labelsToRemove.forEach { detectionHistory.remove(it) }
        
        // Filter detections - only include confirmed objects
        val confirmedDetections = detections.filter { detection ->
            (detectionHistory[detection.label] ?: 0) >= DETECTION_CONFIRMATION_THRESHOLD
        }
        
        if (confirmedDetections.isEmpty()) {
            return
        }
        
        // Process detections with distance estimation
        val detectedObjects = confirmedDetections.map { detection ->
            processDetection(detection, bitmap.width.toFloat(), bitmap.height.toFloat())
        }
        
        // Check for danger zone objects
        val dangerObjects = detectedObjects.filter { it.distanceLevel == DistanceLevel.DANGER }
        if (dangerObjects.isNotEmpty()) {
            vibrateAlert(VibrationPattern.DANGER)
        } else {
            val warningObjects = detectedObjects.filter { it.distanceLevel == DistanceLevel.WARNING }
            if (warningObjects.isNotEmpty()) {
                vibrateAlert(VibrationPattern.WARNING)
            }
        }
        
        // Send results to main thread
        withContext(Dispatchers.Main) {
            onDetectionResult(detectedObjects.sortedByDescending { it.confidence })
        }
    }
    
    private fun processDetection(
        detection: YoloObjectDetector.Detection,
        imageWidth: Float,
        imageHeight: Float
    ): DetectedObject {
        val box = detection.boundingBox
        val centerX = box.centerX()
        
        // Determine horizontal position
        val position = when {
            centerX < imageWidth / 3 -> Position.LEFT
            centerX > 2 * imageWidth / 3 -> Position.RIGHT
            else -> Position.CENTER
        }
        
        val horizontalDesc = when (position) {
            Position.LEFT -> "on your left"
            Position.RIGHT -> "on your right"
            Position.CENTER -> "in front of you"
        }
        
        // Calculate distance based on bounding box size
        val boxArea = box.width() * box.height()
        val screenArea = imageWidth * imageHeight
        val areaRatio = boxArea / screenArea
        
        val (distanceLevel, distanceDesc) = when {
            areaRatio > DANGER_THRESHOLD -> DistanceLevel.DANGER to "very close - slow down"
            areaRatio > WARNING_THRESHOLD -> DistanceLevel.WARNING to "approaching"
            areaRatio > SAFE_THRESHOLD -> DistanceLevel.SAFE to "at safe distance"
            else -> DistanceLevel.FAR to "far away"
        }
        
        val label = detection.label.lowercase().replaceFirstChar { it.uppercase() }
        
        Log.d(TAG, "📦 $label (${(detection.confidence * 100).toInt()}%) $horizontalDesc, $distanceDesc")
        
        return DetectedObject(
            label = label,
            confidence = detection.confidence,
            boundingBox = detection.boundingBox,
            distanceDescription = "$horizontalDesc, $distanceDesc",
            distanceLevel = distanceLevel,
            position = position
        )
    }
    
    private fun vibrateAlert(pattern: VibrationPattern) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastVibrationTime < vibrationCooldown) {
            return // Throttle vibrations
        }
        
        vibrator?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val effect = when (pattern) {
                    VibrationPattern.DANGER -> VibrationEffect.createWaveform(
                        longArrayOf(0, 200, 100, 200), // Double buzz
                        -1
                    )
                    VibrationPattern.WARNING -> VibrationEffect.createOneShot(
                        150,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                }
                it.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                when (pattern) {
                    VibrationPattern.DANGER -> it.vibrate(longArrayOf(0, 200, 100, 200), -1)
                    VibrationPattern.WARNING -> it.vibrate(150)
                }
            }
        }
        
        lastVibrationTime = currentTime
        Log.d(TAG, "📳 Vibration alert: $pattern")
    }
    
    private fun estimateBrightness(bitmap: Bitmap): Float {
        return try {
            // Sample center pixels for brightness estimation
            val centerX = bitmap.width / 2
            val centerY = bitmap.height / 2
            val sampleSize = 50.coerceAtMost(bitmap.width / 4)
            
            var totalBrightness = 0L
            var pixelCount = 0
            
            for (x in (centerX - sampleSize).coerceAtLeast(0) until (centerX + sampleSize).coerceAtMost(bitmap.width)) {
                for (y in (centerY - sampleSize).coerceAtLeast(0) until (centerY + sampleSize).coerceAtMost(bitmap.height)) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    totalBrightness += (r + g + b) / 3
                    pixelCount++
                }
            }
            
            if (pixelCount > 0) (totalBrightness / pixelCount).toFloat() else 128f
        } catch (e: Exception) {
            128f // Default medium brightness
        }
    }
    
    fun cleanup() {
        yoloDetector.close()
    }
    
    enum class VibrationPattern {
        DANGER, WARNING
    }
}