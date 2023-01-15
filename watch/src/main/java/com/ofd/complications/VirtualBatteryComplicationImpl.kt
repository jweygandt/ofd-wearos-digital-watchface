package com.ofd.complications

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Icon
import androidx.wear.watchface.ComplicationSlot
import com.ofd.watchface.vcomp.StandardComplication
import java.time.Instant

/**
 * Why did wearos only do "monochromatic" and likely "static" images? Let's be dynamic. Already did
 * for Heartbeat complication (another of my GitHub projects), so this simply overrides the
 * "icon" part of the existing battery complication.
 *
 * BTW use this in positions 1-4, the SHORT_TEXT positions
 */
class VirtualBatteryComplicationImpl(
    slot: ComplicationSlot,
    resources: Resources,
    instant: Instant?
) :
    StandardComplication(slot, resources, instant) {
    override val image: Icon? get() = null

    override fun customDrawable(
        canvas: Canvas, bleft: Float, btop: Float, bbottom: Float, sqsize:
        Float
    )
        : Boolean {
        val batteryPaint = Paint().apply {
            isAntiAlias = true
            color = Color.GRAY
            style = Paint.Style.FILL_AND_STROKE
            strokeCap = Paint.Cap.BUTT
            strokeWidth = sqsize / 4f
        }

        val pct = Integer.parseInt(text).toFloat()
        val cut = (1f - pct / 100f) * .8f * sqsize + .1f * sqsize
        val fillcolor =
            if (pct > 25f) Color.GREEN else if (pct > 10f) Color.YELLOW else Color.RED

        if (cut < .3f * sqsize) {
            canvas.drawLine(
                bleft + sqsize / 2f,
                btop + .1f * sqsize,
                bleft + sqsize / 2f,
                btop + cut,
                batteryPaint
            )
            batteryPaint.color = fillcolor
            canvas.drawLine(
                bleft + sqsize / 2f,
                btop + cut,
                bleft + sqsize / 2f,
                btop + .4f * sqsize,
                batteryPaint
            )
            batteryPaint.strokeWidth = sqsize / 2.5f
            canvas.drawLine(
                bleft + sqsize / 2f,
                btop + .3f * sqsize,
                bleft + sqsize / 2f,
                bbottom - .1f * sqsize,
                batteryPaint
            )
        } else {
            canvas.drawLine(
                bleft + sqsize / 2f,
                btop + .1f * sqsize,
                bleft + sqsize / 2f,
                btop + .4f * sqsize,
                batteryPaint
            )
            batteryPaint.strokeWidth = sqsize / 2.5f
            canvas.drawLine(
                bleft + sqsize / 2f,
                btop + .3f * sqsize,
                bleft + sqsize / 2f,
                btop + cut,
                batteryPaint
            )
            batteryPaint.color = fillcolor
            canvas.drawLine(
                bleft + sqsize / 2f,
                btop + cut,
                bleft + sqsize / 2f,
                bbottom - .1f * sqsize,
                batteryPaint
            )
        }
        return true
    }
}
