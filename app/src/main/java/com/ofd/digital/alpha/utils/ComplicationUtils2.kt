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
package com.ofd.digital.alpha.utils

import android.content.Context
import android.graphics.RectF
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.ComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.ofd.digital.alpha.OFD
import com.ofd.digital.alpha.R

private const val DEFAULT_COMPLICATION_STYLE_DRAWABLE_ID = R.drawable.complication_red_style

private const val g1 = .028f
private const val g2 = .028f
private const val r = .100f
private const val C = .500f
private val o1 = ((r + .5f * g2) /
    Math.tan(Math.asin((r + .5 * g2) / (C - r - g1)))).toFloat()
private const val cx = .500f
private const val cy = .500f

private val c1x = cx - r - .5f * g2
private val c1y = cy + o1

private val c2x = cx + r + .5f * g2
private val c2y = cy + o1

private val c3x = c1x
private val c3y = cy + o1 - 2 * r - g1

private val c4x = c2x
private val c4y = c3y

private const val g3 = .028f
private const val g4 = .028f
private const val c5w = .630f

private val c5top = OFD.dateBottomP +
    OFD.dateTimeGapP + OFD.timeHeightP +
    g4
private val c5bottom = cy + o1 - 3f * r - g2 - g3
private val c5left = cx - .5f * c5w
private val c5right = cx + .5f * c5w

// Unique IDs for each complication. The settings activity that supports allowing users
// to select their complication data provider requires numbers to be >= 0.
//internal const val COMPLICATION_1 = 101
//internal const val COMPLICATION_2 = 102
//internal const val COMPLICATION_3 = 103
//internal const val COMPLICATION_4 = 104
//internal const val COMPLICATION_5 = 105

/**
 * Represents the unique id associated with a complication and the complication types it supports.
 */
sealed class ComplicationConfig2(val id: Int, val supportedTypes: List<ComplicationType>) {
    object C1 : ComplicationConfig(
        COMPLICATION_1,
        listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.SMALL_IMAGE
        )
    )

    object C2 : ComplicationConfig(
        COMPLICATION_2,
        listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.SMALL_IMAGE
        )
    )

    object C3 : ComplicationConfig(
        COMPLICATION_3,
        listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.SMALL_IMAGE
        )
    )

    object C4 : ComplicationConfig(
        COMPLICATION_4,
        listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.SMALL_IMAGE
        )
    )

    object C5 : ComplicationConfig(
        COMPLICATION_5,
        listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.SMALL_IMAGE,
            ComplicationType.LONG_TEXT
        )
    )
}

// Utility function that initializes default complication slots (left and right).
fun createComplicationSlotManager2(
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
                c1x - r,
                c1y - r,
                c1x + r,
                c1y + r
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
                c2x - r,
                c2y - r,
                c2x + r,
                c2y + r
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
                c3x - r,
                c3y - r,
                c3x + r,
                c3y + r
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
                c4x - r,
                c4y - r,
                c4x + r,
                c4y + r
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

    return ComplicationSlotsManager(
        listOf(c1, c2, c3, c4, c5),
        currentUserStyleRepository
    )
}
