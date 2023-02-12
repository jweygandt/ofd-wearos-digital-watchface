package com.ofd.complications

import android.graphics.Canvas
import android.graphics.Rect
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.RenderParameters
import com.ofd.watchface.vcomp.StandardComplication
import com.ofd.watchface.vcomp.VirtualComplication
import com.ofd.watchface.vcomp.VirtualComplicationWatchRenderSupport
import java.time.Instant
import java.time.ZonedDateTime

/*
 * In the spirit of breaking rules and hacking away...
 *
 * Complication come in different flavors: 1) The Wear OS definition (seperate process);
 * 2) The Wear OS definition, but in the same process as the Watch/Default process.
 * 3) Real time complications - those that display data from the watch process space
 * each and ever render. 4) Complex, custom complications that have interactions
 *
 * So as we create slots we need to wrap ComplicationSlot with this wrapper to extend
 * its functionality.
 */
class ComplicationWrapper(private val slot: ComplicationSlot) {

    fun virtualComplication(
        watch: VirtualComplicationWatchRenderSupport, instant: Instant?
    ): VirtualComplication {
        val cls = slot.complicationData.value.dataSource?.className
        if (cls != null) {
            when (cls) {
                VirtualComplicationStatus::class.qualifiedName -> return VirtualComplicationStatusImpl(watch)
                VirtualComplicationPlayPause::class.qualifiedName -> return VirtualComplicationPlayPauseImpl(
                    watch
                )
            }
            if (cls.endsWith("BatteryProviderService")) return VirtualBatteryComplicationImpl(
                slot, watch, instant
            )
        }
        return StandardComplication(slot, watch, instant)
    }

    val enabled get() = slot.enabled

    val id get() = slot.id

    fun renderHighlightLayer(
        canvas: Canvas, zonedDateTime: ZonedDateTime, renderParameters: RenderParameters
    ) {
        slot.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
    }

    fun computeBounds(rect: Rect): Rect {
        return slot.computeBounds(rect)
    }

    fun defaultRender(
        canvas: Canvas, zonedDateTime: ZonedDateTime, renderParameters: RenderParameters
    ) {
        slot.render(canvas, zonedDateTime, renderParameters)
    }
}

