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
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.ofd.openweather.OpenWeatherActivity
import com.ofd.openweather.OpenWeatherService
import com.ofd.watch.R
import com.ofd.watchface.location.WatchLocationService
import kotlinx.coroutines.runBlocking

/**
 * Not yet complete...
 */
class OpenWeather : ComplicationDataSourceService() {


    companion object {
        private const val TAG = "OpenWeather"
    }

    private var api: OpenWeatherService.OpenWeatherAPI? = null

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

    override fun onComplicationRequest(
        request: ComplicationRequest, listener: ComplicationRequestListener
    ) {
        Log.d(TAG, "onComplicationRequest() id: ${request.complicationInstanceId}")

        val wl = WatchLocationService.getLocation()
        Log.d(TAG, "Updating AirQuality: " + wl.valid)
        if (api == null) api = OpenWeatherService.OpenWeatherAPI(applicationContext)

        if (wl.valid) {
            api!!.get(wl.location) {
                updateComplication(request.complicationType, listener, it)
            }
        } else {
            updateComplication(
                request.complicationType,
                listener,
                OpenWeatherService.OWNowResult("no location", 0f, "", false)
            )
        }
    }

    private fun updateComplication(
        complicationType: ComplicationType,
        listener: ComplicationRequestListener,
        ow: OpenWeatherService.OWNowResult
    ) {
        runBlocking {
            Log.d(TAG, "Results: " + OpenWeatherService.OpenWeatherAPI.statusString())
            //aqd.sortedSensors.forEach { s -> Log.d(TAG, s.toString()) }

//            val request = ImageRequest.Builder(applicationContext)
//                .data("http://openweathermap.org/img/wn/10d@2x.png").build()
//            val result = imageLoader.execute(request)

            val image = MonochromaticImage.Builder(
                ow.icon ?: Icon.createWithResource(
                    applicationContext, R.drawable.openweather
                )
            ).build()
            val cdata = when (complicationType) {

                ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(
                        text = ow.temp.toInt()
                            .toString() + "\u00b0" + "?ExpiresMS=" + (System.currentTimeMillis() + 30 * 60 * 1000)
                    ).build(),
                    contentDescription = PlainComplicationText.Builder(text = "AirQuality").build(),
                ).setMonochromaticImage(image).setTapAction(tapAction()).build()

                else -> {
                    Log.w(TAG, "Unexpected complication type $complicationType")
                    null
                }
            }

            listener.onComplicationData(cdata)
        }
    }

    fun Context.tapAction(): PendingIntent? {
        val intent = Intent(this, OpenWeatherActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        Log.d(TAG, "Deactivated")
    }


}
