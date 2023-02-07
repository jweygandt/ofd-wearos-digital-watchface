package com.ofd.watchface.vcomp

import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.util.Log
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
enum class ICON_BACKGROUND { NONE, GRAY80 }

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
    val color: Int
    val expiresms: Long
    val iconBackground: ICON_BACKGROUND
}

// This is used for debugging and such, don't really need more than one instance
val virtualComplicationStatus = VirtualComplicationStatusImpl()

/**
 * This wraps the standard complication with VirtualComplication so it can be rendered just like
 * a VComp.
 */
open class StandardComplication(
    private val slot: ComplicationSlot,
    private val watch: VirtualComplicationWatchRenderSupport,
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

    private val fulltext: String
        get() {
            if (instant != null) {
                val v = slot.complicationData.value
                val txt = when (v) {
                    is ShortTextComplicationData -> v.text.getTextAt(
                        watch.context.resources, instant
                    ).toString()
                    is LongTextComplicationData -> v.text.getTextAt(
                        watch.context.resources, instant
                    ).toString()
                    is RangedValueComplicationData -> (v.contentDescription?.getTextAt(
                        watch.context.resources, instant
                    ) ?: v.text?.getTextAt(watch.context.resources, instant)).toString()
                    else -> throw Exception("OOPS")
                }
                return txt
            }
            throw Exception("Null instant")
        }

    override val text: String
        get() {
            val exms = expiresms
            if((instant!!.epochSecond*1000L) > expiresms)
                return "??"
            val txt = fulltext
            val inx = txt.indexOf("?")
            return if (inx < 0) txt else txt.substring(0, inx)
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

    override val color: Int
        get() {
            val txt = fulltext
            val inx = txt.indexOf("Color=")
            if (inx < 0) return -1
            val qry = txt.substring(inx + "Color=".length)
            val i2 = qry.indexOf("&")
            val color = Integer.parseInt(if (i2 < 0) qry else qry.substring(0, i2))
            return color
        }

    override val expiresms: Long
        get() {
            val txt = fulltext
            val inx = txt.indexOf("ExpiresMS=")
            if (inx < 0) return Long.MAX_VALUE
            val qry = txt.substring(inx + "ExpiresMS=".length)
            val i2 = qry.indexOf("&")
            val v = (if (i2 < 0) qry else qry.substring(0, i2)).toLong()
            return v
        }

    override val iconBackground: ICON_BACKGROUND
        get() {
            val txt = fulltext
            val inx = txt.indexOf("IconBackground=")
            if (inx < 0) return ICON_BACKGROUND.NONE
            val qry = txt.substring(inx + "IconBackground=".length)
            val i2 = qry.indexOf("&")
            val v = (if (i2 < 0) qry else qry.substring(0, i2))
            try {
                return ICON_BACKGROUND.valueOf(v)
            }catch(e:Exception){
                Log.e(TAG, "Bad value for IconBackground="+v)
                return ICON_BACKGROUND.NONE
            }
        }

    companion object {
        val TAG = "VirtualComplication"
    }
}
