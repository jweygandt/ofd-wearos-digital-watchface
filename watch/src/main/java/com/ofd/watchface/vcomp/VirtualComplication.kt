package com.ofd.watchface.vcomp

import android.graphics.Canvas
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationType

/**
 * This is the data I have determined that I need during a render operation, could do subclasses
 * for the different types, but simply a union, and unneeded attributes are not feteched if not
 * needed for the type.
 *
 * Additionally...
 *
 * To support a custom battery icon rendering, we have customDrawable() that will
 * provide the ability to render something in place of the Icon
 *
 * To support tapping on VComp, we have a tapCallback() which will support an inprocess click on
 * VComp rather than the out of process Intent
 */
enum class ICON_BACKGROUND { NONE, GRAY80 }

interface VirtualComplication {
    val type: ComplicationType
    val image: Icon?
    val text: String
    val rangeValue: Float
    val rangeMin: Float
    val rangeMax: Float

    /*
     * Sould the complication like a custom drawable, this will do the drawing and return true,
     * see VirtualBatteryComplicationImpl for a sample
     */
    fun customDrawable(
        canvas: Canvas, bleft: Float, btop: Float, bbottom: Float, sqsize: Float
    ): Boolean = false

    val onTap: Runnable?
    val color: Int
    val expiresms: Long
    val iconBackground: ICON_BACKGROUND
}

