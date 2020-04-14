package com.cafrecode.views.circurlarpercent

import android.animation.TimeInterpolator
import android.annotation.TargetApi
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.Paint.Cap
import android.os.Build
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import com.cafrecode.views.circurlarpercent.ColorUtils.getRGBGradient
import java.text.DecimalFormat

/**
 * An circle view, similar to Android's ProgressBar.
 * Can be used in 'value mode' or 'spinning mode'.
 *
 *
 * In spinning mode it can be used like a intermediate progress bar.
 *
 *
 * In value mode it can be used as a progress bar or to visualize any other value.
 * Setting a value is fully animated. There are also nice transitions from animating to value mode.
 *
 *
 * Typical use case would be to load a new value. During the loading time set the CircleView to spinning.
 * As soon as you get your value, just set it with [.setValueAnimated].
 *
 * @author Jakob Grabner, based on the Progress wheel of Todd Davies
 * https://github.com/Todd-Davies/CircleView
 *
 *
 * Licensed under the Creative Commons Attribution 3.0 license see:
 * http://creativecommons.org/licenses/by/3.0/
 */
class CircleProgressView(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {
    //----------------------------------
//region members
//Colors (with defaults)
    private val mBarColorStandard = -0xff6978 //stylish blue
    protected var mLayoutHeight = 0
    protected var mLayoutWidth = 0
    //Rectangles
    protected var mCircleBounds = RectF()
    protected var mInnerCircleBound = RectF()
    protected var mCenter: PointF? = null
    /**
     * Maximum size of the text.
     */
    protected var mOuterTextBounds = RectF()
    /**
     * Actual size of the text.
     */
    protected var mActualTextBounds = RectF()
    protected var mUnitBounds = RectF()
    protected var mCircleOuterContour = RectF()
    protected var mCircleInnerContour = RectF()
    //value animation
    var mDirection =
        Direction.CW
    var currentValue = 0f
    var mValueTo = 0f
    var mValueFrom = 0f
    /**
     * The max value of the progress bar. Used to calculate the percentage of the current value.
     * The bar fills according to the percentage. The default value is 100.
     *
     * @param _maxValue The max value.
     */
    var maxValue = 100f
    /**
     * The min value allowed of the progress bar. Used to limit the min possible value of the current value.
     *
     * @param _minValueAllowed The min value allowed.
     */
    var minValueAllowed = 0f
    /**
     * The max value allowed of the progress bar. Used to limit the max possible value of the current value.
     *
     * @param _maxValueAllowed The max value allowed.
     */
    var maxValueAllowed = -1f
    // spinner animation
    var mSpinningBarLengthCurrent = 0f
    var mSpinningBarLengthOrig = 42f
    var mCurrentSpinnerDegreeValue = 0f
    //Animation
//The amount of degree to move the bar by on each draw
    var mSpinSpeed = 2.8f
    //Enable spin
    var mSpin = false
    /**
     * The animation duration in ms
     */
    var mAnimationDuration = 900.0
    /**
     * @return The number of ms to wait between each draw call.
     */
    /**
     * @param delayMillis The number of ms to wait between each draw call.
     */
    //The number of milliseconds to wait in between each draw
    var delayMillis = 10
    // helper for AnimationState.END_SPINNING_START_ANIMATING
    var mDrawBarWhileSpinning = false
    //The animation handler containing the animation state machine.
    var mAnimationHandler = AnimationHandler(this)
    //The current state of the animation state machine.
    var mAnimationState =
        AnimationState.IDLE
    var mAnimationStateChangedListener: AnimationStateChangedListener? = null
    private var mBarWidth = 40
    private var mRimWidth = 40
    private var mStartAngle = 270
    private var mOuterContourSize = 1f
    private var mInnerContourSize = 1f
    // Bar start/end width and type
    private var mBarStartEndLineWidth = 0
    //----------------------------------
//region getter/setter
    var barStartEndLine = BarStartEndLine.NONE
        private set
    private var mBarStartEndLineColor = -0x56000000
    private var mBarStartEndLineSweep = 10f
    //Default text sizes
    private var mUnitTextSize = 10
    private var mTextSize = 10
    //Text scale
    private var mTextScale = 1f
    private var mUnitScale = 1f
    private var mOuterContourColor = -0x56000000
    private var mInnerContourColor = -0x56000000
    private var mSpinnerColor = mBarColorStandard //stylish blue
    private var mBackgroundCircleColor = 0x00000000 //transparent
    private var mRimColor = -0x557c2f37
    private var mTextColor = -0x1000000
    private var mUnitColor = -0x1000000
    private var mIsAutoColorEnabled = false
    var barColors = intArrayOf(
        mBarColorStandard //stylish blue
    )
        private set
    //Caps
    private var mBarStrokeCap = Cap.BUTT
    private var mSpinnerStrokeCap = Cap.BUTT
    //Paints
    private val mBarPaint = Paint()
    private var mShaderlessBarPaint: Paint? = null
    private val mBarSpinnerPaint = Paint()
    private val mBarStartEndLinePaint = Paint()
    private val mBackgroundCirclePaint = Paint()
    private val mRimPaint = Paint()
    private val mTextPaint = Paint()
    private val mUnitTextPaint = Paint()
    private val mOuterContourPaint = Paint()
    private val mInnerContourPaint = Paint()
    //Other
// The text to show
    private var mText: String? = ""
    private var mTextLength = 0
    private var mUnit = ""
    private var mUnitPosition = UnitPosition.RIGHT_TOP
    /**
     * Indicates if the given text, the current percentage, or the current value should be shown.
     */
    private var mTextMode = TextMode.PERCENT
    private var mIsAutoTextSize = false
    private var mShowUnit = false
    //clipping
    private var mClippingBitmap: Bitmap? = null
    private val mMaskPaint: Paint
    /**
     * @return The relative size (scale factor) of the unit text size to the text size
     */
    /**
     * Relative size of the unite string to the value string.
     */
    var relativeUniteSize = 1f
        private set
    private var mSeekModeEnabled = false
    private var mShowTextWhileSpinning = false
    private var mShowBlock = false
    private var mBlockCount = 18
    private var mBlockScale = 0.9f
    private var mBlockDegree = 360 / mBlockCount.toFloat()
    private var mBlockScaleDegree = mBlockDegree * mBlockScale
    var roundToBlock = false
    var roundToWholeNumber = false
    private var mTouchEventCount = 0
    private var onProgressChangedListener: OnProgressChangedListener? = null
    private var previousProgressChangedValue = 0f
    private var decimalFormat = DecimalFormat("0")
    // Text typeface
    private var textTypeface: Typeface? = null
    private var unitTextTypeface: Typeface? = null

    /**
     * Allows to add a line to the start/end of the bar
     *
     * @param _barWidth        The width of the stroke on the start/end of the bar in pixel.
     * @param _barStartEndLine The type of line on the start/end of the bar.
     * @param _lineColor       The line color
     * @param _sweepWidth      The sweep amount in degrees for the start and end bars to cover.
     */
    fun setBarStartEndLine(
        _barWidth: Int,
        _barStartEndLine: BarStartEndLine, @ColorInt _lineColor: Int,
        _sweepWidth: Float
    ) {
        mBarStartEndLineWidth = _barWidth
        barStartEndLine = _barStartEndLine
        mBarStartEndLineColor = _lineColor
        mBarStartEndLineSweep = _sweepWidth
    }

    /**
     * @param _barStrokeCap The stroke cap of the progress bar.
     */
    var barStrokeCap: Cap
        get() = mBarStrokeCap
        set(_barStrokeCap) {
            mBarStrokeCap = _barStrokeCap
            mBarPaint.strokeCap = _barStrokeCap
            if (mBarStrokeCap != Cap.BUTT) {
                mShaderlessBarPaint = Paint(mBarPaint)
                mShaderlessBarPaint!!.shader = null
                mShaderlessBarPaint!!.color = barColors[0]
            }
        }

    /**
     * @param barWidth The width of the progress bar in pixel.
     */
    var barWidth: Int
        get() = mBarWidth
        set(barWidth) {
            mBarWidth = barWidth
            mBarPaint.strokeWidth = barWidth.toFloat()
            mBarSpinnerPaint.strokeWidth = barWidth.toFloat()
        }

    var blockCount: Int
        get() = mBlockCount
        set(blockCount) {
            if (blockCount > 1) {
                mShowBlock = true
                mBlockCount = blockCount
                mBlockDegree = 360.0f / blockCount
                mBlockScaleDegree = mBlockDegree * mBlockScale
            } else {
                mShowBlock = false
            }
        }

    var blockScale: Float
        get() = mBlockScale
        set(blockScale) {
            if (blockScale >= 0.0f && blockScale <= 1.0f) {
                mBlockScale = blockScale
                mBlockScaleDegree = mBlockDegree * blockScale
            }
        }

    /**
     * @param _contourColor The color of the background contour of the circle.
     */
    var outerContourColor: Int
        get() = mOuterContourColor
        set(_contourColor) {
            mOuterContourColor = _contourColor
            mOuterContourPaint.color = _contourColor
        }

    /**
     * @param _contourSize The size of the background contour of the circle.
     */
    var outerContourSize: Float
        get() = mOuterContourSize
        set(_contourSize) {
            mOuterContourSize = _contourSize
            mOuterContourPaint.strokeWidth = _contourSize
        }

    /**
     * @param _contourColor The color of the background contour of the circle.
     */
    var innerContourColor: Int
        get() = mInnerContourColor
        set(_contourColor) {
            mInnerContourColor = _contourColor
            mInnerContourPaint.color = _contourColor
        }

    /**
     * @param _contourSize The size of the background contour of the circle.
     */
    var innerContourSize: Float
        get() = mInnerContourSize
        set(_contourSize) {
            mInnerContourSize = _contourSize
            mInnerContourPaint.strokeWidth = _contourSize
        }

    val fillColor: Int
        get() = mBackgroundCirclePaint.color

    /**
     * @param rimColor The color of the rim around the Circle.
     */
    var rimColor: Int
        get() = mRimColor
        set(rimColor) {
            mRimColor = rimColor
            mRimPaint.color = rimColor
        }

    var rimShader: Shader?
        get() = mRimPaint.shader
        set(shader) {
            mRimPaint.shader = shader
        }

    /**
     * @param rimWidth The width in pixel of the rim around the circle
     */
    var rimWidth: Int
        get() = mRimWidth
        set(rimWidth) {
            mRimWidth = rimWidth
            mRimPaint.strokeWidth = rimWidth.toFloat()
        }

    fun getSpinSpeed(): Float {
        return field
    }

    /**
     * The amount of degree to move the bar on every draw call.
     *
     * @param spinSpeed the speed of the spinner
     */
    fun setSpinSpeed(spinSpeed: Float) {
        field = spinSpeed
    }

    fun getSpinnerStrokeCap(): Cap {
        return mSpinnerStrokeCap
    }

    /**
     * @param _spinnerStrokeCap The stroke cap of the progress bar in spinning mode.
     */
    fun setSpinnerStrokeCap(_spinnerStrokeCap: Cap) {
        mSpinnerStrokeCap = _spinnerStrokeCap
        mBarSpinnerPaint.strokeCap = _spinnerStrokeCap
    }

    fun getStartAngle(): Int {
        return mStartAngle
    }

    fun setStartAngle(
        @IntRange(
            from = 0,
            to = 360
        ) _startAngle: Int
    ) { // get a angle between 0 and 360
        mStartAngle = normalizeAngle(_startAngle.toFloat()).toInt()
    }

    fun calcTextColor(): Int {
        return mTextColor
    }

    /**
     * Sets the text color.
     * You also need to  set [.setTextColorAuto] to false to see your color.
     *
     * @param textColor the color
     */
    fun setTextColor(@ColorInt textColor: Int) {
        mTextColor = textColor
        mTextPaint.color = textColor
    }

    /**
     * @return The scale value
     */
    fun getTextScale(): Float {
        return mTextScale
    }

    /**
     * Scale factor for main text in the center of the circle view.
     * Only used if auto text size is enabled.
     *
     * @param _textScale The scale value.
     */
    fun setTextScale(@FloatRange(from = 0.0) _textScale: Float) {
        mTextScale = _textScale
    }

    fun getTextSize(): Int {
        return mTextSize
    }

    /**
     * Text size of the text string. Disables auto text size
     * If auto text size is on, use [.setTextScale] to scale textSize.
     *
     * @param textSize The text size of the unit.
     */
    fun setTextSize(@IntRange(from = 0) textSize: Int) {
        mTextPaint.textSize = textSize.toFloat()
        mTextSize = textSize
        mIsAutoTextSize = false
    }

    fun getUnit(): String {
        return mUnit
    }

    /**
     * @param _unit The unit to show next to the current value.
     * You also need to set [.setUnitVisible] to true.
     */
    fun setUnit(_unit: String?) {
        mUnit = _unit ?: ""
        invalidate()
    }

    /**
     * @return The scale value
     */
    fun getUnitScale(): Float {
        return mUnitScale
    }

    /**
     * Scale factor for unit text next to the main text.
     * Only used if auto text size is enabled.
     *
     * @param _unitScale The scale value.
     */
    fun setUnitScale(@FloatRange(from = 0.0) _unitScale: Float) {
        mUnitScale = _unitScale
    }

    fun getUnitSize(): Int {
        return mUnitTextSize
    }

    /**
     * Text size of the unit string. Only used if text size is also set. (So automatic text size
     * calculation is off. see [.setTextSize]).
     * If auto text size is on, use [.setUnitScale] to scale unit size.
     *
     * @param unitSize The text size of the unit.
     */
    fun setUnitSize(@IntRange(from = 0) unitSize: Int) {
        mUnitTextSize = unitSize
        mUnitTextPaint.textSize = unitSize.toFloat()
    }

    /**
     * @return true if auto text size is enabled, false otherwise.
     */
    fun isAutoTextSize(): Boolean {
        return mIsAutoTextSize
    }

    /**
     * @param _autoTextSize true to enable auto text size calculation.
     */
    fun setAutoTextSize(_autoTextSize: Boolean) {
        mIsAutoTextSize = _autoTextSize
    }

    fun isSeekModeEnabled(): Boolean {
        return mSeekModeEnabled
    }

    fun setSeekModeEnabled(_seekModeEnabled: Boolean) {
        mSeekModeEnabled = _seekModeEnabled
    }

    fun isShowBlock(): Boolean {
        return mShowBlock
    }

    fun setShowBlock(showBlock: Boolean) {
        mShowBlock = showBlock
    }

    fun isShowTextWhileSpinning(): Boolean {
        return mShowTextWhileSpinning
    }

    /**
     * @param shouldDrawTextWhileSpinning True to show text in spinning mode, false to hide it.
     */
    fun setShowTextWhileSpinning(shouldDrawTextWhileSpinning: Boolean) {
        mShowTextWhileSpinning = shouldDrawTextWhileSpinning
    }

    fun isUnitVisible(): Boolean {
        return mShowUnit
    }

    /**
     * @param _showUnit True to show unit, false to hide it.
     */
    fun setUnitVisible(_showUnit: Boolean) {
        if (_showUnit != mShowUnit) {
            mShowUnit = _showUnit
            triggerReCalcTextSizesAndPositions() // triggers recalculating text sizes
        }
    }

    /**
     * Sets the color of progress bar.
     *
     * @param barColors One or more colors. If more than one color is specified, a gradient of the colors is used.
     */
    fun setBarColor(@ColorInt vararg barColors: Int) {
        this.barColors = barColors
        setupBarPaint()
    }

    /**
     * @param _clippingBitmap The bitmap used for clipping. Set to null to disable clipping.
     * Default: No clipping.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    fun setClippingBitmap(_clippingBitmap: Bitmap?) {
        mClippingBitmap = if (width > 0 && height > 0) {
            Bitmap.createScaledBitmap(_clippingBitmap!!, width, height, false)
        } else {
            _clippingBitmap
        }
        if (mClippingBitmap == null) { // enable HW acceleration
            setLayerType(LAYER_TYPE_HARDWARE, null)
        } else { // disable HW acceleration
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
    }

    /**
     * Sets the background color of the entire Progress Circle.
     * Set the color to 0x00000000 (Color.TRANSPARENT) to hide it.
     *
     * @param circleColor the color.
     */
    fun setFillCircleColor(@ColorInt circleColor: Int) {
        mBackgroundCircleColor = circleColor
        mBackgroundCirclePaint.color = circleColor
    }

    fun setOnAnimationStateChangedListener(_animationStateChangedListener: AnimationStateChangedListener?) {
        mAnimationStateChangedListener = _animationStateChangedListener
    }

    fun setOnProgressChangedListener(listener: OnProgressChangedListener?) {
        onProgressChangedListener = listener
    }

    /**
     * @param _color The color of progress the bar in spinning mode.
     */
    fun setSpinBarColor(@ColorInt _color: Int) {
        mSpinnerColor = _color
        mBarSpinnerPaint.color = mSpinnerColor
    }

    /**
     * Length of spinning bar in degree.
     *
     * @param barLength length in degree
     */
    fun setSpinningBarLength(@FloatRange(from = 0.0) barLength: Float) {
        mSpinningBarLengthOrig = barLength
        mSpinningBarLengthCurrent = mSpinningBarLengthOrig
    }

    /**
     * Set the text in the middle of the circle view.
     * You need also set the [TextMode] to TextMode.TEXT to see the text.
     *
     * @param text The text to show
     */
    fun setText(text: String?) {
        mText = text ?: ""
        invalidate()
    }

    /**
     * If auto text color is enabled, the text color  and the unit color is always the same as the rim color.
     * This is useful if the rim has multiple colors (color gradient), than the text will always have
     * the color of the tip of the rim.
     *
     * @param isEnabled true to enable, false to disable
     */
    fun setTextColorAuto(isEnabled: Boolean) {
        mIsAutoColorEnabled = isEnabled
    }

    /**
     * Sets the auto text mode.
     *
     * @param _textValue The mode
     */
    fun setTextMode(_textValue: TextMode) {
        mTextMode = _textValue
    }

    /**
     * @param typeface The typeface to use for the text
     */
    fun setTextTypeface(typeface: Typeface?) {
        mTextPaint.typeface = typeface
    }

    /**
     * Sets the unit text color.
     * Also sets [.setTextColorAuto] to false
     *
     * @param unitColor The color.
     */
    fun setUnitColor(@ColorInt unitColor: Int) {
        mUnitColor = unitColor
        mUnitTextPaint.color = unitColor
        mIsAutoColorEnabled = false
    }

    fun setUnitPosition(_unitPosition: UnitPosition) {
        mUnitPosition = _unitPosition
        triggerReCalcTextSizesAndPositions() // triggers recalculating text sizes
    }

    /**
     * @param typeface The typeface to use for the unit text
     */
    fun setUnitTextTypeface(typeface: Typeface?) {
        mUnitTextPaint.typeface = typeface
    }

    /**
     * @param _relativeUniteSize The relative scale factor of the unit text size to the text size.
     * Only useful for autotextsize=true; Effects both, the unit text size and the text size.
     */
    fun setUnitToTextScale(@FloatRange(from = 0.0) _relativeUniteSize: Float) {
        relativeUniteSize = _relativeUniteSize
        triggerReCalcTextSizesAndPositions()
    }

    /**
     * Sets the direction of circular motion (clockwise or counter-clockwise).
     */
    fun setDirection(direction: Direction) {
        mDirection = direction
    }

    /**
     * Set the value of the circle view without an animation.
     * Stops any currently active animations.
     *
     * @param _value The value.
     */
    fun setValue(_value: Float) { // round to block
        var _value = _value
        if (mShowBlock && roundToBlock) {
            val value_per_block = maxValue / mBlockCount.toFloat()
            _value = Math.round(_value / value_per_block) * value_per_block
        } else if (roundToWholeNumber) { // round to whole number
            _value = Math.round(_value).toFloat()
        }
        // respect min and max values allowed
        _value = Math.max(minValueAllowed, _value)
        if (maxValueAllowed >= 0) _value = Math.min(maxValueAllowed, _value)
        val msg = Message()
        msg.what = AnimationMsg.SET_VALUE.ordinal
        msg.obj = floatArrayOf(_value, _value)
        mAnimationHandler.sendMessage(msg)
        triggerOnProgressChanged(_value)
    }

    /**
     * Sets the value of the circle view with an animation.
     * The current value is used as the start value of the animation
     *
     * @param _valueTo value after animation
     */
    fun setValueAnimated(_valueTo: Float) {
        setValueAnimated(_valueTo, 1200)
    }

    /**
     * Sets the value of the circle view with an animation.
     * The current value is used as the start value of the animation
     *
     * @param _valueTo           value after animation
     * @param _animationDuration the duration of the animation in milliseconds.
     */
    fun setValueAnimated(_valueTo: Float, _animationDuration: Long) {
        setValueAnimated(currentValue, _valueTo, _animationDuration)
    }

    /**
     * Sets the value of the circle view with an animation.
     *
     * @param _valueFrom         start value of the animation
     * @param _valueTo           value after animation
     * @param _animationDuration the duration of the animation in milliseconds
     */
    fun setValueAnimated(
        _valueFrom: Float,
        _valueTo: Float,
        _animationDuration: Long
    ) { // round to block
        var _valueTo = _valueTo
        if (mShowBlock && roundToBlock) {
            val value_per_block = maxValue / mBlockCount.toFloat()
            _valueTo = Math.round(_valueTo / value_per_block) * value_per_block
        } else if (roundToWholeNumber) {
            _valueTo = Math.round(_valueTo).toFloat()
        }
        // respect min and max values allowed
        _valueTo = Math.max(minValueAllowed, _valueTo)
        if (maxValueAllowed >= 0) _valueTo = Math.min(maxValueAllowed, _valueTo)
        mAnimationDuration = _animationDuration.toDouble()
        val msg = Message()
        msg.what = AnimationMsg.SET_VALUE_ANIMATED.ordinal
        msg.obj = floatArrayOf(_valueFrom, _valueTo)
        mAnimationHandler.sendMessage(msg)
        triggerOnProgressChanged(_valueTo)
    }

    fun getDecimalFormat(): DecimalFormat {
        return decimalFormat
    }

    fun setDecimalFormat(decimalFormat: DecimalFormat?) {
        requireNotNull(decimalFormat) { "decimalFormat must not be null!" }
        this.decimalFormat = decimalFormat
    }

    /**
     * Sets interpolator for value animations.
     *
     * @param interpolator the interpolator
     */
    fun setValueInterpolator(interpolator: TimeInterpolator?) {
        mAnimationHandler.setValueInterpolator(interpolator!!)
    }

    /**
     * Sets the interpolator for length changes of the bar.
     *
     * @param interpolator the interpolator
     */
    fun setLengthChangeInterpolator(interpolator: TimeInterpolator?) {
        mAnimationHandler.setLengthChangeInterpolator(interpolator!!)
    }
    //endregion getter/setter
//----------------------------------
    /**
     * Parse the attributes passed to the view from the XML
     *
     * @param a the attributes to parse
     */
    private fun parseAttributes(a: TypedArray) {
        barWidth = a.getDimension(
            R.styleable.CircleProgressView_cpv_barWidth,
            mBarWidth.toFloat()
        ).toInt()
        rimWidth = a.getDimension(
            R.styleable.CircleProgressView_cpv_rimWidth,
            mRimWidth.toFloat()
        ).toInt()
        setSpinSpeed(
            a.getFloat(
                R.styleable.CircleProgressView_cpv_spinSpeed,
                mSpinSpeed
            ) as Int.toFloat()
        )
        setSpin(
            a.getBoolean(
                R.styleable.CircleProgressView_cpv_spin,
                mSpin
            )
        )
        setDirection(
            Direction.values()[a.getInt(
                R.styleable.CircleProgressView_cpv_direction,
                0
            )]
        )
        val value =
            a.getFloat(R.styleable.CircleProgressView_cpv_value, currentValue)
        setValue(value)
        currentValue = value
        if (a.hasValue(R.styleable.CircleProgressView_cpv_barColor) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor1) && a.hasValue(
                R.styleable.CircleProgressView_cpv_barColor2
            ) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor3)
        ) {
            barColors = intArrayOf(
                a.getColor(
                    R.styleable.CircleProgressView_cpv_barColor,
                    mBarColorStandard
                ),
                a.getColor(R.styleable.CircleProgressView_cpv_barColor1, mBarColorStandard),
                a.getColor(R.styleable.CircleProgressView_cpv_barColor2, mBarColorStandard),
                a.getColor(R.styleable.CircleProgressView_cpv_barColor3, mBarColorStandard)
            )
        } else if (a.hasValue(R.styleable.CircleProgressView_cpv_barColor) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor1) && a.hasValue(
                R.styleable.CircleProgressView_cpv_barColor2
            )
        ) {
            barColors = intArrayOf(
                a.getColor(
                    R.styleable.CircleProgressView_cpv_barColor,
                    mBarColorStandard
                ),
                a.getColor(R.styleable.CircleProgressView_cpv_barColor1, mBarColorStandard),
                a.getColor(R.styleable.CircleProgressView_cpv_barColor2, mBarColorStandard)
            )
        } else if (a.hasValue(R.styleable.CircleProgressView_cpv_barColor) && a.hasValue(R.styleable.CircleProgressView_cpv_barColor1)) {
            barColors = intArrayOf(
                a.getColor(
                    R.styleable.CircleProgressView_cpv_barColor,
                    mBarColorStandard
                ), a.getColor(R.styleable.CircleProgressView_cpv_barColor1, mBarColorStandard)
            )
        } else {
            barColors = intArrayOf(
                a.getColor(
                    R.styleable.CircleProgressView_cpv_barColor,
                    mBarColorStandard
                ), a.getColor(R.styleable.CircleProgressView_cpv_barColor, mBarColorStandard)
            )
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_barStrokeCap)) {
            barStrokeCap = StrokeCap.values()[a.getInt(
                R.styleable.CircleProgressView_cpv_barStrokeCap,
                0
            )].paintCap
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_barStartEndLineWidth) && a.hasValue(R.styleable.CircleProgressView_cpv_barStartEndLine)) {
            setBarStartEndLine(
                a.getDimension(R.styleable.CircleProgressView_cpv_barStartEndLineWidth, 0f).toInt(),
                BarStartEndLine.values()[a.getInt(
                    R.styleable.CircleProgressView_cpv_barStartEndLine,
                    3
                )],
                a.getColor(
                    R.styleable.CircleProgressView_cpv_barStartEndLineColor,
                    mBarStartEndLineColor
                ),
                a.getFloat(
                    R.styleable.CircleProgressView_cpv_barStartEndLineSweep,
                    mBarStartEndLineSweep
                )
            )
        }
        setSpinBarColor(a.getColor(R.styleable.CircleProgressView_cpv_spinColor, mSpinnerColor))
        setSpinningBarLength(
            a.getFloat(
                R.styleable.CircleProgressView_cpv_spinBarLength,
                mSpinningBarLengthOrig
            )
        )
        if (a.hasValue(R.styleable.CircleProgressView_cpv_textSize)) {
            setTextSize(
                a.getDimension(
                    R.styleable.CircleProgressView_cpv_textSize,
                    mTextSize.toFloat()
                ).toInt()
            )
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_unitSize)) {
            setUnitSize(
                a.getDimension(
                    R.styleable.CircleProgressView_cpv_unitSize,
                    mUnitTextSize.toFloat()
                ).toInt()
            )
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_textColor)) {
            setTextColor(a.getColor(R.styleable.CircleProgressView_cpv_textColor, mTextColor))
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_unitColor)) {
            setUnitColor(a.getColor(R.styleable.CircleProgressView_cpv_unitColor, mUnitColor))
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_autoTextColor)) {
            setTextColorAuto(
                a.getBoolean(
                    R.styleable.CircleProgressView_cpv_autoTextColor,
                    mIsAutoColorEnabled
                )
            )
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_autoTextSize)) {
            setAutoTextSize(
                a.getBoolean(
                    R.styleable.CircleProgressView_cpv_autoTextSize,
                    mIsAutoTextSize
                )
            )
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_textMode)) {
            setTextMode(
                TextMode.values()[a.getInt(
                    R.styleable.CircleProgressView_cpv_textMode,
                    0
                )]
            )
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_unitPosition)) {
            setUnitPosition(
                UnitPosition.values()[a.getInt(
                    R.styleable.CircleProgressView_cpv_unitPosition,
                    3
                )]
            )
        }
        //if the mText is empty, show current percentage value
        if (a.hasValue(R.styleable.CircleProgressView_cpv_text)) {
            setText(a.getString(R.styleable.CircleProgressView_cpv_text))
        }
        setUnitToTextScale(a.getFloat(R.styleable.CircleProgressView_cpv_unitToTextScale, 1f))
        rimColor = a.getColor(
            R.styleable.CircleProgressView_cpv_rimColor,
            mRimColor
        )
        setFillCircleColor(
            a.getColor(
                R.styleable.CircleProgressView_cpv_fillColor,
                mBackgroundCircleColor
            )
        )
        outerContourColor = a.getColor(
            R.styleable.CircleProgressView_cpv_outerContourColor,
            mOuterContourColor
        )
        outerContourSize = a.getDimension(
            R.styleable.CircleProgressView_cpv_outerContourSize,
            mOuterContourSize
        )
        innerContourColor = a.getColor(
            R.styleable.CircleProgressView_cpv_innerContourColor,
            mInnerContourColor
        )
        innerContourSize = a.getDimension(
            R.styleable.CircleProgressView_cpv_innerContourSize,
            mInnerContourSize
        )
        maxValue = a.getFloat(R.styleable.CircleProgressView_cpv_maxValue, maxValue)
        minValueAllowed = a.getFloat(
            R.styleable.CircleProgressView_cpv_minValueAllowed,
            minValueAllowed
        )
        maxValueAllowed = a.getFloat(
            R.styleable.CircleProgressView_cpv_maxValueAllowed,
            maxValueAllowed
        )
        roundToBlock = a.getBoolean(
            R.styleable.CircleProgressView_cpv_roundToBlock,
            roundToBlock
        )
        roundToWholeNumber = a.getBoolean(
            R.styleable.CircleProgressView_cpv_roundToWholeNumber,
            roundToWholeNumber
        )
        setUnit(a.getString(R.styleable.CircleProgressView_cpv_unit))
        setUnitVisible(a.getBoolean(R.styleable.CircleProgressView_cpv_showUnit, mShowUnit))
        setTextScale(a.getFloat(R.styleable.CircleProgressView_cpv_textScale, mTextScale))
        setUnitScale(a.getFloat(R.styleable.CircleProgressView_cpv_unitScale, mUnitScale))
        setSeekModeEnabled(
            a.getBoolean(
                R.styleable.CircleProgressView_cpv_seekMode,
                mSeekModeEnabled
            )
        )
        setStartAngle(a.getInt(R.styleable.CircleProgressView_cpv_startAngle, mStartAngle))
        setShowTextWhileSpinning(
            a.getBoolean(
                R.styleable.CircleProgressView_cpv_showTextInSpinningMode,
                mShowTextWhileSpinning
            )
        )
        if (a.hasValue(R.styleable.CircleProgressView_cpv_blockCount)) {
            blockCount = a.getInt(R.styleable.CircleProgressView_cpv_blockCount, 1)
            blockScale = a.getFloat(R.styleable.CircleProgressView_cpv_blockScale, 0.9f)
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_textTypeface)) {
            try {
                textTypeface = Typeface.createFromAsset(
                    context.assets,
                    a.getString(R.styleable.CircleProgressView_cpv_textTypeface)
                )
            } catch (exception: Exception) { // error while trying to inflate typeface (is the path set correctly?)
            }
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_unitTypeface)) {
            try {
                unitTextTypeface = Typeface.createFromAsset(
                    context.assets,
                    a.getString(R.styleable.CircleProgressView_cpv_unitTypeface)
                )
            } catch (exception: Exception) { // error while trying to inflate typeface (is the path set correctly?)
            }
        }
        if (a.hasValue(R.styleable.CircleProgressView_cpv_decimalFormat)) {
            try {
                val pattern =
                    a.getString(R.styleable.CircleProgressView_cpv_decimalFormat)
                if (pattern != null) {
                    decimalFormat = DecimalFormat(pattern)
                }
            } catch (exception: Exception) {
                Log.w(TAG, exception.message)
            }
        }
        // Recycle
        a.recycle()
    }

    /*
     * When this is called, make the view square.
     * From: http://www.jayway.com/2012/12/12/creating-custom-android-views-part-4-measuring-and-how-to-force-a-view-to-be-square/
     *
     */
    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) { // The first thing that happen is that we call the superclass
// implementation of onMeasure. The reason for that is that measuring
// can be quite a complex process and calling the super method is a
// convenient way to get most of this complexity handled.
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // We can’t use getWidth() or getHeight() here. During the measuring
// pass the view has not gotten its final size yet (this happens first
// at the start of the layout pass) so we have to use getMeasuredWidth()
// and getMeasuredHeight().
        val size: Int
        val width = measuredWidth
        val height = measuredHeight
        val widthWithoutPadding = width - paddingLeft - paddingRight
        val heightWithoutPadding = height - paddingTop - paddingBottom
        // Finally we have some simple logic that calculates the size of the view
// and calls setMeasuredDimension() to set that size.
// Before we compare the width and height of the view, we remove the padding,
// and when we set the dimension we add it back again. Now the actual content
// of the view will be square, but, depending on the padding, the total dimensions
// of the view might not be.
        size = if (widthWithoutPadding > heightWithoutPadding) {
            heightWithoutPadding
        } else {
            widthWithoutPadding
        }
        // If you override onMeasure() you have to call setMeasuredDimension().
// This is how you report back the measured size.  If you don’t call
// setMeasuredDimension() the parent will throw an exception and your
// application will crash.
// We are calling the onMeasure() method of the superclass so we don’t
// actually need to call setMeasuredDimension() since that takes care
// of that. However, the purpose with overriding onMeasure() was to
// change the default behaviour and to do that we need to call
// setMeasuredDimension() with our own values.
        setMeasuredDimension(
            size + paddingLeft + paddingRight,
            size + paddingTop + paddingBottom
        )
    }

    /**
     * Use onSizeChanged instead of onAttachedToWindow to get the dimensions of the view,
     * because this method is called after measuring the dimensions of MATCH_PARENT and WRAP_CONTENT.
     * Use this dimensions to setup the bounds and paints.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Share the dimensions
        mLayoutWidth = w
        mLayoutHeight = h
        setupBounds()
        setupBarPaint()
        if (mClippingBitmap != null) {
            mClippingBitmap =
                Bitmap.createScaledBitmap(mClippingBitmap!!, width, height, false)
        }
        invalidate()
    }

    //----------------------------------
// region helper
    private fun calcTextSizeForCircle(
        _text: String,
        _textPaint: Paint,
        _circleBounds: RectF
    ): Float { //get mActualTextBounds bounds
        val innerCircleBounds = getInnerCircleRect(_circleBounds)
        return calcTextSizeForRect(
            _text,
            _textPaint,
            innerCircleBounds
        )
    }

    private fun getInnerCircleRect(_circleBounds: RectF): RectF {
        val circleWidth = +_circleBounds.width() - Math.max(
            mBarWidth,
            mRimWidth
        ) - mOuterContourSize - mInnerContourSize.toDouble()
        val width = circleWidth / 2.0 * Math.sqrt(2.0)
        val widthDelta = (_circleBounds.width() - width.toFloat()) / 2f
        var scaleX = 1f
        var scaleY = 1f
        if (isUnitVisible()) {
            when (mUnitPosition) {
                UnitPosition.TOP, UnitPosition.BOTTOM -> {
                    scaleX =
                        1.1f // scaleX square to rectangle, so the longer text with unit fits better
                    scaleY = 0.88f
                }
                UnitPosition.LEFT_TOP, UnitPosition.RIGHT_TOP, UnitPosition.LEFT_BOTTOM, UnitPosition.RIGHT_BOTTOM -> {
                    scaleX =
                        0.77f // scaleX square to rectangle, so the longer text with unit fits better
                    scaleY = 1.33f
                }
            }
        }
        return RectF(
            _circleBounds.left + widthDelta * scaleX,
            _circleBounds.top + widthDelta * scaleY,
            _circleBounds.right - widthDelta * scaleX,
            _circleBounds.bottom - widthDelta * scaleY
        )
    }

    private fun triggerOnProgressChanged(value: Float) {
        if (onProgressChangedListener != null && value != previousProgressChangedValue) {
            onProgressChangedListener!!.onProgressChanged(value)
            previousProgressChangedValue = value
        }
    }

    private fun triggerReCalcTextSizesAndPositions() {
        mTextLength = -1
        mOuterTextBounds = getInnerCircleRect(mCircleBounds)
        invalidate()
    }

    private fun calcTextColor(value: Double): Int {
        return if (barColors.size > 1) {
            val percent = 1f / maxValue * value
            var low = Math.floor((barColors.size - 1) * percent).toInt()
            var high = low + 1
            if (low < 0) {
                low = 0
                high = 1
            } else if (high >= barColors.size) {
                low = barColors.size - 2
                high = barColors.size - 1
            }
            getRGBGradient(
                barColors[low],
                barColors[high],
                (1 - (barColors.size - 1) * percent % 1.0).toFloat()
            )
        } else if (barColors.size == 1) {
            barColors[0]
        } else {
            Color.BLACK
        }
    }

    private fun setTextSizeAndTextBoundsWithAutoTextSize(
        unitGapWidthHalf: Float,
        unitWidth: Float,
        unitGapHeightHalf: Float,
        unitHeight: Float,
        text: String
    ) {
        var textRect = mOuterTextBounds
        if (mShowUnit) { //shrink text Rect so that there is space for the unit
            textRect = when (mUnitPosition) {
                UnitPosition.TOP -> RectF(
                    mOuterTextBounds.left,
                    mOuterTextBounds.top + unitHeight + unitGapHeightHalf,
                    mOuterTextBounds.right,
                    mOuterTextBounds.bottom
                )
                UnitPosition.BOTTOM -> RectF(
                    mOuterTextBounds.left,
                    mOuterTextBounds.top,
                    mOuterTextBounds.right,
                    mOuterTextBounds.bottom - unitHeight - unitGapHeightHalf
                )
                UnitPosition.LEFT_TOP, UnitPosition.LEFT_BOTTOM -> RectF(
                    mOuterTextBounds.left + unitWidth + unitGapWidthHalf,
                    mOuterTextBounds.top,
                    mOuterTextBounds.right,
                    mOuterTextBounds.bottom
                )
                UnitPosition.RIGHT_TOP, UnitPosition.RIGHT_BOTTOM -> RectF(
                    mOuterTextBounds.left,
                    mOuterTextBounds.top,
                    mOuterTextBounds.right - unitWidth - unitGapWidthHalf,
                    mOuterTextBounds.bottom
                )
                else -> RectF(
                    mOuterTextBounds.left,
                    mOuterTextBounds.top,
                    mOuterTextBounds.right - unitWidth - unitGapWidthHalf,
                    mOuterTextBounds.bottom
                )
            }
        }
        mTextPaint.textSize = calcTextSizeForRect(
            text,
            mTextPaint,
            textRect
        ) * mTextScale
        mActualTextBounds = calcTextBounds(text, mTextPaint, textRect) // center text in text rect
    }

    private fun setTextSizeAndTextBoundsWithFixedTextSize(text: String) {
        mTextPaint.textSize = mTextSize.toFloat()
        mActualTextBounds = calcTextBounds(text, mTextPaint, mCircleBounds) //center text in circle
    }

    private fun setUnitTextBoundsAndSizeWithAutoTextSize(
        unitGapWidthHalf: Float,
        unitWidth: Float,
        unitGapHeightHalf: Float,
        unitHeight: Float
    ) { //calc the rectangle containing the unit text
        mUnitBounds = when (mUnitPosition) {
            UnitPosition.TOP -> {
                RectF(
                    mOuterTextBounds.left,
                    mOuterTextBounds.top,
                    mOuterTextBounds.right,
                    mOuterTextBounds.top + unitHeight - unitGapHeightHalf
                )
            }
            UnitPosition.BOTTOM -> RectF(
                mOuterTextBounds.left,
                mOuterTextBounds.bottom - unitHeight + unitGapHeightHalf,
                mOuterTextBounds.right,
                mOuterTextBounds.bottom
            )
            UnitPosition.LEFT_TOP, UnitPosition.LEFT_BOTTOM -> {
                RectF(
                    mOuterTextBounds.left,
                    mOuterTextBounds.top,
                    mOuterTextBounds.left + unitWidth - unitGapWidthHalf,
                    mOuterTextBounds.top + unitHeight
                )
            }
            UnitPosition.RIGHT_TOP, UnitPosition.RIGHT_BOTTOM -> {
                RectF(
                    mOuterTextBounds.right - unitWidth + unitGapWidthHalf,
                    mOuterTextBounds.top,
                    mOuterTextBounds.right,
                    mOuterTextBounds.top + unitHeight
                )
            }
            else -> {
                RectF(
                    mOuterTextBounds.right - unitWidth + unitGapWidthHalf,
                    mOuterTextBounds.top,
                    mOuterTextBounds.right,
                    mOuterTextBounds.top + unitHeight
                )
            }
        }
        mUnitTextPaint.textSize = calcTextSizeForRect(
            mUnit,
            mUnitTextPaint,
            mUnitBounds
        ) * mUnitScale
        mUnitBounds = calcTextBounds(
            mUnit,
            mUnitTextPaint,
            mUnitBounds
        ) // center text in rectangle and reuse it
        when (mUnitPosition) {
            UnitPosition.LEFT_TOP, UnitPosition.RIGHT_TOP -> {
                //move unite to top of text
                val dy = mActualTextBounds.top - mUnitBounds.top
                mUnitBounds.offset(0f, dy)
            }
            UnitPosition.LEFT_BOTTOM, UnitPosition.RIGHT_BOTTOM -> {
                //move unite to bottom of text
                val dy = mActualTextBounds.bottom - mUnitBounds.bottom
                mUnitBounds.offset(0f, dy)
            }
        }
    }

    private fun setUnitTextBoundsAndSizeWithFixedTextSize(
        unitGapWidth: Float,
        unitGapHeight: Float
    ) {
        mUnitTextPaint.textSize = mUnitTextSize.toFloat()
        mUnitBounds = calcTextBounds(
            mUnit,
            mUnitTextPaint,
            mOuterTextBounds
        ) // center text in rectangle and reuse it
        when (mUnitPosition) {
            UnitPosition.TOP -> mUnitBounds.offsetTo(
                mUnitBounds.left,
                mActualTextBounds.top - unitGapHeight - mUnitBounds.height()
            )
            UnitPosition.BOTTOM -> mUnitBounds.offsetTo(
                mUnitBounds.left,
                mActualTextBounds.bottom + unitGapHeight
            )
            UnitPosition.LEFT_TOP, UnitPosition.LEFT_BOTTOM -> mUnitBounds.offsetTo(
                mActualTextBounds.left - unitGapWidth - mUnitBounds.width(),
                mUnitBounds.top
            )
            UnitPosition.RIGHT_TOP, UnitPosition.RIGHT_BOTTOM -> mUnitBounds.offsetTo(
                mActualTextBounds.right + unitGapWidth,
                mUnitBounds.top
            )
            else -> mUnitBounds.offsetTo(mActualTextBounds.right + unitGapWidth, mUnitBounds.top)
        }
        when (mUnitPosition) {
            UnitPosition.LEFT_TOP, UnitPosition.RIGHT_TOP -> {
                //move unite to top of text
                val dy = mActualTextBounds.top - mUnitBounds.top
                mUnitBounds.offset(0f, dy)
            }
            UnitPosition.LEFT_BOTTOM, UnitPosition.RIGHT_BOTTOM -> {
                //move unite to bottom of text
                val dy = mActualTextBounds.bottom - mUnitBounds.bottom
                mUnitBounds.offset(0f, dy)
            }
        }
    }

    /**
     * Returns the bounding rectangle of the given _text, with the size and style defined in the _textPaint centered in the middle of the _textBounds
     *
     * @param _text       The text.
     * @param _textPaint  The paint defining the text size and style.
     * @param _textBounds The rect where the text will be centered.
     * @return The bounding box of the text centered in the _textBounds.
     */
    private fun calcTextBounds(
        _text: String,
        _textPaint: Paint,
        _textBounds: RectF
    ): RectF {
        val textBoundsTmp = Rect()
        //get current text bounds
        _textPaint.getTextBounds(_text, 0, _text.length, textBoundsTmp)
        val width = textBoundsTmp.left + textBoundsTmp.width().toFloat()
        val height =
            textBoundsTmp.bottom + textBoundsTmp.height() * 0.93f // the height of calcTextBounds is a bit to high, therefore  * 0.93
        //center in circle
        val textRect = RectF()
        textRect.left = _textBounds.left + (_textBounds.width() - width) / 2
        textRect.top = _textBounds.top + (_textBounds.height() - height) / 2
        textRect.right = textRect.left + width
        textRect.bottom = textRect.top + height
        return textRect
    }
    //endregion helper
