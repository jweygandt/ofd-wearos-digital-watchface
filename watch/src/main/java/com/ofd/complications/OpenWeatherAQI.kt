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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.ofd.apis.AQIResult
import com.ofd.apis.openweather.OpenWeatherAQIActivity
import com.ofd.apis.openweather.openWeatherAQI
import com.ofd.watch.R
import com.ofd.watchface.location.WatchLocationService

class OpenWeatherAQI : SuspendingComplicationDataSourceService() {


    companion object {
        private const val TAG = "OpenWeatherAQI"
    }


    override fun onComplicationActivated(
        complicationInstanceId: Int, type: ComplicationType
    ) {
        Log.d(TAG, "onComplicationActivated(): $complicationInstanceId")
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "AirQuality").build(),
            contentDescription = PlainComplicationText.Builder(text = "AirQuality").build()
        ).setTapAction(null).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        Log.d(TAG, "onComplicationRequest() id: ${request.complicationInstanceId}")

        val wl = WatchLocationService.getLocation()
        Log.d(TAG, "Updating AirQuality: " + wl.valid)

        return getComplicationData(
            if (wl.valid) {
                openWeatherAQI(applicationContext, wl.location)
            } else {
                AQIResult.Error("OpenWeather","no location")
            }, request.complicationType
        )
    }

    private fun getComplicationData(
        aqiResult: AQIResult, complicationType: ComplicationType
    ): ComplicationData? {
        return when (aqiResult) {
            is AQIResult.AQI -> {
                Log.d(TAG, "Results: " + aqiResult.statusString())
                //aqd.sortedSensors.forEach { s -> Log.d(TAG, s.toString()) }
                val image = MonochromaticImage.Builder(
                    Icon.createWithResource(
                        applicationContext, R.drawable.ic_air_quality
                    )
                ).build()
                when (complicationType) {

                    ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                        text = PlainComplicationText.Builder(
                            text = aqiResult.aqistr
                        ).build(),
                        contentDescription = PlainComplicationText.Builder(text = "AirQuality")
                            .build(),
                    ).setMonochromaticImage(image).build()

                    ComplicationType.RANGED_VALUE -> {
                        val mx: Float
                        val mn: Float
                        val value = aqiResult.aqippm
                        val color = aqiResult.color - 1
                        if (value <= 50) {
                            mn = 0f
                            mx = 50f
//                    color = 0
                        } else if (value <= 100) {
                            mn = 50f
                            mx = 100f
//                    color = 1
                        } else if (value <= 150) {
                            mn = 100f
                            mx = 150f
//                    color = 2
                        } else if (value <= 200) {
                            mn = 150f
                            mx = 200f
//                    color = 3
                        } else if (value <= 300) {
                            mn = 200f
                            mx = 300f
//                    color = 4
                        } else {
                            mn = 300f
                            mx = 400f
//                    color = 5
                        }

                        RangedValueComplicationData.Builder(
                            value = value.toFloat(),
                            min = mn,
                            max = mx,
                            contentDescription = PlainComplicationText.Builder(
                                (value.toInt()
                                    .toString() + " ppm") + "?Color=" + color + "&ExpiresMS=" + (System.currentTimeMillis() + 30 * 60 * 1000)
                            ).build()
                        ).setMonochromaticImage(image).setTapAction(tapAction()).build()

                    }
                    else -> null
                }
            }
            is AQIResult.Error -> {
                Log.w(TAG, "Unexpected complication type $complicationType")
                null
            }
        }
    }

    fun Context.tapAction(): PendingIntent? {
        val intent = Intent(
            this, OpenWeatherAQIActivity::class.java
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }


    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        Log.d(TAG, "Deactivated")
    }


}
