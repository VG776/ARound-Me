package com.example.aroundme.detection

import android.content.Context
import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

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
    
    private val detector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .build()
    )

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            detector.process(image)
                .addOnSuccessListener { detectedObjects ->
                    val results = detectedObjects.mapNotNull { detectedObject ->
                        val box = detectedObject.boundingBox
                        val centerX = box.centerX()
                        val screenWidth = imageProxy.width.toFloat()
                        
                        // Calculate relative position and distance
                        val horizontalPosition = when {
                            centerX < screenWidth / 3 -> "on your left"
                            centerX > 2 * screenWidth / 3 -> "on your right"
                            else -> "straight ahead"
                        }

                        val confidence = if (detectedObject.labels.isNotEmpty()) {
                            detectedObject.labels[0].confidence
                        } else 0f

                        val label = if (detectedObject.labels.isNotEmpty()) {
                            detectedObject.labels[0].text
                        } else "unknown object"

                        // Estimate distance based on bounding box size
                        val distanceDesc = when {
                            box.width() * box.height() > 0.5 * imageProxy.width * imageProxy.height ->
                                "very close"
                            box.width() * box.height() > 0.25 * imageProxy.width * imageProxy.height ->
                                "close"
                            else -> "far"
                        }

                        DetectedObject(
                            label = label,
                            confidence = confidence,
                            boundingBox = RectF(box),
                            distanceDescription = "$horizontalPosition, $distanceDesc"
                        )
                    }
                    onDetectionResult(results)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}