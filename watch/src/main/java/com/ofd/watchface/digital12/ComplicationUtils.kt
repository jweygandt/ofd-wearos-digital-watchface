/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(ComplicationExperimental::class)

package com.ofd.watchface.digital12

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.ComplicationTapFilter
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.ofd.complications.*
import com.ofd.watch.R
import com.ofd.watchface.vcomp.ComplicationSlotManagerHolder
import kotlinx.coroutines.launch

private const val DEFAULT_COMPLICATION_STYLE_DRAWABLE_ID = R.drawable.complication_red_style


// bottom of first complication
private val o1 = .5f + .5f * Math.sin(Math.acos((D12.c1WidthP + .5f * D12.C12GapP) / .5))
    .toFloat() - D12.c1WatchGapExtraP

private val c1lp = .5f - D12.c1WidthP - .5f * D12.C12GapP
private val c1tp = o1 - D12.C1HeightP
private val c1rp = .5f - .5f * D12.C12GapP
private val c1bp = o1

private val c2lp = .5f + .5f * D12.C12GapP
private val c2tp = c1tp
private val c2rp = .5f + .5f * D12.C12GapP + D12.c1WidthP
private val c2bp = c1bp

private val c3lp = c1lp
private val c3tp = c1tp - D12.c1C3GapP - D12.C1HeightP
private val c3rp = c1rp
private val c3bp = c1tp - D12.c1C3GapP

private val c4lp = c2lp
private val c4tp = c3tp
private val c4rp = c2rp
private val c4bp = c3bp

private val c5left = .5f - .5f * D12.c5WidthP
private val c5top = D12.dateBottomP + D12.dateTimeGapP + D12.timeHeightP + D12.timeC5GapP
private val c5right = .5f + .5f * D12.c5WidthP
private val c5bottom = c3tp - D12.c3C5GapP

private val c6cx = c3lp - D12.c1C6GapP - D12.c6RadiusP
private val c6cy = c3bp + D12.c1C3GapP / 2f

private val o67 =
    ((D12.c6RadiusP + .5f * D12.c7C8GapP) / Math.tan(Math.asin((D12.c6RadiusP + .5 * D12.c7C8GapP) / (.5f - D12.c6RadiusP - D12.bottomRoundClockGapP)))).toFloat()

private val c7cx = .5f - D12.c6RadiusP - D12.c7C8GapP
private val c7cy = .5f + o67

private val c8cx = .5f + D12.c6RadiusP + D12.c7C8GapP
private val c8cy = c7cy

private val c9cx = c4rp + D12.c1C6GapP + D12.c6RadiusP
private val c9cy = c6cy

// Unique IDs for each complication. The settings activity that supports allowing users
// to select their complication data provider requires numbers to be >= 0.
// 1-4 are small text
// 5 long text
// 6-9 are small image, tap for action
// 10-11 are range arcs
// 12 is full screen, moon phase
// 14 is the special overlay, which is hardwired into the system
internal const val COMPLICATION_1 = 101
internal const val COMPLICATION_2 = 102
internal const val COMPLICATION_3 = 103
internal const val COMPLICATION_4 = 104
internal const val COMPLICATION_5 = 105
internal const val COMPLICATION_6 = 106
internal const val COMPLICATION_7 = 107
internal const val COMPLICATION_8 = 108
internal const val COMPLICATION_9 = 109
internal const val COMPLICATION_10 = 110
internal const val COMPLICATION_11 = 111
internal const val COMPLICATION_12 = 112
internal const val COMPLICATION_14 = 114

private const val TAG = "ComplicationUtils"

val BOX_COMPS = listOf(
    ComplicationType.SHORT_TEXT
)

val BUTTON_COMPS = listOf(
    ComplicationType.SMALL_IMAGE,
)

val ARC_COMPS = listOf(
    ComplicationType.RANGED_VALUE,
)

val FULLIMAGE_COMP = listOf(
    ComplicationType.SMALL_IMAGE,
)

val OVERLAY_COMP = listOf(
    ComplicationType.SMALL_IMAGE,
)


/**
 * Represents the unique id associated with a complication and the complication types it supports.
 *
 * TODO - From a copy/paste,  likely need to inline it all
 */
sealed class ComplicationConfig(val id: Int, val supportedTypes: List<ComplicationType>) {

    object C1 : ComplicationConfig(
        COMPLICATION_1, BOX_COMPS
    )

    object C2 : ComplicationConfig(
        COMPLICATION_2, BOX_COMPS
    )

    object C3 : ComplicationConfig(
        COMPLICATION_3, BOX_COMPS
    )

    object C4 : ComplicationConfig(
        COMPLICATION_4, BOX_COMPS
    )

    object C5 : ComplicationConfig(
        COMPLICATION_5, listOf(
            ComplicationType.LONG_TEXT,
        )
    )

    object C6 : ComplicationConfig(
        COMPLICATION_6, BUTTON_COMPS
    )

