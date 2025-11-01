package com.surendramaran.yolov8tflite

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

class NavigationManager(
    private val context: Context,
    private val onTtsReady: () -> Unit
) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var isSpeaking = false
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    
    private var lastAnnouncementTime = 0L
    private val announcementInterval = 2000L // Announce every 2 seconds
    
    // Track previous obstacles to avoid repetitive announcements
    private var previousObstacles = listOf<DetectedObstacle>()

    init {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported")
            } else {
                isTtsReady = true
                textToSpeech?.setSpeechRate(1.1f) // Slightly faster for better responsiveness
                
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                    }

                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                    }
                })
                
                onTtsReady()
            }
        } else {
            Log.e(TAG, "TTS Initialization failed")
        }
    }

    fun analyzeAndAnnounce(boundingBoxes: List<BoundingBox>) {
        if (!isTtsReady || isSpeaking) return
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnnouncementTime < announcementInterval) return
        
        val obstacles = categorizeObstacles(boundingBoxes)
        
        if (obstacles.isEmpty()) {
            return
        }
        
        // Check for significant changes
        if (hasSignificantChange(obstacles)) {
            val navigationMessage = generateNavigationMessage(obstacles)
            speak(navigationMessage)
            lastAnnouncementTime = currentTime
            previousObstacles = obstacles
            
            // Vibrate based on danger level
            vibrateForObstacles(obstacles)
        }
    }

    private fun categorizeObstacles(boundingBoxes: List<BoundingBox>): List<DetectedObstacle> {
        return boundingBoxes.map { box ->
            val position = determinePosition(box.cx)
            val distance = estimateDistance(box.h)
            val dangerLevel = calculateDangerLevel(distance, box.h)
            
            DetectedObstacle(
                name = box.clsName,
                position = position,
                distance = distance,
                dangerLevel = dangerLevel,
                centerX = box.cx,
                centerY = box.cy,
                height = box.h,
                width = box.w
            )
        }.sortedByDescending { it.dangerLevel }
    }

    private fun determinePosition(centerX: Float): Position {
        return when {
            centerX < 0.35f -> Position.LEFT
            centerX > 0.65f -> Position.RIGHT
            else -> Position.CENTER
        }
    }

    private fun estimateDistance(height: Float): Distance {
        return when {
            height > 0.6f -> Distance.VERY_CLOSE
            height > 0.4f -> Distance.CLOSE
            height > 0.2f -> Distance.MEDIUM
            else -> Distance.FAR
        }
    }

    private fun calculateDangerLevel(distance: Distance, height: Float): Int {
        val distanceScore = when(distance) {
            Distance.VERY_CLOSE -> 4
            Distance.CLOSE -> 3
            Distance.MEDIUM -> 2
            Distance.FAR -> 1
        }
        val sizeScore = (height * 5).toInt()
        return distanceScore + sizeScore
    }

    private fun hasSignificantChange(newObstacles: List<DetectedObstacle>): Boolean {
        if (previousObstacles.isEmpty()) return true
        if (newObstacles.size != previousObstacles.size) return true
        
        // Check for new high-danger obstacles
        val highDangerNew = newObstacles.filter { it.dangerLevel >= 5 }
        val highDangerOld = previousObstacles.filter { it.dangerLevel >= 5 }
        
        return highDangerNew.size != highDangerOld.size ||
               highDangerNew.any { new -> 
                   highDangerOld.none { old -> 
                       old.name == new.name && old.position == new.position 
                   }
               }
    }

    private fun generateNavigationMessage(obstacles: List<DetectedObstacle>): String {
        if (obstacles.isEmpty()) return "Path clear"
        
        // Focus on the most dangerous obstacle
        val primaryObstacle = obstacles.first()
        
        val distanceText = when(primaryObstacle.distance) {
            Distance.VERY_CLOSE -> "Warning! Very close"
            Distance.CLOSE -> "Close"
            Distance.MEDIUM -> "Ahead"
            Distance.FAR -> ""
        }
        
        val positionText = when(primaryObstacle.position) {
            Position.LEFT -> "on your left"
            Position.RIGHT -> "on your right"
            Position.CENTER -> "in front"
        }
        
        val message = buildString {
            append(distanceText)
            if (distanceText.isNotEmpty()) append(" ")
            append(primaryObstacle.name)
            append(" ")
            append(positionText)
            
            // Add guidance
            if (primaryObstacle.dangerLevel >= 5) {
                val guidance = when(primaryObstacle.position) {
                    Position.LEFT -> ". Move right"
                    Position.RIGHT -> ". Move left"
                    Position.CENTER -> ". Stop or move aside"
                }
                append(guidance)
            }
        }
        
        return message
    }

    private fun vibrateForObstacles(obstacles: List<DetectedObstacle>) {
        if (!vibrator.hasVibrator()) return
        
        val maxDanger = obstacles.maxOfOrNull { it.dangerLevel } ?: 0
        
        val pattern = when {
            maxDanger >= 7 -> longArrayOf(0, 200, 100, 200) // Very dangerous
            maxDanger >= 5 -> longArrayOf(0, 150, 100, 150) // Dangerous
            maxDanger >= 3 -> longArrayOf(0, 100) // Moderate
            else -> return // No vibration for low danger
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (isTtsReady && !isSpeaking) {
            textToSpeech?.speak(text, queueMode, null, UUID.randomUUID().toString())
        }
    }

    fun speakImmediate(text: String) {
        if (isTtsReady) {
            textToSpeech?.stop()
            isSpeaking = false
            speak(text, TextToSpeech.QUEUE_FLUSH)
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        isSpeaking = false
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    companion object {
        private const val TAG = "NavigationManager"
    }
}

data class DetectedObstacle(
    val name: String,
    val position: Position,
    val distance: Distance,
    val dangerLevel: Int,
    val centerX: Float,
    val centerY: Float,
    val height: Float,
    val width: Float
)

enum class Position {
    LEFT, CENTER, RIGHT
}

enum class Distance {
    VERY_CLOSE, CLOSE, MEDIUM, FAR
}
