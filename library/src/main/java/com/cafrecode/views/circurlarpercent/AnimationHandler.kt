package com.cafrecode.views.circurlarpercent

import android.animation.TimeInterpolator
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import java.lang.ref.WeakReference

class AnimationHandler internal constructor(circleView: CircleProgressView) :
    Handler(circleView.context.mainLooper) {
    private val mCircleViewWeakReference: WeakReference<CircleProgressView>
    // Spin bar length in degree at start of animation
    private var mSpinningBarLengthStart = 0f
    private var mAnimationStartTime: Long = 0
    private var mLengthChangeAnimationStartTime: Long = 0
    private var mLengthChangeInterpolator: TimeInterpolator = DecelerateInterpolator()
    // The interpolator for value animations
    private var mInterpolator: TimeInterpolator = AccelerateDecelerateInterpolator()
    private var mLengthChangeAnimationDuration = 0.0
    private var mFrameStartTime: Long = 0
    /**
     * Sets interpolator for value animations.
     *
     * @param mInterpolator the m interpolator
     */
    fun setValueInterpolator(mInterpolator: TimeInterpolator) {
        this.mInterpolator = mInterpolator
    }

    /**
     * Sets the interpolator for length changes of the bar.
     *
     * @param mLengthChangeInterpolator the m length change interpolator
     */
    fun setLengthChangeInterpolator(mLengthChangeInterpolator: TimeInterpolator) {
        this.mLengthChangeInterpolator = mLengthChangeInterpolator
    }

    override fun handleMessage(msg: Message) {
        val circleView = mCircleViewWeakReference.get() ?: return
        val msgType = AnimationMsg.values()[msg.what]
        if (msgType == AnimationMsg.TICK) {
            removeMessages(AnimationMsg.TICK.ordinal) // necessary to remove concurrent ticks.
        }
        //if (msgType != AnimationMsg.TICK)
//    Log.d("JaGr", TAG + "LOG00099: State:" + circleView.mAnimationState + "     Received: " + msgType);
        mFrameStartTime = SystemClock.uptimeMillis()
        when (circleView.mAnimationState) {
            AnimationState.IDLE -> when (msgType) {
                AnimationMsg.START_SPINNING -> enterSpinning(circleView)
                AnimationMsg.STOP_SPINNING -> {
                }
                AnimationMsg.SET_VALUE -> setValue(msg, circleView)
                AnimationMsg.SET_VALUE_ANIMATED -> enterSetValueAnimated(msg, circleView)
                AnimationMsg.TICK -> removeMessages(AnimationMsg.TICK.ordinal) // remove old ticks
            }
            AnimationState.SPINNING -> when (msgType) {
                AnimationMsg.START_SPINNING -> {
                }
                AnimationMsg.STOP_SPINNING -> enterEndSpinning(circleView)
                AnimationMsg.SET_VALUE -> setValue(msg, circleView)
                AnimationMsg.SET_VALUE_ANIMATED -> enterEndSpinningStartAnimating(circleView, msg)
                AnimationMsg.TICK -> {
                    // set length
                    val length_delta =
                        circleView.mSpinningBarLengthCurrent - circleView.mSpinningBarLengthOrig
                    var t =
                        ((System.currentTimeMillis() - mLengthChangeAnimationStartTime)
                                / mLengthChangeAnimationDuration).toFloat()
                    t = if (t > 1.0f) 1.0f else t
                    val interpolatedRatio =
                        mLengthChangeInterpolator.getInterpolation(t)
                    if (Math.abs(length_delta) < 1) { //spinner length is within bounds
                        circleView.mSpinningBarLengthCurrent = circleView.mSpinningBarLengthOrig
                    } else if (circleView.mSpinningBarLengthCurrent < circleView.mSpinningBarLengthOrig) { //spinner to short, --> grow
                        circleView.mSpinningBarLengthCurrent =
                            mSpinningBarLengthStart + (circleView.mSpinningBarLengthOrig - mSpinningBarLengthStart) * interpolatedRatio
                    } else { //spinner to long, --> shrink
                        circleView.mSpinningBarLengthCurrent =
                            mSpinningBarLengthStart - (mSpinningBarLengthStart - circleView.mSpinningBarLengthOrig) * interpolatedRatio
                    }
                    circleView.mCurrentSpinnerDegreeValue += circleView.mSpinSpeed // spin speed value (in degree)
                    if (circleView.mCurrentSpinnerDegreeValue > 360) {
                        circleView.mCurrentSpinnerDegreeValue = 0f
                    }
                    sendEmptyMessageDelayed(
                        AnimationMsg.TICK.ordinal,
                        circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime)
                    )
                    circleView.invalidate()
                }
            }
            AnimationState.END_SPINNING -> when (msgType) {
                AnimationMsg.START_SPINNING -> {
                    circleView.mAnimationState =
                        AnimationState.SPINNING
                    if (circleView.mAnimationStateChangedListener != null) {
                        circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState)
                    }
                    sendEmptyMessageDelayed(
                        AnimationMsg.TICK.ordinal,
                        circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime)
                    )
                }
                AnimationMsg.STOP_SPINNING -> {
                }
                AnimationMsg.SET_VALUE -> setValue(msg, circleView)
                AnimationMsg.SET_VALUE_ANIMATED -> enterEndSpinningStartAnimating(circleView, msg)
                AnimationMsg.TICK -> {
                    var t =
                        ((System.currentTimeMillis() - mLengthChangeAnimationStartTime)
                                / mLengthChangeAnimationDuration).toFloat()
                    t = if (t > 1.0f) 1.0f else t
                    val interpolatedRatio =
                        mLengthChangeInterpolator.getInterpolation(t)
                    circleView.mSpinningBarLengthCurrent =
                        mSpinningBarLengthStart * (1f - interpolatedRatio)
                    circleView.mCurrentSpinnerDegreeValue += circleView.mSpinSpeed // spin speed value (not in percent)
                    if (circleView.mSpinningBarLengthCurrent < 0.01f) { //end here, spinning finished
                        circleView.mAnimationState =
                            AnimationState.IDLE
                        if (circleView.mAnimationStateChangedListener != null) {
                            circleView.mAnimationStateChangedListener.onAnimationStateChanged(
                                circleView.mAnimationState
                            )
                        }
                    }
                    sendEmptyMessageDelayed(
                        AnimationMsg.TICK.ordinal,
                        circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime)
                    )
                    circleView.invalidate()
                }
            }
            AnimationState.END_SPINNING_START_ANIMATING -> when (msgType) {
                AnimationMsg.START_SPINNING -> {
                    circleView.mDrawBarWhileSpinning = false
                    enterSpinning(circleView)
                }
                AnimationMsg.STOP_SPINNING -> {
                }
                AnimationMsg.SET_VALUE -> {
                    circleView.mDrawBarWhileSpinning = false
                    setValue(msg, circleView)
                }
                AnimationMsg.SET_VALUE_ANIMATED -> {
                    circleView.mValueFrom = 0f // start from zero after spinning
                    circleView.mValueTo = (msg.obj as FloatArray)[1]
                    sendEmptyMessageDelayed(
                        AnimationMsg.TICK.ordinal,
                        circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime)
                    )
                }
                AnimationMsg.TICK -> {
                    //shrink spinner till it has its original length
                    if (circleView.mSpinningBarLengthCurrent > circleView.mSpinningBarLengthOrig && !circleView.mDrawBarWhileSpinning) { //spinner to long, --> shrink
                        var t =
                            ((System.currentTimeMillis() - mLengthChangeAnimationStartTime)
                                    / mLengthChangeAnimationDuration).toFloat()
                        t = if (t > 1.0f) 1.0f else t
                        val interpolatedRatio =
                            mLengthChangeInterpolator.getInterpolation(t)
                        circleView.mSpinningBarLengthCurrent =
                            mSpinningBarLengthStart * (1f - interpolatedRatio)
                    }
                    // move spinner for spin speed value (not in percent)
                    circleView.mCurrentSpinnerDegreeValue += circleView.mSpinSpeed
                    //if the start of the spinner reaches zero, start animating the value
                    if (circleView.mCurrentSpinnerDegreeValue > 360 && !circleView.mDrawBarWhileSpinning) {
                        mAnimationStartTime = System.currentTimeMillis()
                        circleView.mDrawBarWhileSpinning = true
                        initReduceAnimation(circleView)
                        if (circleView.mAnimationStateChangedListener != null) {
                            circleView.mAnimationStateChangedListener.onAnimationStateChanged(
                                AnimationState.START_ANIMATING_AFTER_SPINNING
                            )
                        }
                    }
                    //value is already animating, calc animation value and reduce spinner
                    if (circleView.mDrawBarWhileSpinning) {
                        circleView.mCurrentSpinnerDegreeValue = 360f
                        circleView.mSpinningBarLengthCurrent -= circleView.mSpinSpeed
                        calcNextAnimationValue(circleView)
                        var t =
                            ((System.currentTimeMillis() - mLengthChangeAnimationStartTime)
                                    / mLengthChangeAnimationDuration).toFloat()
                        t = if (t > 1.0f) 1.0f else t
                        val interpolatedRatio =
                            mLengthChangeInterpolator.getInterpolation(t)
                        circleView.mSpinningBarLengthCurrent =
                            mSpinningBarLengthStart * (1f - interpolatedRatio)
                    }
                    //spinner is no longer visible switch state to animating
                    if (circleView.mSpinningBarLengthCurrent < 0.1) { //spinning finished, start animating the current value
                        circleView.mAnimationState =
                            AnimationState.ANIMATING
                        if (circleView.mAnimationStateChangedListener != null) {
                            circleView.mAnimationStateChangedListener.onAnimationStateChanged(
                                circleView.mAnimationState
                            )
                        }
                        circleView.invalidate()
                        circleView.mDrawBarWhileSpinning = false
                        circleView.mSpinningBarLengthCurrent = circleView.mSpinningBarLengthOrig
                    } else {
                        circleView.invalidate()
                    }
                    sendEmptyMessageDelayed(
                        AnimationMsg.TICK.ordinal,
                        circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime)
                    )
                }
            }
            AnimationState.ANIMATING -> when (msgType) {
                AnimationMsg.START_SPINNING -> enterSpinning(circleView)
                AnimationMsg.STOP_SPINNING -> {
                }
                AnimationMsg.SET_VALUE -> setValue(msg, circleView)
                AnimationMsg.SET_VALUE_ANIMATED -> {
                    mAnimationStartTime = System.currentTimeMillis()
                    //restart animation from current value
                    circleView.mValueFrom = circleView.mCurrentValue
                    circleView.mValueTo = (msg.obj as FloatArray)[1]
                }
                AnimationMsg.TICK -> {
                    if (calcNextAnimationValue(circleView)) { //animation finished
                        circleView.mAnimationState =
                            AnimationState.IDLE
                        if (circleView.mAnimationStateChangedListener != null) {
                            circleView.mAnimationStateChangedListener.onAnimationStateChanged(
                                circleView.mAnimationState
                            )
                        }
                        circleView.mCurrentValue = circleView.mValueTo
                    }
                    sendEmptyMessageDelayed(
                        AnimationMsg.TICK.ordinal,
                        circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime)
                    )
                    circleView.invalidate()
                }
            }
        }
    }

    private fun enterSetValueAnimated(
        msg: Message,
        circleView: CircleProgressView
    ) {
        circleView.mValueFrom = (msg.obj as FloatArray)[0]
        circleView.mValueTo = (msg.obj as FloatArray)[1]
        mAnimationStartTime = System.currentTimeMillis()
        circleView.mAnimationState = AnimationState.ANIMATING
        if (circleView.mAnimationStateChangedListener != null) {
            circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState)
        }
        sendEmptyMessageDelayed(
            AnimationMsg.TICK.ordinal,
            circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime)
        )
    }

    private fun enterEndSpinningStartAnimating(
        circleView: CircleProgressView,
        msg: Message
    ) {
        circleView.mAnimationState =
            AnimationState.END_SPINNING_START_ANIMATING
        if (circleView.mAnimationStateChangedListener != null) {
            circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState)
        }
        circleView.mValueFrom = 0f // start from zero after spinning
        circleView.mValueTo = (msg.obj as FloatArray)[1]
        mLengthChangeAnimationStartTime = System.currentTimeMillis()
        mSpinningBarLengthStart = circleView.mSpinningBarLengthCurrent
        sendEmptyMessageDelayed(
            AnimationMsg.TICK.ordinal,
            circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime)
        )
    }

    private fun enterEndSpinning(circleView: CircleProgressView) {
        circleView.mAnimationState =
            AnimationState.END_SPINNING
        initReduceAnimation(circleView)
        if (circleView.mAnimationStateChangedListener != null) {
            circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState)
        }
        sendEmptyMessageDelayed(
            AnimationMsg.TICK.ordinal,
            circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime)
        )
    }

    private fun initReduceAnimation(circleView: CircleProgressView) {
        val degreesTillFinish = circleView.mSpinningBarLengthCurrent
        val stepsTillFinish = degreesTillFinish / circleView.mSpinSpeed
        mLengthChangeAnimationDuration =
            stepsTillFinish * circleView.mFrameDelayMillis * 2f.toDouble()
        mLengthChangeAnimationStartTime = System.currentTimeMillis()
        mSpinningBarLengthStart = circleView.mSpinningBarLengthCurrent
    }

    private fun enterSpinning(circleView: CircleProgressView) {
        circleView.mAnimationState = AnimationState.SPINNING
        if (circleView.mAnimationStateChangedListener != null) {
            circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState)
        }
        circleView.mSpinningBarLengthCurrent =
            360f / circleView.mMaxValue * circleView.mCurrentValue
        circleView.mCurrentSpinnerDegreeValue =
            360f / circleView.mMaxValue * circleView.mCurrentValue
        mLengthChangeAnimationStartTime = System.currentTimeMillis()
        mSpinningBarLengthStart = circleView.mSpinningBarLengthCurrent
        //calc animation time
        val stepsTillFinish =
            circleView.mSpinningBarLengthOrig / circleView.mSpinSpeed
        mLengthChangeAnimationDuration =
            (stepsTillFinish * circleView.mFrameDelayMillis * 2f).toDouble()
        sendEmptyMessageDelayed(
            AnimationMsg.TICK.ordinal,
            circleView.mFrameDelayMillis - (SystemClock.uptimeMillis() - mFrameStartTime)
        )
    }

    /**
     * *
     *
     * @param circleView the circle view
     * @return false if animation still running, true if animation is finished.
     */
    private fun calcNextAnimationValue(circleView: CircleProgressView): Boolean {
        var t = ((System.currentTimeMillis() - mAnimationStartTime)
                / circleView.mAnimationDuration).toFloat()
        t = if (t > 1.0f) 1.0f else t
        val interpolatedRatio = mInterpolator.getInterpolation(t)
        circleView.mCurrentValue =
            circleView.mValueFrom + (circleView.mValueTo - circleView.mValueFrom) * interpolatedRatio
        return t >= 1
    }

    private fun setValue(msg: Message, circleView: CircleProgressView) {
        circleView.mValueFrom = circleView.mValueTo
        circleView.mValueTo = (msg.obj as FloatArray)[0]
        circleView.mCurrentValue = circleView.mValueTo
        circleView.mAnimationState = AnimationState.IDLE
        if (circleView.mAnimationStateChangedListener != null) {
            circleView.mAnimationStateChangedListener.onAnimationStateChanged(circleView.mAnimationState)
        }
        circleView.invalidate()
    }

    init {
        mCircleViewWeakReference = WeakReference(circleView)
    }
}