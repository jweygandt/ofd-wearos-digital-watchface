/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ofd.complications

import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.ofd.digital.alpha.OFD
import java.util.concurrent.atomic.AtomicInteger

class VirtualComplicationStatusImpl : VirtualComplication {
    override val type: ComplicationType
        get() = ComplicationType.LONG_TEXT
    override val image: Icon? get() = null
    override val text: String
        get() = OFD.status.get() ?: "Not yet set"
    override val rangeValue: Float
        get() = 0f
    override val rangeMin: Float
        get() = 0f
    override val rangeMax: Float
        get() = 0f

    override fun customDrawable(
        canvas: Canvas,
        bleft: Float,
        btop: Float,
        bbottom: Float,
        sqsize: Float
    ): Boolean = false

    override val tapCallback: Runnable?
        get() = null
}

class VirtualComplicationStatus : ComplicationDataSourceService() {


    companion object {
        private const val TAG = "ComplicationStatus"

        var uctr = AtomicInteger(0)


    }

    override fun onComplicationActivated(
        complicationInstanceId: Int,
        type: ComplicationType
    ) {
        Log.d(TAG, "onComplicationActivated(): $complicationInstanceId")


    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "Here is line one\nline 2\nline 3 and one that is long\nline 4")
                .build(),
            contentDescription = PlainComplicationText.Builder(text = "Calendar.")
                .build()
        )
            .setTapAction(null)
            .build()
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        val msg = OFD.status.get() ?: "Not yet set"
        val data = when (request.complicationType) {

            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(
                    text = "Short"
                ).build(),
                contentDescription = PlainComplicationText.Builder(text = "Calender")
                    .build()
            )
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder(
                    text = msg
                ).build(),
                contentDescription = PlainComplicationText.Builder(text = "Calender")
                    .build()
            )
                .build()

            else -> {
                Log.w(TAG, "Unexpected complication type ${request.complicationType}")
                null
            }
        }
        listener.onComplicationData(data)
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        Log.d(TAG, "Deactivated")
    }


}
