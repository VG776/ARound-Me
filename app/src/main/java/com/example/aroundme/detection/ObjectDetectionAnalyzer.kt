package com.example.aroundme.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.aroundme.detection.ImageUtils.rotateBitmap
import com.example.aroundme.detection.ImageUtils.toBitmap
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import org.tensorflow.lite.support.image.TensorImage

data class DetectedObject(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF,
    val distanceDescription: String
)

class ObjectDetectionAnalyzer(
    context: Context,
    private val onDetectionResult: (List<DetectedObject>) -> Unit
) : ImageAnalysis.Analyzer {
    
    // Use TensorFlow Lite Task API for robust model loading
    private var detector: ObjectDetector? = null
    private var modelLoadFailed = false

    init {
        try {
            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(5)
                .setScoreThreshold(0.4f)
                .build()

            detector = ObjectDetector.createFromFileAndOptions(
                context,
                "model.tflite",
                options
            )
            Log.d("ObjectDetection", "✅ Successfully loaded model.tflite using TFLite Task API")
        } catch (e: Exception) {
            // Don't crash the app if the model is incompatible or native init fails.
            Log.e("ObjectDetection", "❌ Failed to load model.tflite: ${e.message}", e)
            // Try a known-good reference model if present
            try {
                val refName = "model_ref_efficientdet_lite0.tflite"
                val options = ObjectDetector.ObjectDetectorOptions.builder()
                    .setMaxResults(5)
                    .setScoreThreshold(0.4f)
                    .build()
                detector = ObjectDetector.createFromFileAndOptions(
                    context,
                    refName,
                    options
                )
                Log.w("ObjectDetection", "✅ Loaded fallback model: $refName")
                modelLoadFailed = false
            } catch (e2: Exception) {
                modelLoadFailed = true
                detector = null
                Log.e("ObjectDetection", "❌ Fallback model load failed: ${e2.message}", e2)
                // We intentionally do NOT rethrow here. The analyzer will operate as no-op.
            }
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        try {
            // Convert ImageProxy to Bitmap using proper YUV conversion
            val bitmap = imageProxy.toBitmap()
            val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
            
            // Create TensorImage from bitmap
            val tensorImage = TensorImage.fromBitmap(rotatedBitmap)
            
            // If detector failed to initialize earlier, skip analysis.
            val localDetector = detector
            if (localDetector == null) {
                if (!modelLoadFailed) {
                    Log.w("ObjectDetection", "Detector is null, skipping analysis")
                } else {
                    Log.w("ObjectDetection", "Model failed to load previously; skipping analysis")
                }
                return
            }

            // Run detection
            val results = localDetector.detect(tensorImage)

            Log.d("ObjectDetection", "🔍 Detected ${results.size} objects")
            
            if (results.isNotEmpty()) {
                val detectedObjects = results.mapNotNull { detection ->
                    processDetection(detection, rotatedBitmap.width.toFloat())
                }
                
                if (detectedObjects.isNotEmpty()) {
                    onDetectionResult(detectedObjects.sortedByDescending { it.confidence })
                }
            }
        } catch (e: Exception) {
            Log.e("ObjectDetection", "Detection error: ${e.message}", e)
        } finally {
            imageProxy.close()
        }
    }
    
    private fun processDetection(detection: Detection, screenWidth: Float): DetectedObject? {
        val category = detection.categories.maxByOrNull { it.score } ?: return null
        
        val box = detection.boundingBox
        val centerX = box.centerX()
        
        val horizontalPosition = when {
            centerX < screenWidth / 3 -> "on your left"
            centerX > 2 * screenWidth / 3 -> "on your right"
            else -> "in front of you"
        }
        
        val label = category.label.lowercase().replaceFirstChar { it.uppercase() }
        val confidence = category.score
        
        // Estimate distance based on bounding box size
        val boxArea = box.width() * box.height()
        val screenArea = screenWidth * box.height() * 2 // rough estimate
        val ratio = boxArea / screenArea
        
        val distanceDesc = when {
            ratio > 0.25 -> "very close"
            ratio > 0.08 -> "close"
            ratio > 0.02 -> "at medium distance"
            else -> "far away"
        }
        
        Log.d("ObjectDetection", "📦 $label (${(confidence * 100).toInt()}%) $horizontalPosition, $distanceDesc")
        
        return DetectedObject(
            label = label,
            confidence = confidence,
            boundingBox = RectF(box),
            distanceDescription = "$horizontalPosition, $distanceDesc"
        )
    }
}