//----------------------------------
//----------------------------------
//region Setting up stuff
    /**
     * Set the bounds of the component
     */
    private fun setupBounds() { // Width should equal to Height, find the min value to setup the circle
        val minValue = Math.min(mLayoutWidth, mLayoutHeight)
        // Calc the Offset if needed
        val xOffset = mLayoutWidth - minValue
        val yOffset = mLayoutHeight - minValue
        // Add the offset
        val paddingTop = this.paddingTop + (yOffset / 2).toFloat()
        val paddingBottom = this.paddingBottom + (yOffset / 2).toFloat()
        val paddingLeft = this.paddingLeft + (xOffset / 2).toFloat()
        val paddingRight = this.paddingRight + (xOffset / 2).toFloat()
        val width = width //this.getLayoutParams().width;
        val height = height //this.getLayoutParams().height;
        val circleWidthHalf =
            if (mBarWidth / 2f > mRimWidth / 2f + mOuterContourSize) mBarWidth / 2f else mRimWidth / 2f + mOuterContourSize
        mCircleBounds = RectF(
            paddingLeft + circleWidthHalf,
            paddingTop + circleWidthHalf,
            width - paddingRight - circleWidthHalf,
            height - paddingBottom - circleWidthHalf
        )
        mInnerCircleBound = RectF(
            paddingLeft + mBarWidth,
            paddingTop + mBarWidth,
            width - paddingRight - mBarWidth,
            height - paddingBottom - mBarWidth
        )
        mOuterTextBounds = getInnerCircleRect(mCircleBounds)
        mCircleInnerContour = RectF(
            mCircleBounds.left + mRimWidth / 2.0f + mInnerContourSize / 2.0f,
            mCircleBounds.top + mRimWidth / 2.0f + mInnerContourSize / 2.0f,
            mCircleBounds.right - mRimWidth / 2.0f - mInnerContourSize / 2.0f,
            mCircleBounds.bottom - mRimWidth / 2.0f - mInnerContourSize / 2.0f
        )
        mCircleOuterContour = RectF(
            mCircleBounds.left - mRimWidth / 2.0f - mOuterContourSize / 2.0f,
            mCircleBounds.top - mRimWidth / 2.0f - mOuterContourSize / 2.0f,
            mCircleBounds.right + mRimWidth / 2.0f + mOuterContourSize / 2.0f,
            mCircleBounds.bottom + mRimWidth / 2.0f + mOuterContourSize / 2.0f
        )
        mCenter = PointF(mCircleBounds.centerX(), mCircleBounds.centerY())
    }

    private fun setupBarPaint() {
        if (barColors.size > 1) {
            mBarPaint.shader = SweepGradient(
                mCircleBounds.centerX(),
                mCircleBounds.centerY(),
                barColors,
                null
            )
            val matrix = Matrix()
            mBarPaint.shader.getLocalMatrix(matrix)
            matrix.postTranslate(-mCircleBounds.centerX(), -mCircleBounds.centerY())
            matrix.postRotate(mStartAngle.toFloat())
            matrix.postTranslate(mCircleBounds.centerX(), mCircleBounds.centerY())
            mBarPaint.shader.setLocalMatrix(matrix)
            mBarPaint.color = barColors[0]
        } else if (barColors.size == 1) {
            mBarPaint.color = barColors[0]
            mBarPaint.shader = null
        } else {
            mBarPaint.color = mBarColorStandard
            mBarPaint.shader = null
        }
        mBarPaint.isAntiAlias = true
        mBarPaint.strokeCap = mBarStrokeCap
        mBarPaint.style = Paint.Style.STROKE
        mBarPaint.strokeWidth = mBarWidth.toFloat()
        if (mBarStrokeCap != Cap.BUTT) {
            mShaderlessBarPaint = Paint(mBarPaint)
            mShaderlessBarPaint!!.shader = null
            mShaderlessBarPaint!!.color = barColors[0]
        }
    }

    /**
     * Setup all paints.
     * Call only if changes to color or size properties are not visible.
     */
    fun setupPaints() {
        setupBarPaint()
        setupBarSpinnerPaint()
        setupOuterContourPaint()
        setupInnerContourPaint()
        setupUnitTextPaint()
        setupTextPaint()
        setupBackgroundCirclePaint()
        setupRimPaint()
        setupBarStartEndLinePaint()
    }

    private fun setupBarStartEndLinePaint() {
        mBarStartEndLinePaint.color = mBarStartEndLineColor
        mBarStartEndLinePaint.isAntiAlias = true
        mBarStartEndLinePaint.style = Paint.Style.STROKE
        mBarStartEndLinePaint.strokeWidth = mBarStartEndLineWidth.toFloat()
    }

    private fun setupOuterContourPaint() {
        mOuterContourPaint.color = mOuterContourColor
        mOuterContourPaint.isAntiAlias = true
        mOuterContourPaint.style = Paint.Style.STROKE
        mOuterContourPaint.strokeWidth = mOuterContourSize
    }

    private fun setupInnerContourPaint() {
        mInnerContourPaint.color = mInnerContourColor
        mInnerContourPaint.isAntiAlias = true
        mInnerContourPaint.style = Paint.Style.STROKE
        mInnerContourPaint.strokeWidth = mInnerContourSize
    }

    private fun setupUnitTextPaint() {
        mUnitTextPaint.style = Paint.Style.FILL
        mUnitTextPaint.isAntiAlias = true
        if (unitTextTypeface != null) {
            mUnitTextPaint.typeface = unitTextTypeface
        }
    }

    private fun setupTextPaint() {
        mTextPaint.isSubpixelText = true
        mTextPaint.isLinearText = true
        mTextPaint.typeface = Typeface.MONOSPACE
        mTextPaint.color = mTextColor
        mTextPaint.style = Paint.Style.FILL
        mTextPaint.isAntiAlias = true
        mTextPaint.textSize = mTextSize.toFloat()
        if (textTypeface != null) {
            mTextPaint.typeface = textTypeface
        } else {
            mTextPaint.typeface = Typeface.MONOSPACE
        }
    }

    private fun setupBackgroundCirclePaint() {
        mBackgroundCirclePaint.color = mBackgroundCircleColor
        mBackgroundCirclePaint.isAntiAlias = true
        mBackgroundCirclePaint.style = Paint.Style.FILL
    }

    private fun setupRimPaint() {
        mRimPaint.color = mRimColor
        mRimPaint.isAntiAlias = true
        mRimPaint.style = Paint.Style.STROKE
        mRimPaint.strokeWidth = mRimWidth.toFloat()
    }

    private fun setupBarSpinnerPaint() {
        mBarSpinnerPaint.isAntiAlias = true
        mBarSpinnerPaint.strokeCap = mSpinnerStrokeCap
        mBarSpinnerPaint.style = Paint.Style.STROKE
        mBarSpinnerPaint.strokeWidth = mBarWidth.toFloat()
        mBarSpinnerPaint.color = mSpinnerColor
    }

    //endregion Setting up stuff
