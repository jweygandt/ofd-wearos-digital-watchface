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

package com.ofd.digital.alpha.utils

import android.content.Context
import android.graphics.RectF
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.ofd.digital.alpha.OFD
import com.ofd.digital.alpha.R

private const val DEFAULT_COMPLICATION_STYLE_DRAWABLE_ID = R.drawable.complication_red_style

// y of c6,c7
private val o67 = ((OFD.c6RadiusP + .5f * OFD.g67) /
    Math.tan(Math.asin((OFD.c6RadiusP + .5 * OFD.g67) / (.5f - OFD.c6RadiusP - OFD.bottomRoundClockGapP)))).toFloat()
private val c6cx = .5f - OFD.c6RadiusP - .5f*OFD.C12GapP
private val c6cy = .5f + o67
private val c7cx = .5f + OFD.c6RadiusP + .5f*OFD.C12GapP
private val c7cy = c6cy

// bottom of first complication
private val o1xx = .5f + .5f * Math.sin(Math.acos((OFD.c1WidthP + .5f * OFD.C12GapP) / .5)).toFloat()
private val o1 = c6cy - OFD.c6RadiusP - OFD.c1C3GapP

private val c1lp = .5f - OFD.c1WidthP - .5f * OFD.C12GapP
private val c1tp = o1 - OFD.C1HeightP
private val c1rp = .5f - .5f * OFD.C12GapP
private val c1bp = o1

private val c2lp = .5f + .5f * OFD.C12GapP
private val c2tp = c1tp
private val c2rp = .5f + .5f * OFD.C12GapP + OFD.c1WidthP
private val c2bp = c1bp

private val c3lp = c1lp
private val c3tp = c1tp - OFD.c1C3GapP - OFD.C1HeightP
private val c3rp = c1rp
private val c3bp = c1tp - OFD.c1C3GapP

private val c4lp = c2lp
private val c4tp = c3tp
private val c4rp = c2rp
private val c4bp = c3bp

private val c5left = .5f - .5f * OFD.c5WidthP
private val c5top = OFD.dateBottomP +
    OFD.dateTimeGapP + OFD.timeHeightP +
    OFD.timeC5GapP
private val c5right = .5f + .5f * OFD.c5WidthP
private val c5bottom = c3tp - OFD.c3C5GapP

// Unique IDs for each complication. The settings activity that supports allowing users
// to select their complication data provider requires numbers to be >= 0.
//internal const val COMPLICATION_1 = 101
//internal const val COMPLICATION_2 = 102
//internal const val COMPLICATION_3 = 103
//internal const val COMPLICATION_4 = 104
//internal const val COMPLICATION_5 = 105
//internal const val COMPLICATION_6 = 106
//internal const val COMPLICATION_7 = 107

/**
 * Represents the unique id associated with a complication and the complication types it supports.
 */
sealed class ComplicationConfig3(val id: Int, val supportedTypes: List<ComplicationType>) {
    object C1 : ComplicationConfig(
        COMPLICATION_1,
        listOf(
            ComplicationType.SHORT_TEXT
        )
    )

    object C2 : ComplicationConfig(
        COMPLICATION_2,
        listOf(
            ComplicationType.SHORT_TEXT
        )
    )

    object C3 : ComplicationConfig(
        COMPLICATION_3,
        listOf(
            ComplicationType.SHORT_TEXT
        )
    )

    object C4 : ComplicationConfig(
        COMPLICATION_4,
        listOf(
            ComplicationType.SHORT_TEXT
        )
    )

    object C5 : ComplicationConfig(
        COMPLICATION_5,
        listOf(
//            ComplicationType.RANGED_VALUE,
//            ComplicationType.MONOCHROMATIC_IMAGE,
//            ComplicationType.SHORT_TEXT,
//            ComplicationType.SMALL_IMAGE,
            ComplicationType.LONG_TEXT,
            ComplicationType.LIST
        )
    )

    object C6 : ComplicationConfig(
        COMPLICATION_6,
        listOf(
            ComplicationType.SMALL_IMAGE,
        )
    )

    object C7 : ComplicationConfig(
        COMPLICATION_7,
        listOf(
            ComplicationType.SMALL_IMAGE,
        )
    )
}

// Utility function that initializes default complication slots (left and right).
fun createComplicationSlotManager3(
    context: Context,
    currentUserStyleRepository: CurrentUserStyleRepository,
    drawableId: Int = DEFAULT_COMPLICATION_STYLE_DRAWABLE_ID
): ComplicationSlotsManager {
    val defaultCanvasComplicationFactory =
        CanvasComplicationFactory { watchState, listener ->
            CanvasComplicationDrawable(
                ComplicationDrawable.getDrawable(context, drawableId)!!,
                watchState,
                listener
            )
        }

    val c1 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C1.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C1.supportedTypes,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.DATA_SOURCE_DAY_OF_WEEK,
            ComplicationType.SHORT_TEXT
        ),
        bounds = ComplicationSlotBounds(
            RectF(
                c1lp,
                c1tp,
                c1rp,
                c1bp
            )
        )
    )
        .build()

    val c2 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C2.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C2.supportedTypes,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.DATA_SOURCE_STEP_COUNT,
            ComplicationType.SHORT_TEXT
        ),
        bounds = ComplicationSlotBounds(
            RectF(
                c2lp,
                c2tp,
                c2rp,
                c2bp
            )
        )
    ).build()


    val c3 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C3.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C3.supportedTypes,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.DATA_SOURCE_STEP_COUNT,
            ComplicationType.SHORT_TEXT
        ),
        bounds = ComplicationSlotBounds(
            RectF(
                c3lp,
                c3tp,
                c3rp,
                c3bp
            )
        )
    ).build()


    val c4 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C4.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C4.supportedTypes,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.DATA_SOURCE_STEP_COUNT,
            ComplicationType.SHORT_TEXT
        ),
        bounds = ComplicationSlotBounds(
            RectF(
                c4lp,
                c4tp,
                c4rp,
                c4bp
            )
        )
    ).build()

    val c5 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C5.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C5.supportedTypes,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.DATA_SOURCE_DAY_AND_DATE,
            ComplicationType.LONG_TEXT
        ),
        bounds = ComplicationSlotBounds(
            RectF(
                c5left,
                c5top,
                c5right,
                c5bottom
            )
        )
    ).build()

    val c6 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C6.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C6.supportedTypes,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.NO_DATA_SOURCE,
            ComplicationType.SMALL_IMAGE
        ),
        bounds = ComplicationSlotBounds(
            RectF(
                c6cx-OFD.c6RadiusP,
                c6cy-OFD.c6RadiusP,
                c6cx+OFD.c6RadiusP,
                c6cy+OFD.c6RadiusP
            )
        )
    ).build()

    val c7 = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        id = ComplicationConfig.C7.id,
        canvasComplicationFactory = defaultCanvasComplicationFactory,
        supportedTypes = ComplicationConfig.C7.supportedTypes,
        defaultDataSourcePolicy = DefaultComplicationDataSourcePolicy(
            SystemDataSources.NO_DATA_SOURCE,
            ComplicationType.SMALL_IMAGE
        ),
        bounds = ComplicationSlotBounds(
            RectF(
                c7cx-OFD.c6RadiusP,
                c7cy-OFD.c6RadiusP,
                c7cx+OFD.c6RadiusP,
                c7cy+OFD.c6RadiusP
            )
        )
    ).build()

    return ComplicationSlotsManager(
        listOf(c1, c2, c3, c4, c5, c6, c7),
        currentUserStyleRepository
    )
}
