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
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.ofd.watchface.location.WatchLocationService
import com.ofd.openweather.OpenWeatherAQIService
import com.ofd.watch.R

class OpenWeatherAQI : ComplicationDataSourceService() {


    companion object {
        private const val TAG = "OpenWeatherAQI"
    }

    private var api: OpenWeatherAQIService.OpenWeatherAQIAPI? = null

    override fun onComplicationActivated(
        complicationInstanceId: Int, type: ComplicationType
    ) {
        Log.d(TAG, "onComplicationActivated(): $complicationInstanceId")
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "AirQuality")
                .build(),
            contentDescription = PlainComplicationText.Builder(text = "AirQuality").build()
        ).setTapAction(null).build()
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        Log.d(TAG, "onComplicationRequest() id: ${request.complicationInstanceId}")

        val wl = WatchLocationService.getLocation()
        Log.d(TAG, "Updating AirQuality: " + wl.valid)
        if (api == null)
            api = OpenWeatherAQIService.OpenWeatherAQIAPI(applicationContext)

        if (wl.valid) {
            api!!.getAQI(wl.location.location!!){
                updateComplication(request.complicationType, listener, it)
            }
        } else {
            updateComplication(
                request.complicationType, listener,
                OpenWeatherAQIService.OWAQIResult("no location", 0, 0f, false)
            )
        }
    }

    private fun updateComplication(
        complicationType: ComplicationType,
        listener: ComplicationRequestListener,
        aqi: OpenWeatherAQIService.OWAQIResult
    ) {
        Log.d(TAG, "Results: " + OpenWeatherAQIService.OpenWeatherAQIAPI.statusString())
        //aqd.sortedSensors.forEach { s -> Log.d(TAG, s.toString()) }
        val image = MonochromaticImage.Builder(
            Icon.createWithResource(
                applicationContext,
                R.drawable.ic_air_quality
            )
        ).build()
        val cdata = when (complicationType) {

            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(
                    text = aqi.aqi.toString() + " ppm"
                ).build(),
                contentDescription = PlainComplicationText.Builder(text = "AirQuality").build(),
            ).setMonochromaticImage(image)
                .build()

            ComplicationType.RANGED_VALUE -> {
                val mx: Float
                val mn: Float
                val color: Int
                val value = aqi.aqi
                if (value <= 50) {
                    mn = 0f
                    mx = 50f
                    color = 0
                } else if (value <= 100) {
                    mn = 50f
                    mx = 100f
                    color = 1
                } else if (value <= 150) {
                    mn = 100f
                    mx = 150f
                    color = 2
                } else if (value <= 200) {
                    mn = 150f
                    mx = 200f
                    color = 3
                } else if (value <= 300) {
                    mn = 200f
                    mx = 300f
                    color = 4
                } else {
                    mn = 300f
                    mx = 400f
                    color = 5
                }

                RangedValueComplicationData.Builder(
                    value = value,
                    min = mn,
                    max = mx,
                    contentDescription = PlainComplicationText.Builder(
                        (if (aqi.hasRealData) value.toInt().toString() + " ppm" else "--") +
                            "?Color:" + color
                    ).build()
                ).setMonochromaticImage(image)
                    .build()

            }
            else -> {
                Log.w(TAG, "Unexpected complication type $complicationType")
                null
            }
        }

        listener.onComplicationData(cdata)
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        Log.d(TAG, "Deactivated")
    }


}