//----------------------------------
//----------------------------------
//region draw all the things
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (DEBUG) {
            drawDebug(canvas)
        }
        val degrees = 360f / maxValue * currentValue
        // Draw the background circle
        if (mBackgroundCircleColor != 0) {
            canvas.drawArc(mInnerCircleBound, 360f, 360f, false, mBackgroundCirclePaint)
        }
        //Draw the rim
        if (mRimWidth > 0) {
            if (!mShowBlock) {
                canvas.drawArc(mCircleBounds, 360f, 360f, false, mRimPaint)
            } else {
                drawBlocks(canvas, mCircleBounds, mStartAngle.toFloat(), 360f, false, mRimPaint)
            }
        }
        //Draw outer contour
        if (mOuterContourSize > 0) {
            canvas.drawArc(mCircleOuterContour, 360f, 360f, false, mOuterContourPaint)
        }
        //Draw outer contour
        if (mInnerContourSize > 0) {
            canvas.drawArc(mCircleInnerContour, 360f, 360f, false, mInnerContourPaint)
        }
        //Draw spinner
        if (mAnimationState === AnimationState.SPINNING || mAnimationState === AnimationState.END_SPINNING) {
            drawSpinner(canvas)
            if (mShowTextWhileSpinning) {
                drawTextWithUnit(canvas)
            }
        } else if (mAnimationState === AnimationState.END_SPINNING_START_ANIMATING) { //draw spinning arc
            drawSpinner(canvas)
            if (mDrawBarWhileSpinning) {
                drawBar(canvas, degrees)
                drawTextWithUnit(canvas)
            } else if (mShowTextWhileSpinning) {
                drawTextWithUnit(canvas)
            }
        } else {
            drawBar(canvas, degrees)
            drawTextWithUnit(canvas)
        }
        if (mClippingBitmap != null) {
            canvas.drawBitmap(mClippingBitmap!!, 0f, 0f, mMaskPaint)
        }
        if (mBarStartEndLineWidth > 0 && barStartEndLine !== BarStartEndLine.NONE) {
            drawStartEndLine(canvas, degrees)
        }
    }

    private fun drawStartEndLine(
        _canvas: Canvas,
        _degrees: Float
    ) {
        if (_degrees == 0f) return
        var startAngle =
            if (mDirection === Direction.CW) mStartAngle.toFloat() else mStartAngle - _degrees
        startAngle -= mBarStartEndLineSweep / 2f
        if (barStartEndLine === BarStartEndLine.START || barStartEndLine === BarStartEndLine.BOTH) {
            _canvas.drawArc(
                mCircleBounds,
                startAngle,
                mBarStartEndLineSweep,
                false,
                mBarStartEndLinePaint
            )
        }
        if (barStartEndLine === BarStartEndLine.END || barStartEndLine === BarStartEndLine.BOTH) {
            _canvas.drawArc(
                mCircleBounds,
                startAngle + _degrees,
                mBarStartEndLineSweep,
                false,
                mBarStartEndLinePaint
            )
        }
    }

    private fun drawDebug(canvas: Canvas) {
        val innerRectPaint = Paint()
        innerRectPaint.color = Color.YELLOW
        canvas.drawRect(mCircleBounds, innerRectPaint)
    }

    private fun drawBlocks(
        _canvas: Canvas,
        circleBounds: RectF,
        startAngle: Float,
        _degrees: Float,
        userCenter: Boolean,
        paint: Paint
    ) {
        var tmpDegree = 0.0f
        while (tmpDegree < _degrees) {
            _canvas.drawArc(
                circleBounds,
                startAngle + tmpDegree,
                Math.min(mBlockScaleDegree, _degrees - tmpDegree),
                userCenter,
                paint
            )
            tmpDegree += mBlockDegree
        }
    }

    private fun drawSpinner(canvas: Canvas) {
        if (mSpinningBarLengthCurrent < 0) {
            mSpinningBarLengthCurrent = 1f
        }
        val startAngle: Float
        startAngle = if (mDirection === Direction.CW) {
            mStartAngle + mCurrentSpinnerDegreeValue - mSpinningBarLengthCurrent
        } else {
            mStartAngle - mCurrentSpinnerDegreeValue
        }
        canvas.drawArc(
            mCircleBounds, startAngle, mSpinningBarLengthCurrent, false,
            mBarSpinnerPaint
        )
    }

    private fun drawTextWithUnit(canvas: Canvas) {
        val relativeGapHeight: Float
        val relativeGapWidth: Float
        val relativeHeight: Float
        val relativeWidth: Float
        when (mUnitPosition) {
            UnitPosition.TOP, UnitPosition.BOTTOM -> {
                relativeGapWidth = 0.05f //gap size between text and unit
                relativeGapHeight = 0.025f //gap size between text and unit
                relativeHeight = 0.25f * relativeUniteSize
                relativeWidth = 0.4f * relativeUniteSize
            }
            UnitPosition.LEFT_TOP, UnitPosition.RIGHT_TOP, UnitPosition.LEFT_BOTTOM, UnitPosition.RIGHT_BOTTOM -> {
                relativeGapWidth = 0.05f //gap size between text and unit
                relativeGapHeight = 0.025f //gap size between text and unit
                relativeHeight = 0.55f * relativeUniteSize
                relativeWidth = 0.3f * relativeUniteSize
            }
            else -> {
                relativeGapWidth = 0.05f
                relativeGapHeight = 0.025f
                relativeHeight = 0.55f * relativeUniteSize
                relativeWidth = 0.3f * relativeUniteSize
            }
        }
        val unitGapWidthHalf = mOuterTextBounds.width() * relativeGapWidth / 2f
        val unitWidth = mOuterTextBounds.width() * relativeWidth
        val unitGapHeightHalf = mOuterTextBounds.height() * relativeGapHeight / 2f
        val unitHeight = mOuterTextBounds.height() * relativeHeight
        var update = false
        //Draw Text
        if (mIsAutoColorEnabled) {
            mTextPaint.color = calcTextColor(currentValue.toDouble())
        }
        //set text
        val text: String
        text = when (mTextMode) {
            TextMode.TEXT -> if (mText != null) mText!! else ""
            TextMode.PERCENT -> decimalFormat.format(100f / maxValue * currentValue.toDouble())
            TextMode.VALUE -> decimalFormat.format(currentValue.toDouble())
            else -> if (mText != null) mText!! else ""
        }
        // only re-calc position and size if string length changed
        if (mTextLength != text.length) {
            update = true
            mTextLength = text.length
            if (mTextLength == 1) {
                mOuterTextBounds = getInnerCircleRect(mCircleBounds)
                mOuterTextBounds = RectF(
                    mOuterTextBounds.left + mOuterTextBounds.width() * 0.1f,
                    mOuterTextBounds.top,
                    mOuterTextBounds.right - mOuterTextBounds.width() * 0.1f,
                    mOuterTextBounds.bottom
                )
            } else {
                mOuterTextBounds = getInnerCircleRect(mCircleBounds)
            }
            if (mIsAutoTextSize) {
                setTextSizeAndTextBoundsWithAutoTextSize(
                    unitGapWidthHalf,
                    unitWidth,
                    unitGapHeightHalf,
                    unitHeight,
                    text
                )
            } else {
                setTextSizeAndTextBoundsWithFixedTextSize(text)
            }
        }
        if (DEBUG) {
            val rectPaint = Paint()
            rectPaint.color = Color.MAGENTA
            canvas.drawRect(mOuterTextBounds, rectPaint)
            rectPaint.color = Color.GREEN
            canvas.drawRect(mActualTextBounds, rectPaint)
        }
        canvas.drawText(
            text,
            mActualTextBounds.left - mTextPaint.textSize * 0.02f,
            mActualTextBounds.bottom,
            mTextPaint
        )
        if (mShowUnit) {
            if (mIsAutoColorEnabled) {
                mUnitTextPaint.color = calcTextColor(currentValue.toDouble())
            }
            if (update) { //calc unit text position
                if (mIsAutoTextSize) {
                    setUnitTextBoundsAndSizeWithAutoTextSize(
                        unitGapWidthHalf,
                        unitWidth,
                        unitGapHeightHalf,
                        unitHeight
                    )
                } else {
                    setUnitTextBoundsAndSizeWithFixedTextSize(
                        unitGapWidthHalf * 2f,
                        unitGapHeightHalf * 2f
                    )
                }
            }
            if (DEBUG) {
                val rectPaint = Paint()
                rectPaint.color = Color.RED
                canvas.drawRect(mUnitBounds, rectPaint)
            }
            canvas.drawText(
                mUnit,
                mUnitBounds.left - mUnitTextPaint.textSize * 0.02f,
                mUnitBounds.bottom,
                mUnitTextPaint
            )
        }
    }

    private fun drawBar(_canvas: Canvas, _degrees: Float) {
        val startAngle =
            if (mDirection === Direction.CW) mStartAngle.toFloat() else mStartAngle - _degrees
        if (!mShowBlock) {
            if (mBarStrokeCap != Cap.BUTT && _degrees > 0 && barColors.size > 1) {
                if (_degrees > 180) {
                    _canvas.drawArc(mCircleBounds, startAngle, _degrees / 2, false, mBarPaint)
                    _canvas.drawArc(mCircleBounds, startAngle, 1f, false, mShaderlessBarPaint!!)
                    _canvas.drawArc(
                        mCircleBounds,
                        startAngle + _degrees / 2,
                        _degrees / 2,
                        false,
                        mBarPaint
                    )
                } else {
                    _canvas.drawArc(mCircleBounds, startAngle, _degrees, false, mBarPaint)
                    _canvas.drawArc(mCircleBounds, startAngle, 1f, false, mShaderlessBarPaint!!)
                }
            } else {
                _canvas.drawArc(mCircleBounds, startAngle, _degrees, false, mBarPaint)
            }
        } else {
            drawBlocks(_canvas, mCircleBounds, startAngle, _degrees, false, mBarPaint)
        }
    }
    //endregion draw
