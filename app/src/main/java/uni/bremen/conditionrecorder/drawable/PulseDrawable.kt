package uni.bremen.conditionrecorder.drawable

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.animation.DecelerateInterpolator


class PulseDrawable(private val color: Int) : Drawable() {
    
    private lateinit var centerPaint: Paint
    private lateinit var pulsePaint: Paint
    
    private var fullSizeRadius: Float = 0.toFloat()
    private var currentExpandAnimationValue = 0f
    private var currentAlphaAnimationValue = 255

    override fun getOpacity(): Int = pulsePaint.alpha

    private val pulseColor: Int
        get() = Color.argb(currentAlphaAnimationValue, Color.red(color),
                Color.green(color),
                Color.blue(color))

    init {
        initializeDrawable()
    }

    private fun initializeDrawable() {
        preparePaints()
        prepareAnimation()
    }

    private fun prepareAnimation() {
        val expandAnimator = ValueAnimator.ofFloat(0f, 1f)
        expandAnimator.repeatCount = ValueAnimator.INFINITE
        expandAnimator.repeatMode = ValueAnimator.RESTART
        expandAnimator.addUpdateListener { animation ->
            currentExpandAnimationValue = animation.animatedValue as Float
            if (currentExpandAnimationValue == 0f) {
                currentAlphaAnimationValue = 255
            }
            invalidateSelf()
        }
        val alphaAnimator = ValueAnimator.ofInt(255, 0)
        alphaAnimator.startDelay = ANIMATION_DURATION_IN_MS / 4
        alphaAnimator.repeatCount = ValueAnimator.INFINITE
        alphaAnimator.repeatMode = ValueAnimator.RESTART
        alphaAnimator.addUpdateListener { animation -> currentAlphaAnimationValue = animation.animatedValue as Int }
        val animation = AnimatorSet()
        animation.playTogether(expandAnimator, alphaAnimator)
        animation.duration = ANIMATION_DURATION_IN_MS
        animation.interpolator = DecelerateInterpolator()
        animation.start()
    }

    private fun preparePaints() {
        pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        pulsePaint.style = Paint.Style.FILL
        centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        centerPaint.style = Paint.Style.FILL
        centerPaint.color = color
    }

    override fun setAlpha(alpha: Int) {
        pulsePaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter) {}

    override fun draw(canvas: Canvas) {
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        calculateFullSizeRadius()
        preparePaintShader()
        renderPulse(canvas, centerX, centerY)
        renderCenterArea(canvas, centerX, centerY)
    }

    private fun renderPulse(canvas: Canvas, centerX: Float, centerY: Float) {
        val currentRadius = fullSizeRadius * currentExpandAnimationValue
        if (currentRadius > MINIMUM_RADIUS) {
            canvas.drawCircle(centerX, centerY, currentRadius, pulsePaint)
        }
    }

    private fun renderCenterArea(canvas: Canvas, centerX: Float, centerY: Float) {
        val currentCenterAreaRadius = fullSizeRadius * CENTER_AREA_SIZE
        if (currentCenterAreaRadius > MINIMUM_RADIUS) {
            canvas.save()
            val left = centerX - currentCenterAreaRadius
            val top = centerY - currentCenterAreaRadius
            val right = centerX + currentCenterAreaRadius
            val bottom = centerY + currentCenterAreaRadius
            canvas.clipRect(left, top, right, bottom)
            canvas.drawCircle(centerX, centerY, currentCenterAreaRadius, centerPaint)
            canvas.restore()
        }
    }

    private fun preparePaintShader() {
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        val radius = Math.min(bounds.width(), bounds.height()) / 2F
        if (radius > MINIMUM_RADIUS) {
            val edgeColor = pulseColor
            val centerColor = Color.argb(PULSE_START_COLOR_OPACITY, Color.red(color),
                    Color.green(color),
                    Color.blue(color))
            pulsePaint.shader = RadialGradient(
                    centerX, centerY, radius,
                    centerColor, edgeColor,
                    Shader.TileMode.CLAMP)
        } else {
            pulsePaint.shader = null
        }
    }

    private fun calculateFullSizeRadius() {
        val minimumDiameter = Math.min(bounds.width(), bounds.height())
        fullSizeRadius = minimumDiameter / 2F
    }

    companion object {
        const val CENTER_AREA_SIZE = 0.6f
        const val PULSE_START_COLOR_OPACITY = 0
        const val MINIMUM_RADIUS = 0f
        const val ANIMATION_DURATION_IN_MS = 1500L
    }
}