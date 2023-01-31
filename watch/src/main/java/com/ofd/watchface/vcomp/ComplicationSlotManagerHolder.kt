package com.ofd.watchface.vcomp

import androidx.wear.watchface.ComplicationSlotsManager
import com.ofd.complications.ComplicationWrapper

/**
 * Don't is suck there are so many final classes? OK, maybe I'm thinking Java and not Kotlin, as
 * I look at ComplicationWrapper maybe somthing like extension functions could be done? Anyway
 * that is for another day...
 *
 * Anyway we need these parallel classes to handled Virtual Complications
 */
class ComplicationSlotManagerHolder(
    val slotManager: ComplicationSlotsManager,
    val slotWrappers: List<ComplicationWrapper>
) {
    lateinit var watch: VirtualComplicationWatchRenderSupport
}
