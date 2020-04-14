package com.cafrecode.views.circurlarpercent

interface AnimationStateChangedListener {
    /**
     * Call if animation state changes.
     * This code runs in the animation loop, so keep your code short!
     *
     * @param _animationState The new animation state
     */
    fun onAnimationStateChanged(_animationState: AnimationState?)
}