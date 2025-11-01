package com.example.aroundme.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * OCR Service using ML Kit Text Recognition
 * Detects text and arrow signs from images
 */
class OcrService {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    companion object {
        private const val TAG = "OcrService"
        
        // Arrow detection patterns
        private val ARROW_PATTERNS = mapOf(
            "←" to "left",
            "→" to "right",
            "↑" to "up",
            "↓" to "down",
            "<-" to "left",
            "->" to "right",
            "^" to "up",
            "v" to "down"
        )
    }
    
    data class OcrResult(
        val text: String,
        val arrows: List<String>,
        val hasText: Boolean
    )
    
    /**
     * Process image and extract text and arrows
     */
    suspend fun processImage(bitmap: Bitmap): OcrResult {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(inputImage).await()
            
            val extractedText = result.text.trim()
            val detectedArrows = detectArrows(extractedText)
            
            Log.d(TAG, "OCR Result: ${extractedText.take(100)}")
            if (detectedArrows.isNotEmpty()) {
                Log.d(TAG, "Arrows detected: $detectedArrows")
            }
            
            OcrResult(
                text = extractedText,
                arrows = detectedArrows,
                hasText = extractedText.isNotEmpty()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image", e)
            OcrResult(
                text = "",
                arrows = emptyList(),
                hasText = false
            )
        }
    }
    
    /**
     * Detect arrow symbols and directions
     */
    private fun detectArrows(text: String): List<String> {
        val arrows = mutableListOf<String>()
        
        ARROW_PATTERNS.forEach { (symbol, direction) ->
            if (text.contains(symbol, ignoreCase = true)) {
                arrows.add(direction)
            }
        }
        
        // Also check for text-based directions
        val lowerText = text.lowercase()
        when {
            lowerText.contains("left") -> arrows.add("left")
            lowerText.contains("right") -> arrows.add("right")
            lowerText.contains("up") || lowerText.contains("ahead") -> arrows.add("up")
            lowerText.contains("down") || lowerText.contains("below") -> arrows.add("down")
        }
        
        return arrows.distinct()
    }
    
    /**
     * Format OCR result for speech
     */
    fun formatForSpeech(result: OcrResult): String {
        val parts = mutableListOf<String>()
        
        if (result.arrows.isNotEmpty()) {
            val arrowText = result.arrows.joinToString(", ")
            parts.add("Arrow pointing $arrowText")
        }
        
        if (result.text.isNotEmpty()) {
            // Clean up text for speech
            val cleanText = result.text
                .replace("\n", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            
            if (cleanText.length > 200) {
                parts.add("The sign says: ${cleanText.take(200)}... and more")
            } else {
                parts.add("The sign says: $cleanText")
            }
        }
        
        return when {
            parts.isEmpty() -> "I don't see any text or signs"
            else -> parts.joinToString(". ")
        }
    }
    
    /**
     * Cleanup resources
     */
    fun close() {
        textRecognizer.close()
    }
}
