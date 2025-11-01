package com.example.aroundme.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Optimized YOLOv5 TFLite Object Detector
 * Low-latency real-time detection with GPU acceleration
 */
class YoloObjectDetector(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var labels: List<String> = emptyList()
    private var inputSize = 320
    private var isInitialized = false
    
    // Output tensor info
    private var outputShape: IntArray? = null
    
    companion object {
        private const val TAG = "YoloDetector"
        private const val MODEL_FILE = "yolov5n.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val CONFIDENCE_THRESHOLD = 0.35f  // Increased to reduce false positives
        private const val IOU_THRESHOLD = 0.45f
        private const val MAX_DETECTIONS = 10
        
        // Problematic class pairs (similar appearance)
        private val SIMILAR_CLASSES = mapOf(
            "refrigerator" to listOf("wall", "door", "oven", "microwave"),
            "tv" to listOf("wall", "window"),
            "laptop" to listOf("book"),
            "cell phone" to listOf("remote")
        )
    }
    
    data class Detection(
        val label: String,
        val confidence: Float,
        val boundingBox: RectF
    )
    
    init {
        try {
            loadModel()
            loadLabels()
            isInitialized = true
            Log.d(TAG, "✅ YOLOv5 detector initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize YOLOv5 detector", e)
            isInitialized = false
        }
    }
    
    private fun loadModel() {
        try {
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                
                // Try GPU acceleration if available
                val compatList = CompatibilityList()
                if (compatList.isDelegateSupportedOnThisDevice) {
                    try {
                        gpuDelegate = GpuDelegate()
                        addDelegate(gpuDelegate)
                        Log.d(TAG, "GPU acceleration enabled")
                    } catch (e: Exception) {
                        Log.w(TAG, "GPU delegate failed, using CPU", e)
                    }
                }
            }
            
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE)
            interpreter = Interpreter(modelBuffer, options)
            
            // Get input shape
            val inputShape = interpreter?.getInputTensor(0)?.shape()
            if (inputShape != null && inputShape.size >= 4) {
                inputSize = inputShape[1]
            }
            
            // Get output shape for proper parsing
            outputShape = interpreter?.getOutputTensor(0)?.shape()
            
            Log.d(TAG, "Model loaded. Input: $inputSize x $inputSize, Output shape: ${outputShape?.contentToString()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model", e)
            throw e
        }
    }
    
    private fun loadLabels() {
        try {
            labels = FileUtil.loadLabels(context, LABELS_FILE)
            Log.d(TAG, "Loaded ${labels.size} labels")
        } catch (e: Exception) {
            Log.w(TAG, "Could not load labels file, using COCO classes")
            labels = getCocoLabels()
        }
    }
    
    /**
     * Detect objects in the given bitmap - OPTIMIZED
     */
    fun detect(bitmap: Bitmap): List<Detection> {
        if (!isInitialized || interpreter == null) {
            Log.w(TAG, "Detector not initialized")
            return emptyList()
        }
        
        try {
            val startTime = System.currentTimeMillis()
            
            // Preprocess image
            val inputBuffer = preprocessImage(bitmap)
            
            // Prepare output buffer based on actual model output
            val output = Array(1) { Array(outputShape?.get(1) ?: 25200) { FloatArray(outputShape?.get(2) ?: 85) } }
            
            // Run inference
            interpreter?.run(inputBuffer, output)
            val inferenceTime = System.currentTimeMillis() - startTime
            
            // Parse output
            val detections = parseOutputOptimized(output[0], bitmap.width, bitmap.height)
            
            // Apply NMS
            val filteredDetections = applyNMS(detections)
            
            Log.d(TAG, "⚡ Inference: ${inferenceTime}ms, Detected: ${filteredDetections.size}")
            return filteredDetections
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during detection", e)
            return emptyList()
        }
    }
    
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // Resize bitmap
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val intValues = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)
        
        // Normalize to [0, 1]
        for (pixelValue in intValues) {
            byteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)
            byteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
        }
        
        return byteBuffer
    }
    
    private fun parseOutputOptimized(output: Array<FloatArray>, imageWidth: Int, imageHeight: Int): List<Detection> {
        val detections = mutableListOf<Detection>()
        val numDetections = output.size
        val numElements = if (numDetections > 0) output[0].size else 0
        
        if (numElements < 85) {
            Log.w(TAG, "Unexpected output format: $numDetections x $numElements")
            return emptyList()
        }
        
        for (i in 0 until numDetections) {
            val detection = output[i]
            
            // YOLOv5 format: [x, y, w, h, confidence, class_scores...]
            val xCenter = detection[0]
            val yCenter = detection[1]
            val width = detection[2]
            val height = detection[3]
            val confidence = detection[4]
            
            if (confidence < CONFIDENCE_THRESHOLD) continue
            
            // Find best class
            var maxScore = 0f
            var maxIndex = 0
            for (c in 5 until min(numElements, 85)) {
                val score = detection[c]
                if (score > maxScore) {
                    maxScore = score
                    maxIndex = c - 5
                }
            }
            
            val finalConfidence = confidence * maxScore
            if (finalConfidence < CONFIDENCE_THRESHOLD) continue
            
            // Additional filtering for problematic classes
            val label = if (maxIndex < labels.size) labels[maxIndex] else "Object"
            
            // Skip low-confidence detections of problematic objects
            if (isProblematicObject(label) && finalConfidence < 0.50f) {
                Log.d(TAG, "⚠️ Skipping low-confidence $label (${finalConfidence})")
                continue
            }
            
            // Convert normalized coordinates to pixel coordinates
            val left = ((xCenter - width / 2) * imageWidth).coerceIn(0f, imageWidth.toFloat())
            val top = ((yCenter - height / 2) * imageHeight).coerceIn(0f, imageHeight.toFloat())
            val right = ((xCenter + width / 2) * imageWidth).coerceIn(0f, imageWidth.toFloat())
            val bottom = ((yCenter + height / 2) * imageHeight).coerceIn(0f, imageHeight.toFloat())
            
            // Skip invalid boxes
            if (right <= left || bottom <= top) continue
            
            detections.add(
                Detection(
                    label = label,
                    confidence = finalConfidence,
                    boundingBox = RectF(left, top, right, bottom)
                )
            )
        }
        
        return detections
    }
    
    /**
     * Check if object is prone to false positives
     */
    private fun isProblematicObject(label: String): Boolean {
        return SIMILAR_CLASSES.keys.any { it.equals(label, ignoreCase = true) }
    }
    
    private fun applyNMS(detections: List<Detection>): List<Detection> {
        val sortedDetections = detections.sortedByDescending { it.confidence }
        val selectedDetections = mutableListOf<Detection>()
        
        for (detection in sortedDetections) {
            var shouldSelect = true
            
            for (selected in selectedDetections) {
                if (calculateIoU(detection.boundingBox, selected.boundingBox) > IOU_THRESHOLD) {
                    shouldSelect = false
                    break
                }
            }
            
            if (shouldSelect) {
                selectedDetections.add(detection)
                if (selectedDetections.size >= MAX_DETECTIONS) break
            }
        }
        
        return selectedDetections
    }
    
    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectLeft = max(box1.left, box2.left)
        val intersectTop = max(box1.top, box2.top)
        val intersectRight = min(box1.right, box2.right)
        val intersectBottom = min(box1.bottom, box2.bottom)
        
        if (intersectRight < intersectLeft || intersectBottom < intersectTop) {
            return 0f
        }
        
        val intersectArea = (intersectRight - intersectLeft) * (intersectBottom - intersectTop)
        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)
        val unionArea = box1Area + box2Area - intersectArea
        
        return intersectArea / unionArea
    }
    
    fun close() {
        interpreter?.close()
        gpuDelegate?.close()
        interpreter = null
        gpuDelegate = null
    }
    
    private fun getCocoLabels(): List<String> {
        return listOf(
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat",
            "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack",
            "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball",
            "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket",
            "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
            "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair",
            "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse",
            "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink",
            "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier",
            "toothbrush"
        )
    }
}
