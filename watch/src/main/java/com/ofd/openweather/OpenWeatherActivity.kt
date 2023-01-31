/*
 * Copyright 2021 The Android Open Source Project
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
package com.ofd.openweather

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.ofd.watchface.location.LocationViewModel
import com.ofd.watchface.location.ResolvedLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class OpenWeatherActivity : ComponentActivity() {

    var lastTime: Long = 0;
    var forecast: OpenWeatherService.OWForecastResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OpenWeatherMainApp(
            )
        }
    }

    override fun onResume() {
        super.onResume()
        row1.value = emptyList()
        lifecycleScope.launch {
            when (val weather = OpenWeatherService3.OpenWeatherAPI(applicationContext).getWeather(
                LocationViewModel(
                    TAG, applicationContext
                ).readLocationResult() as ResolvedLocation
            )) {
                is OpenWeatherService3.WeatherResult.Weather -> { weather3.value=weather}

                is OpenWeatherService3.WeatherResult.Error -> {
                    Log.e(TAG, "Error: " + weather.msg)}
            }
        }

        if (false) lifecycleScope.launch(Dispatchers.IO) {
            if (forecast == null || lastTime + 5 * 60000 < System.currentTimeMillis()) {
                val location = LocationViewModel(
                    "OpenWeatherActivity", applicationContext
                ).readLocationResult()
                forecast = OpenWeatherService.OpenWeatherAPI(applicationContext)
                    .getForecast(location as ResolvedLocation)
//                        .getForecast(WatchLocationService.getLocation().location.location!!)
                lastTime = System.currentTimeMillis()
            }
            val l = mutableListOf<Hourly>()
            for (i in 4..7) {
                val e = forecast!!.data[i]
                l.add(Hourly(i, e.temp.toInt(), e.bitmap))
            }
            row1.value = l
//            val i =  BitmapFactory.decodeResource(resources, R.drawable.openweather)
//            val h = Hourly(55,86668, i)
//            row1.value = listOf(h,h,h,h)
        }
    }

    override fun onPause() {
        super.onPause()
    }

    companion object {
        val TAG = "OpenWeatherActivity"
    }
}
