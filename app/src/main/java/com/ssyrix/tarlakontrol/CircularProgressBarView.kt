package com.ssyrix.tarlakontrol

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class CircularProgressBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var progress = 0f
    private var progressColor = Color.GREEN
    private var progressBackgroundColor = Color.GRAY
    private var strokeWidth = 16f

    private val paintCircle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val paintProgress = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    init {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CircularProgressBarView,
            0, 0
        )

        progress = typedArray.getFloat(R.styleable.CircularProgressBarView_progress, 0f)
        progressColor = typedArray.getColor(R.styleable.CircularProgressBarView_progressColor, Color.GREEN)
        progressBackgroundColor = typedArray.getColor(R.styleable.CircularProgressBarView_progressBackgroundColor, Color.GRAY)
        strokeWidth = typedArray.getDimension(R.styleable.CircularProgressBarView_strokeWidth, 16f)

        typedArray.recycle()

        // Uygula!
        paintCircle.color = progressBackgroundColor
        paintCircle.strokeWidth = strokeWidth

        paintProgress.color = progressColor
        paintProgress.strokeWidth = strokeWidth
    }

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 100f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val radius = width.coerceAtMost(height) / 2f - strokeWidth
        val cx = width / 2f
        val cy = height / 2f

        canvas.drawCircle(cx, cy, radius, paintCircle)

        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val sweepAngle = 360 * (progress / 100f)
        canvas.drawArc(rect, -90f, sweepAngle, false, paintProgress)
    }
}