    object C7 : ComplicationConfig(
        COMPLICATION_7, BUTTON_COMPS
    )

    object C8 : ComplicationConfig(
        COMPLICATION_8, BUTTON_COMPS
    )

    object C9 : ComplicationConfig(
        COMPLICATION_9, BUTTON_COMPS
    )

    object C10 : ComplicationConfig(
        COMPLICATION_10, ARC_COMPS
    )

    object C11 : ComplicationConfig(
        COMPLICATION_11, ARC_COMPS
    )

    object C12 : ComplicationConfig(
        COMPLICATION_12, FULLIMAGE_COMP
    )

    object C14 : ComplicationConfig(
        COMPLICATION_14, OVERLAY_COMP
    )

}

// Utility function that initializes default complication slots (left and right).
fun createComplicationSlotManager(
    context: Context,
    currentUserStyleRepository: CurrentUserStyleRepository,
    drawableId: Int = DEFAULT_COMPLICATION_STYLE_DRAWABLE_ID
): ComplicationSlotManagerHolder {
    val defaultCanvasComplicationFactory = CanvasComplicationFactory { watchState, listener ->
        CanvasComplicationDrawable(
            ComplicationDrawable.getDrawable(context, drawableId)!!, watchState, listener
        )
    }

    val DEFAULT_SMALL_IMAGE = DefaultComplicationDataSourcePolicy(
        SystemDataSources.NO_DATA_SOURCE, ComplicationType.SMALL_IMAGE
    )
    val DEFAULT_SHORT_TEXT = DefaultComplicationDataSourcePolicy(
        SystemDataSources.NO_DATA_SOURCE, ComplicationType.SHORT_TEXT
    )

    // I need to use an arc as BACKGROUND has a bug during style changes
    val c12 = ComplicationSlot.createEdgeComplicationSlotBuilder(id = ComplicationConfig.C12.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C12.supportedTypes,
        defaultDataSourcePolicy = DEFAULT_SMALL_IMAGE,
        bounds = ComplicationSlotBounds(
            RectF(
                0f, 0f, 1f, 1f
            )
        ),
        object : ComplicationTapFilter {
            override fun hitTest(
                complicationSlot: ComplicationSlot, screenBounds: Rect, x: Int, y: Int
            ): Boolean {
                return false
            }
        }).build()

//    val c12y = ComplicationSlot.createBackgroundComplicationSlotBuilder(
//        id = ComplicationConfig.C12.id,
//        canvasComplicationFactory = defaultCanvasComplicationFactory,
//        supportedTypes = ComplicationConfig.C12.supportedTypes,
//        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
//            SystemDataSources.NO_DATA_SOURCE,
//            ComplicationType.SMALL_IMAGE
//        ),
//    ).build()

    val c14 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C14.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C14.supportedTypes,
        defaultDataSourcePolicy = DEFAULT_SMALL_IMAGE,
        bounds = ComplicationSlotBounds(
            RectF(0f, 0f, 1f, 1f)
        )
    ).setEnabled(false).build()

    val c1 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C1.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C1.supportedTypes,
        defaultDataSourcePolicy = DEFAULT_SHORT_TEXT,
        bounds = ComplicationSlotBounds(
            RectF(
                c1lp, c1tp, c1rp, c1bp
            )
        )
    ).build()

    val c2 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C2.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C2.supportedTypes,
        defaultDataSourcePolicy = DEFAULT_SHORT_TEXT,
        bounds = ComplicationSlotBounds(
            RectF(
                c2lp, c2tp, c2rp, c2bp
            )
        )
    ).build()


    val c3 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C3.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C3.supportedTypes,
        defaultDataSourcePolicy = DEFAULT_SHORT_TEXT,
        bounds = ComplicationSlotBounds(
            RectF(
                c3lp, c3tp, c3rp, c3bp
            )
        )
    ).build()


    val c4 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C4.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C4.supportedTypes,
        defaultDataSourcePolicy = DEFAULT_SHORT_TEXT,
        bounds = ComplicationSlotBounds(
            RectF(
                c4lp, c4tp, c4rp, c4bp
            )
        )
    ).build()

    val c5 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C5.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C5.supportedTypes,
        defaultDataSourcePolicy = DEFAULT_SHORT_TEXT,
        bounds = ComplicationSlotBounds(
            RectF(
                c5left, c5top, c5right, c5bottom
            )
        )
    ).build()

    val c6 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C6.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C6.supportedTypes,
        defaultDataSourcePolicy = DEFAULT_SMALL_IMAGE,
        bounds = ComplicationSlotBounds(
            RectF(
                c6cx - D12.c6RadiusP,
                c6cy - D12.c6RadiusP,
                c6cx + D12.c6RadiusP,
                c6cy + D12.c6RadiusP
            )
        )
    ).build()

    val c7 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C7.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C7.supportedTypes,
        defaultDataSourcePolicy = DEFAULT_SMALL_IMAGE,
        bounds = ComplicationSlotBounds(
            RectF(
                c7cx - D12.c6RadiusP,
                c7cy - D12.c6RadiusP,
                c7cx + D12.c6RadiusP,
                c7cy + D12.c6RadiusP
            )
        )
    ).build()

    val c8 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C8.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C8.supportedTypes,
        defaultDataSourcePolicy = DEFAULT_SMALL_IMAGE,
        bounds = ComplicationSlotBounds(
            RectF(
                c8cx - D12.c6RadiusP,
                c8cy - D12.c6RadiusP,
                c8cx + D12.c6RadiusP,
                c8cy + D12.c6RadiusP
            )
        )
    ).build()

    val c9 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C9.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C9.supportedTypes,
        defaultDataSourcePolicy = DEFAULT_SMALL_IMAGE,
        bounds = ComplicationSlotBounds(
            RectF(
                c9cx - D12.c6RadiusP,
                c9cy - D12.c6RadiusP,
                c9cx + D12.c6RadiusP,
                c9cy + D12.c6RadiusP
            )
        )
    ).build()


    val c10 = ComplicationSlot.createEdgeComplicationSlotBuilder(
        id = ComplicationConfig.C10.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C10.supportedTypes,
        defaultDataSourcePolicy = DEFAULT_SMALL_IMAGE,
        bounds = ComplicationSlotBounds(
            RectF(
                0f, 0f, 1f, 1f
            )
        ),
        ArcTapFilter(
            RectF(
                1f - .5f * cosf(D12.c10startAngle + D12.c10sweepAngle) - .1f,
                .5f * sinf(D12.c10startAngle + D12.c10sweepAngle),
                1f,
                .5f + .5f * sinf(D12.c10startAngle)
            )
        )
    ).build()

    val c11 = ComplicationSlot.createEdgeComplicationSlotBuilder(
        id = ComplicationConfig.C11.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C11.supportedTypes,
        defaultDataSourcePolicy = DEFAULT_SMALL_IMAGE,
        bounds = ComplicationSlotBounds(
            RectF(
                0f, 0f, 1f, 1f
            )
        ),
        ArcTapFilter(
            RectF(
                0f,
                .5f * sinf(D12.c10startAngle + D12.c10sweepAngle),
                .5f * cosf(D12.c10startAngle + D12.c10sweepAngle) + .1f,
                .5f + .5f * sinf(D12.c10startAngle)
            )
        )
    ).build()

    // c7 is the toggle for c14, the main interceptor
    val slots = listOf(c12, c7, c14, c1, c2, c3, c4, c5, c6, c8, c9, c10, c11)
    val manager = ComplicationSlotsManager(
        slots, currentUserStyleRepository
    )

    Log.d(TAG, "Repo: " + currentUserStyleRepository.toString())
    Log.d(TAG, "Style: " + currentUserStyleRepository.userStyle.value.toMutableUserStyle().size)

    val mgrh = ComplicationSlotManagerHolder(manager, slots.map { ComplicationSlotWrapper(it) })
    manager.addTapListener(Listener(mgrh, currentUserStyleRepository))
    return mgrh
}

