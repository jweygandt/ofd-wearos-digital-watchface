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

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.ofd.digital.alpha.DigitalWatchCanvasRenderer
import com.ofd.sunrisesunset.SunriseSunsetCalculator
import com.ofd.sunrisesunset.dto.SSLocation
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


class LocationTest : SuspendingComplicationDataSourceService() {


    companion object {
        private const val TAG = "CalendarService"

        var uctr = AtomicInteger(0)

        fun forceComplicationUpdate(applicationContext: Context) {
            Log.d(TAG, "really updating complication")
            val request =
                ComplicationDataSourceUpdateRequester.create(
                    applicationContext, ComponentName(
                        applicationContext, LocationTest::class.java
                    )
                )
            request.requestUpdateAll()
        }

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

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        Log.d(TAG, "onComplicationRequest() id: ${request.complicationInstanceId}")

        val ll = DigitalWatchCanvasRenderer.lastLocation.get()
        val msg = if (ll != null) {
            val loc = ll.location
            val ssc = SunriseSunsetCalculator(
                SSLocation(
                    loc.location.latitude.toString(),
                    loc.location.longitude.toString()
                ), TimeZone.getDefault()
            )
            val sunrise = ssc.getCivilSunriseForDate(Calendar.getInstance())
            val sunset = ssc.getCivilSunsetForDate(Calendar.getInstance())
            loc.getAddressDescription() + "\n" +
                TimeZone.getDefault().id + "\n" + //loc.getTimeAgo() + "\n" +
                ll.callcnt.toString() + ":" + ll.successcnt.toString() + " " +
                sunrise + " " + sunset + " " + TimeZone.getDefault().id
        } else {
            "Error getting location"
        }

        return when (request.complicationType) {

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
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        Log.d(TAG, "Deactivated")
    }


}
