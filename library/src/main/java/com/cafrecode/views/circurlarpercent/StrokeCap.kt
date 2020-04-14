package com.cafrecode.views.circurlarpercent

import android.graphics.Paint.Cap

enum class StrokeCap(val paintCap: Cap) {
    /**
     * The stroke ends with the path, and does not project beyond it.
     */
    BUTT(Cap.BUTT),
    /**
     * The stroke projects out as a semicircle, with the center at the
     * end of the path.
     */
    ROUND(Cap.ROUND),
    /**
     * The stroke projects out as a square, with the center at the end
     * of the path.
     */
    SQUARE(Cap.SQUARE);

}