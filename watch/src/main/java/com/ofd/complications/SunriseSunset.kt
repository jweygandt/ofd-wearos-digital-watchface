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

import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.ofd.watchface.location.WatchLocationService
import com.ofd.sunrisesunset.SunriseSunsetCalculator
import com.ofd.sunrisesunset.dto.SSLocation
import java.text.SimpleDateFormat
import java.util.*
import com.ofd.watch.R

class SunriseSunset : SuspendingComplicationDataSourceService() {


    companion object {
        private const val TAG = "SunriseSunset"
        val sdf = SimpleDateFormat("h:mm")
    }


    override fun onComplicationActivated(
        complicationInstanceId: Int, type: ComplicationType
    ) {
        Log.d(TAG, "onComplicationActivated(): $complicationInstanceId")
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "Sunrise")
                .build(),
            contentDescription = PlainComplicationText.Builder(text = "Calendar.").build()
        ).setTapAction(null).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        Log.d(TAG, "onComplicationRequest() id: ${request.complicationInstanceId}")

        val wl = WatchLocationService.getLocation()
        var time = "--:--"
        val image: MonochromaticImage?
        if (wl.valid) {
            val ssc = SunriseSunsetCalculator(
                SSLocation(
                    wl.latitude.toString(), wl.longitude.toString()
                ), TimeZone.getDefault()
            )
            val now = Calendar.getInstance()
            now.add(Calendar.MINUTE, -30) // Keep the setting for 30 extra minutes
            val sunrise = ssc.getCivilSunriseCalendarForDate(now)
            val sunset = ssc.getCivilSunsetCalendarForDate(now)
            val nh = now.get(Calendar.HOUR_OF_DAY)
            val nm = now.get(Calendar.MINUTE)
            val rh = sunrise.get(Calendar.HOUR_OF_DAY)
            val rm = sunrise.get(Calendar.MINUTE)
            val sh = sunset.get(Calendar.HOUR_OF_DAY)
            val sm = sunset.get(Calendar.MINUTE)
            val rise = (nh < rh) || (nh > sh) ||
                (nh == rh && nm < rm) ||
                (nh == sh && nm > sm)
            Log.d(TAG, "${rise}:${nh}:${rh}:${sh}")
            time = sdf.format(if (rise) sunrise.time.time else sunset.time.time)
            image = MonochromaticImage.Builder(
                Icon.createWithResource(
                    applicationContext,
                    if (rise) R.drawable.ic_sunrise else R.drawable.ic_sunset
                )
            ).build()
        }else {
            image =MonochromaticImage.Builder(
                Icon.createWithResource(
                    applicationContext,
                    R.drawable.ic_sunset
                )
            ).build()
        }
        return when (request.complicationType) {

            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(
                    text = time
                ).build(),
                contentDescription = PlainComplicationText.Builder(text = "SunriseSunset").build(),
            ).setMonochromaticImage(image)
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
