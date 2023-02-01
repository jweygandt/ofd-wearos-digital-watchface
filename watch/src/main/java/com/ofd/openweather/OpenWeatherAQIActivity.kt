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
import kotlinx.coroutines.withContext


class OpenWeatherAQIActivity : ComponentActivity() {

    var lastTime: Long = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OpenWeatherAQIMainApp(
            )
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            when (val aqi = getAQI(
                applicationContext, LocationViewModel(
                    TAG, applicationContext
                ).readLocationResult() as ResolvedLocation
            )) {
                is AQIResult.AQI -> {
                    withContext(Dispatchers.IO) {
                        aqiaddress.value = try {
                            aqi.rlocation.getShortAddress()
                        } catch (e:java.lang.Exception) {
                            null
                        }
                        Log.d(TAG, "address: " + aqiaddress.value)
                    }
                    aqidata.value = aqi
                }

                is AQIResult.Error -> {
                    Log.e(TAG, "Error: " + aqi.msg)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    companion object {
        val TAG = "OpenWeatherActivity"
    }
}
