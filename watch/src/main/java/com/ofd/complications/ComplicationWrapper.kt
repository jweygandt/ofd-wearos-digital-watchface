package com.ofd.complications

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Icon
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.ofd.digital.alpha.DigitalWatchCanvasRenderer
import java.time.Instant
import java.time.ZonedDateTime

interface VirtualComplication {
    val type: ComplicationType
    val image: Icon?
    val text: String
    val rangeValue: Float
    val rangeMin: Float
    val rangeMax: Float
    fun customDrawable(
        canvas: Canvas,
        bleft: Float,
        btop: Float,
        bbottom: Float,
        sqsize: Float
    ): Boolean

    val tapCallback: Runnable?
}

val virtualComplicationStatus = VirtualComplicationStatusImpl()

open class StandardComplication(
    private val slot: ComplicationSlot, private val resources: Resources?,
    private val instant: Instant?
) : VirtualComplication {

    override val type get() = slot.complicationData.value.type

    // Types to deal with are: SHORT_TEXT, LONG_TEXT, RANGE, SMALL_IMAGE

    override fun customDrawable(
        canvas: Canvas,
        bleft: Float,
        btop: Float,
        bbottom: Float,
        sqsize: Float
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
            if (resources == null)
                throw Exception("Null resources")
            if (instant != null) {
                val v = slot.complicationData.value
                when (v) {
                    is ShortTextComplicationData -> return v.text.getTextAt(resources, instant)
                        .toString()
                    is LongTextComplicationData -> return v.text.getTextAt(resources, instant)
                        .toString()
                    is RangedValueComplicationData -> return (v.contentDescription?.getTextAt(
                        resources,
                        instant
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

/*
 * In the spirit of breaking rules and hacking away...
 *
 * Complication come in different flavors: 1) The Wear OS definition (seperate process);
 * 2) The Wear OS definition, but in the same process as the Watch/Default process.
 * 3) Real time complications - those that display data from the watch process space
 * each and ever render. 4) Complex, custom complications that have interactions
 */
class ComplicationWrapper(private val slot: ComplicationSlot) {

    fun virtualComplication(
        watch : DigitalWatchCanvasRenderer?,
        context: Context?,
        instant: Instant?,
        currentUserStyleRepository: CurrentUserStyleRepository?
    ): VirtualComplication {
        val cls = slot.complicationData.value.dataSource?.className
        if (cls != null) {
            when (cls) {
                VirtualComplicationStatus::class.qualifiedName -> return virtualComplicationStatus
                VirtualComplicationPlayPause::class.qualifiedName -> return VirtualComplicationPlayPauseImpl(
                    watch, context,
                    currentUserStyleRepository
                )
            }
            if (context != null) {
                if (cls.endsWith("BatteryProviderService"))
                    return VirtualBatteryComplicationImpl(
                        slot,
                        context.resources,
                        instant
                    )
            }
        }
        return StandardComplication(slot, context?.resources, instant)
    }


    val enabled get() = slot.enabled

    val id get() = slot.id

    fun renderHighlightLayer(
        canvas: Canvas,
        zonedDateTime: ZonedDateTime,
        renderParameters: RenderParameters
    ) {
        slot.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
    }

    fun computeBounds(rect: Rect): Rect {
        return slot.computeBounds(rect)
    }

    fun defaultRender(
        canvas: Canvas,
        zonedDateTime: ZonedDateTime,
        renderParameters: RenderParameters
    ) {
        slot.render(canvas, zonedDateTime, renderParameters)
    }

}

// Don't is suck there are so many final classes?
class ComplicationSlotManagerHolder(
    val slotManager: ComplicationSlotsManager,
    val slotWrappers: List<ComplicationWrapper>,
    var watch: DigitalWatchCanvasRenderer? = null
)
