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
package com.ofd.digital.alpha

import android.graphics.RectF
import android.graphics.drawable.Icon
import android.util.Log
import android.view.SurfaceHolder
import androidx.wear.watchface.*
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.style.*
import com.ofd.digital.alpha.location.WatchLocationService
import com.ofd.digital.alpha.utils.COMPLICATION_12
import com.ofd.digital.alpha.utils.COMPLICATION_2
import com.ofd.digital.alpha.utils.createComplicationSlotManager

/**
 * Handles much of the boilerplate needed to implement a watch face (minus rendering code; see
 * [DigitalWatchCanvasRenderer]) including the complications and settings (styles user can change on
 * the watch face).
 */
class DigitalWatchFaceService : WatchFaceService() {

    // Creates all complication user settings and adds them to the existing user settings
    // repository.
    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager = createComplicationSlotManager(
        context = applicationContext,
        currentUserStyleRepository = currentUserStyleRepository
    )

    lateinit var optionDisable: UserStyleSetting.ComplicationSlotsUserStyleSetting
    .ComplicationSlotsOption
    lateinit var styleSetting : UserStyleSetting.ComplicationSlotsUserStyleSetting

    override fun createUserStyleSchema(): UserStyleSchema {

        val icon = Icon.createWithResource(applicationContext, R.drawable.ic_air_quality)

        val c12overlay = UserStyleSetting.ComplicationSlotsUserStyleSetting
            .ComplicationSlotOverlay.Builder(COMPLICATION_12).setEnabled(true)
            .setComplicationSlotBounds(ComplicationSlotBounds(RectF(0f,0f,1f,1f))).build()

        val slotOverlayEnable = UserStyleSetting.ComplicationSlotsUserStyleSetting
            .ComplicationSlotOverlay.Builder(COMPLICATION_2).setEnabled(true).build()

        val optionEnable = UserStyleSetting.ComplicationSlotsUserStyleSetting
            .ComplicationSlotsOption(
                UserStyleSetting.Option.Id("optionEnable"),
                applicationContext.resources,
                R.string.blue_style_name,
                icon,
                listOf(slotOverlayEnable,c12overlay),
                null
            )

        val slotOverlayDisable = UserStyleSetting.ComplicationSlotsUserStyleSetting
            .ComplicationSlotOverlay.Builder(COMPLICATION_2).setEnabled(false).build()

        optionDisable = UserStyleSetting.ComplicationSlotsUserStyleSetting
            .ComplicationSlotsOption(
                UserStyleSetting.Option.Id("optionDisable"),
                applicationContext.resources,
                R.string.blue_style_name,
                icon,
                listOf(slotOverlayDisable,c12overlay),
                null
            )
        styleSetting = UserStyleSetting.ComplicationSlotsUserStyleSetting(
            UserStyleSetting.Id("customStyle"),
            applicationContext.resources,
            R.string.app_name,
            R.string.app_name,
            icon,
            listOf(optionDisable, optionEnable),
            listOf(WatchFaceLayer.COMPLICATIONS),
            optionDisable,
            null
        )

//        return UserStyleSchema(listOf(styleSetting))
        return super.createUserStyleSchema()
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        Log.d(TAG, "createWatchFace()")

        WatchLocationService.reset()

        // Creates class that renders the watch face.
        val renderer = DigitalWatchCanvasRenderer(
            context = applicationContext,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            complicationSlotsManager = complicationSlotsManager,
            currentUserStyleRepository = currentUserStyleRepository,
            canvasType = CanvasType.HARDWARE
        )

        // Creates the watch face.
        return WatchFace(
            watchFaceType = WatchFaceType.DIGITAL,
            renderer = renderer
        ).setTapListener(wflistener)
    }

    object wflistener : WatchFace.TapListener {
        override fun onTapEvent(
            tapType: Int,
            tapEvent: TapEvent,
            complicationSlot: ComplicationSlot?
        ) {
            Log.d(
                TAG, "onTap: " + tapType + ":" + tapEvent.toString() + ":" +
                    (complicationSlot?.id ?: -1).toString()
            )
        }
    }


    companion object {
        const val TAG = "OFDDigital"
    }
}
