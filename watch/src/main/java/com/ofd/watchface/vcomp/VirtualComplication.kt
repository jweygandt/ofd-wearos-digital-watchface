package com.ofd.watchface.vcomp

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.drawable.Icon
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.complications.data.*
import com.ofd.complications.VirtualComplicationStatusImpl
import java.time.Instant

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
interface VirtualComplication {
    val type: ComplicationType
    val image: Icon?
    val text: String
    val rangeValue: Float
    val rangeMin: Float
    val rangeMax: Float
    fun customDrawable(
        canvas: Canvas, bleft: Float, btop: Float, bbottom: Float, sqsize: Float
    ): Boolean

    val tapCallback: Runnable?
}

// This is used for debugging and such, don't really need more than one instance
val virtualComplicationStatus = VirtualComplicationStatusImpl()

/**
 * This wraps the standard complication with VirtualComplication so it can be rendered just like
 * a VComp.
 */
open class StandardComplication(
    private val slot: ComplicationSlot,
    private val resources: Resources?,
    private val instant: Instant?
) : VirtualComplication {

    override val type get() = slot.complicationData.value.type

    // Types to deal with are: SHORT_TEXT, LONG_TEXT, RANGE, SMALL_IMAGE

    override fun customDrawable(
        canvas: Canvas, bleft: Float, btop: Float, bbottom: Float, sqsize: Float
    ): Boolean = false

    override val tapCallback: Runnable?
        get() = null

    override val image: Icon?
        get() {
            val v = slot.complicationData.value
            when (v) {
                is ShortTextComplicationData -> return v.monochromaticImage?.image
                is LongTextComplicationData -> return v.monochromaticImage?.image
                    ?: v.smallImage?.image
                is RangedValueComplicationData -> return v.monochromaticImage?.image
                is SmallImageComplicationData -> return v.smallImage.image
                else -> throw Exception("OOPS")
            }
        }

    override val text: String
        get() {
            if (resources == null) throw Exception("Null resources")
            if (instant != null) {
                val v = slot.complicationData.value
                when (v) {
                    is ShortTextComplicationData -> return v.text.getTextAt(resources, instant)
                        .toString()
                    is LongTextComplicationData -> return v.text.getTextAt(resources, instant)
                        .toString()
                    is RangedValueComplicationData -> return (v.contentDescription?.getTextAt(
                        resources, instant
                    ) ?: v.text?.getTextAt(resources, instant)).toString()
                    else -> throw Exception("OOPS")
                }
            }
            throw Exception("Null instant")
        }

    override val rangeValue: Float
        get() {
            val v = slot.complicationData.value
            when (v) {
                is RangedValueComplicationData -> return v.value
                else -> throw Exception("OOPS")
            }
        }

    override val rangeMin: Float
        get() {
            val v = slot.complicationData.value
            when (v) {
                is RangedValueComplicationData -> return v.min
                else -> throw Exception("OOPS")
            }
        }

    override val rangeMax: Float
        get() {
            val v = slot.complicationData.value
            when (v) {
                is RangedValueComplicationData -> return v.max
                else -> throw Exception("OOPS")
            }
        }
}
