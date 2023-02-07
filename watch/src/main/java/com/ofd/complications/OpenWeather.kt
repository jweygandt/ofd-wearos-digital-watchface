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
import com.ofd.apis.WeatherResult
import com.ofd.apis.openweather.OpenWeatherActivity
import com.ofd.apis.openweather.openWeather
import com.ofd.watch.R
import com.ofd.watchface.location.WatchLocationService

/**
 * Not yet complete...
 */
class OpenWeather : SuspendingComplicationDataSourceService() {

    companion object {
        private const val TAG = "OpenWeather"
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
                openWeather(applicationContext, wl.location)
            } else {
                WeatherResult.Error("OpenWeather", "no valid location")
            }, request.complicationType
        )
    }

    private fun getComplicationData(
        weatherResult: WeatherResult, complicationType: ComplicationType
    ): ComplicationData? {
        if (complicationType == ComplicationType.SHORT_TEXT) {
            return when (weatherResult) {
                is WeatherResult.Weather -> {
                    Log.d(TAG, "Results: " + weatherResult.statusString())
                    val current = weatherResult.current
                    val image = MonochromaticImage.Builder(
                        current.currentIcon ?: Icon.createWithResource(
                            applicationContext, R.drawable.openweather
                        )
                    ).build()

                    ShortTextComplicationData.Builder(
                        text = PlainComplicationText.Builder(
                            text = current.currentTemp.toInt()
                                .toString() + "\u00b0" + "?ExpiresMS=" + (weatherResult.current.currentDt + 30 * 60 * 1000) +
                                "&IconBackground=GRAY80"
                        ).build(),
                        contentDescription = PlainComplicationText.Builder(text = "AirQuality")
                            .build(),
                    ).setMonochromaticImage(image).setTapAction(tapAction()).build()
                }

                is WeatherResult.Error -> {
                    ShortTextComplicationData.Builder(
                        text = PlainComplicationText.Builder(
                            text = weatherResult.msg + "?ExpiresMS=" + (System.currentTimeMillis() + 30 * 60 * 1000)
                        ).build(),
                        contentDescription = PlainComplicationText.Builder(text = "AirQuality")
                            .build(),
                    ).setTapAction(tapAction()).build()
                }
            }
        } else {
            Log.w(TAG, "Unexpected complication type $complicationType")
            return null
        }
    }

    fun Context.tapAction(): PendingIntent? {
        val intent =
            Intent(this, OpenWeatherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        Log.d(TAG, "Deactivated")
    }

}