//----------------------------------
    /**
     * Turn off spinning mode
     */
    fun stopSpinning() {
        setSpin(false)
        mAnimationHandler.sendEmptyMessage(AnimationMsg.STOP_SPINNING.ordinal)
    }

    /**
     * Puts the view in spin mode
     */
    fun spin() {
        setSpin(true)
        mAnimationHandler.sendEmptyMessage(AnimationMsg.START_SPINNING.ordinal)
    }

    private fun setSpin(spin: Boolean) {
        mSpin = spin
    }

    //----------------------------------
//region touch input
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mSeekModeEnabled == false) {
            return super.onTouchEvent(event)
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> {
                mTouchEventCount = 0
                val point = PointF(event.x, event.y)
                val angle = getRotationAngleForPointFromStart(point)
                setValueAnimated(maxValue / 360f * angle, 800)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                mTouchEventCount++
                return if (mTouchEventCount > 5) { //touch/move guard
                    val point = PointF(event.x, event.y)
                    val angle = getRotationAngleForPointFromStart(point)
                    setValue(maxValue / 360f * angle)
                    true
                } else {
                    false
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                mTouchEventCount = 0
                return false
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getRotationAngleForPointFromStart(point: PointF): Float {
        val angle = Math.round(
            calcRotationAngleInDegrees(
                mCenter,
                point
            )
        )
        val fromStart =
            if (mDirection === Direction.CW) (angle - mStartAngle).toFloat() else mStartAngle - angle.toFloat()
        return normalizeAngle(fromStart)
    }

    //endregion touch input
//----------------------------------
//-----------------------------------
//region listener for progress change
    interface OnProgressChangedListener {
        fun onProgressChanged(value: Float)
    } //endregion listener for progress change

    //--------------------------------------
    companion object {
        /**
         * The log tag.
         */
        private const val TAG = "CircleView"
        private const val DEBUG = false
        private fun calcTextSizeForRect(
            _text: String,
            _textPaint: Paint,
            _rectBounds: RectF
        ): Float {
            val matrix = Matrix()
            val textBoundsTmp = Rect()
            //replace ones because for some fonts the 1 takes less space which causes issues
            val text = _text.replace('1', '0')
            //get current mText bounds
            _textPaint.getTextBounds(text, 0, text.length, textBoundsTmp)
            val textBoundsTmpF = RectF(textBoundsTmp)
            matrix.setRectToRect(
                textBoundsTmpF,
                _rectBounds,
                Matrix.ScaleToFit.CENTER
            )
            val values = FloatArray(9)
            matrix.getValues(values)
            return _textPaint.textSize * values[Matrix.MSCALE_X]
        }

        /**
         * @param _angle The angle in degree to normalize
         * @return the angle between 0 (EAST) and 360
         */
        private fun normalizeAngle(_angle: Float): Float {
            return (_angle % 360 + 360) % 360
        }

        /**
         * Calculates the angle from centerPt to targetPt in degrees.
         * The return should range from [0,360), rotating CLOCKWISE,
         * 0 and 360 degrees represents EAST,
         * 90 degrees represents SOUTH, etc...
         *
         *
         * Assumes all points are in the same coordinate space.  If they are not,
         * you will need to call SwingUtilities.convertPointToScreen or equivalent
         * on all arguments before passing them  to this function.
         *
         * @param centerPt Point we are rotating around.
         * @param targetPt Point we want to calculate the angle to.
         * @return angle in degrees.  This is the angle from centerPt to targetPt.
         */
        fun calcRotationAngleInDegrees(
            centerPt: PointF?,
            targetPt: PointF
        ): Double { // calculate the angle theta from the deltaY and deltaX values
// (atan2 returns radians values from [-PI,PI])
// 0 currently points EAST.
// NOTE: By preserving Y and X param order to atan2,  we are expecting
// a CLOCKWISE angle direction.
            val theta = Math.atan2(
                targetPt.y - centerPt!!.y.toDouble(),
                targetPt.x - centerPt.x.toDouble()
            )
            // rotate the theta angle clockwise by 90 degrees
// (this makes 0 point NORTH)
// NOTE: adding to an angle rotates it clockwise.
// subtracting would rotate it counter-clockwise
//        theta += Math.PI/2.0;
// convert from radians to degrees
// this will give you an angle from [0->270],[-180,0]
            var angle = Math.toDegrees(theta)
            // convert to positive range [0-360)
// since we want to prevent negative angles, adjust them now.
// we can assume that atan2 will not return a negative value
// greater than one partial rotation
            if (angle < 0) {
                angle += 360.0
            }
            return angle
        }
    }
    //endregion members
//----------------------------------
    /**
     * The constructor for the CircleView
     *
     * @param context The context.
     * @param attrs   The attributes.
     */
    init {
        parseAttributes(
            context.obtainStyledAttributes(
                attrs,
                R.styleable.CircleProgressView
            )
        )
        if (!isInEditMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                setLayerType(LAYER_TYPE_HARDWARE, null)
            }
        }
        mMaskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mMaskPaint.isFilterBitmap = false
        mMaskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        setupPaints()
        if (mSpin) {
            spin()
        }
    }
}