class Listener(
    val mgrh: ComplicationSlotManagerHolder,
    val currentUserStyleRepository: CurrentUserStyleRepository
) : ComplicationSlotsManager.TapCallback {
    override fun onComplicationSlotTapped(complicationSlotId: Int) {
        Log.d("ComplicationUtils", "onTap: " + complicationSlotId)//, Throwable("traceback"))
        val cw = mgrh.slotWrappers.find { cw -> cw.id == complicationSlotId }
        if (complicationSlotId == COMPLICATION_14) {
            mgrh.watch.scope.launch {
                VirtualComplicationPlayPauseImpl.toggleMusic(mgrh.watch)
            }
        } else if (cw != null) {
            val cb =
                cw.virtualComplication(mgrh.watch,null).onTap
            if (cb != null) cb.run()
        }
    }
}

val DEGTORAD = 3.1415f / 180f

private fun sinf(deg: Float) = Math.abs(Math.sin(deg.toDouble() * DEGTORAD)).toFloat()
private fun cosf(deg: Float) = Math.cos(deg.toDouble() * DEGTORAD).toFloat()

class ArcTapFilter(abounds: RectF) : ComplicationTapFilter {
    val b = abounds
    override fun hitTest(
        complicationSlot: ComplicationSlot, screenBounds: Rect, x: Int, y: Int
    ): Boolean {
        val w = (screenBounds.right - screenBounds.left).toFloat()
        val h = (screenBounds.bottom - screenBounds.top).toFloat()
        val t = Rect(
            (b.left * w).toInt(), (b.top * h).toInt(), (b.right * w).toInt(), (b.bottom * h).toInt()
        )
        Log.d("OFD tapFilter", b.toString() + ":" + t.toString() + ":" + x + ":" + y)
        return t.contains(x, y)
    }

    companion object {
        var TAG = "ComplicationUtils"
    }
}
