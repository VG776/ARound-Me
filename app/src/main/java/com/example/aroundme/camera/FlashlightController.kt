package com.example.aroundme.camera

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.camera.core.Camera

/**
 * Flashlight Controller
 * Automatically controls camera flashlight based on lighting conditions
 */
class FlashlightController(private val context: Context) {
    
    private var camera: Camera? = null
    private var isFlashlightOn = false
    private var autoMode = true
    
    companion object {
        private const val TAG = "FlashlightController"
        private const val LOW_LIGHT_THRESHOLD = 70f // Increased for better low-light detection
        private const val HYSTERESIS = 10f // Prevent flickering
    }
    
    private var lastBrightness = 128f
    
    /**
     * Set the camera instance for torch control
     */
    fun setCamera(camera: Camera?) {
        this.camera = camera
    }
    
    /**
     * Toggle flashlight on/off
     */
    fun toggleFlashlight(): Boolean {
        autoMode = false
        return if (isFlashlightOn) {
            turnOff()
            false
        } else {
            turnOn()
            true
        }
    }
    
    /**
     * Turn on flashlight
     */
    fun turnOn() {
        try {
            camera?.cameraControl?.enableTorch(true)
            isFlashlightOn = true
            Log.d(TAG, "💡 Flashlight turned ON")
        } catch (e: Exception) {
            Log.e(TAG, "Error turning on flashlight", e)
        }
    }
    
    /**
     * Turn off flashlight
     */
    fun turnOff() {
        try {
            camera?.cameraControl?.enableTorch(false)
            isFlashlightOn = false
            Log.d(TAG, "💡 Flashlight turned OFF")
        } catch (e: Exception) {
            Log.e(TAG, "Error turning off flashlight", e)
        }
    }
    
    /**
     * Auto-adjust flashlight based on ambient brightness with hysteresis
     */
    fun autoAdjust(brightness: Float) {
        if (!autoMode) return
        
        lastBrightness = brightness
        
        when {
            brightness < LOW_LIGHT_THRESHOLD && !isFlashlightOn -> {
                turnOn()
                Log.d(TAG, "💡 Auto-enabled flashlight (brightness: ${brightness.toInt()})")
            }
            brightness >= (LOW_LIGHT_THRESHOLD + HYSTERESIS) && isFlashlightOn -> {
                turnOff()
                Log.d(TAG, "💡 Auto-disabled flashlight (brightness: ${brightness.toInt()})")
            }
        }
    }
    
    /**
     * Estimate brightness from bitmap (simple method)
     * Returns a value between 0 (dark) and 255 (bright)
     */
    fun estimateBrightness(bitmap: android.graphics.Bitmap): Float {
        return try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            
            var totalBrightness = 0L
            for (pixel in pixels) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                // Simple brightness calculation
                totalBrightness += (r + g + b) / 3
            }
            
            (totalBrightness / pixels.size).toFloat()
        } catch (e: Exception) {
            Log.e(TAG, "Error estimating brightness", e)
            128f // Default to medium brightness
        }
    }
    
    /**
     * Enable/disable auto mode
     */
    fun setAutoMode(enabled: Boolean) {
        autoMode = enabled
        if (!enabled && isFlashlightOn) {
            turnOff()
        }
    }
    
    /**
     * Check if flashlight is currently on
     */
    fun isOn(): Boolean = isFlashlightOn
    
    /**
     * Cleanup
     */
    fun cleanup() {
        if (isFlashlightOn) {
            turnOff()
        }
    }
}
