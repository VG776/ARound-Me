package com.surendramaran.yolov8tflite

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()
    private var dangerBoxPaint = Paint()
    private var warningBoxPaint = Paint()

    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        results = listOf()
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        dangerBoxPaint.reset()
        warningBoxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 42f
        textBackgroundPaint.alpha = 200

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 42f
        textPaint.isFakeBoldText = true

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 6F
        boxPaint.style = Paint.Style.STROKE
        
        // Danger box for very close objects
        dangerBoxPaint.color = Color.RED
        dangerBoxPaint.strokeWidth = 8F
        dangerBoxPaint.style = Paint.Style.STROKE
        dangerBoxPaint.pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
        
        // Warning box for close objects
        warningBoxPaint.color = Color.YELLOW
        warningBoxPaint.strokeWidth = 7F
        warningBoxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results.forEach {
            val left = it.x1 * width
            val top = it.y1 * height
            val right = it.x2 * width
            val bottom = it.y2 * height

            // Choose paint based on distance (height indicates proximity)
            val paint = when {
                it.h > 0.6f -> dangerBoxPaint // Very close
                it.h > 0.4f -> warningBoxPaint // Close
                else -> boxPaint // Normal distance
            }
            
            canvas.drawRect(left, top, right, bottom, paint)
            
            // Draw label with better styling
            val drawableText = "${it.clsName} ${(it.cnf * 100).toInt()}%"

            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            
            // Draw rounded rectangle for text background
            val rectF = RectF(
                left,
                top - textHeight - BOUNDING_RECT_TEXT_PADDING * 2,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING * 2,
                top
            )
            canvas.drawRoundRect(rectF, 8f, 8f, textBackgroundPaint)
            canvas.drawText(
                drawableText, 
                left + BOUNDING_RECT_TEXT_PADDING, 
                top - BOUNDING_RECT_TEXT_PADDING, 
                textPaint
            )

            // Draw position indicator for accessibility
            if (it.h > 0.4f) {
                drawPositionIndicator(canvas, it, left, top, right, bottom)
            }
        }
    }
    
    private fun drawPositionIndicator(canvas: Canvas, box: BoundingBox, left: Float, top: Float, right: Float, bottom: Float) {
        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2
        
        val indicatorPaint = Paint().apply {
            color = if (box.h > 0.6f) Color.RED else Color.YELLOW
            style = Paint.Style.FILL
            alpha = 150
        }
        
        canvas.drawCircle(centerX, centerY, 20f, indicatorPaint)
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}