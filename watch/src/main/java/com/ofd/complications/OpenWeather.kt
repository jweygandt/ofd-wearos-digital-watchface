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
import com.ofd.digital.alpha.R
import com.ofd.digital.alpha.location.WatchLocationService
import com.ofd.openweather.OpenWeatherService
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking

class OpenWeather : ComplicationDataSourceService() {


    companion object {
        private const val TAG = "OpenWeather"
        val uctr = AtomicInteger(0)
    }

    private var api: OpenWeatherService.OpenWeatherAPI? = null

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
            api = OpenWeatherService.OpenWeatherAPI(applicationContext)

        if (wl.valid) {
            api!!.get(wl.location.location!!) {
                updateComplication(request.complicationType, listener, it)
            }
        } else {
            updateComplication(
                request.complicationType, listener,
                OpenWeatherService.OWResult("no location", 0f, "", false)
            )
        }
    }

    fun updateComplication(
        complicationType: ComplicationType,
        listener: ComplicationRequestListener,
        aqi: OpenWeatherService.OWResult
    ) {
        runBlocking {
            Log.d(TAG, "Results: " + OpenWeatherService.OpenWeatherAPI.statusString())
            //aqd.sortedSensors.forEach { s -> Log.d(TAG, s.toString()) }

//            val request = ImageRequest.Builder(applicationContext)
//                .data("http://openweathermap.org/img/wn/10d@2x.png").build()
//            val result = imageLoader.execute(request)

            val image = MonochromaticImage.Builder(
                Icon.createWithResource(
                    applicationContext,
                    R.drawable.ic_air_quality
                )
            ).build()
            val cdata = when (complicationType) {

                ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(
                        text = aqi.temp.toInt().toString() + "\u00b0"
                    ).build(),
                    contentDescription = PlainComplicationText.Builder(text = "AirQuality").build(),
                ).setMonochromaticImage(image)
                    .build()

                else -> {
                    Log.w(TAG, "Unexpected complication type ${complicationType}")
                    null
                }
            }

            listener.onComplicationData(cdata)
        }
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        Log.d(TAG, "Deactivated")
    }


}
