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

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import java.text.SimpleDateFormat
import java.util.*


val weather3 = mutableStateOf<WeatherResult.Weather?>(null)

private val Calendar.hour
    get() = get(Calendar.HOUR_OF_DAY)

private val Calendar.day
    get() = get(Calendar.DAY_OF_YEAR)

private val Calendar.date
    get() = get(Calendar.DAY_OF_MONTH)

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun OpenWeatherMainApp(
) {
    val scalingLazyListState = rememberScalingLazyListState(0, 0)

    val lazyRowState = rememberLazyListState()
    val scrollState = rememberScrollState()

    val sdffull = SimpleDateFormat("MM/dd HH:mm")

    val blackPainter =
        CardDefaults.cardBackgroundPainter(Color.Black, Color.Black, LayoutDirection.Ltr)

    Scaffold(vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = scalingLazyListState) },
        modifier = Modifier.background(Color.DarkGray),
        timeText = { TimeText() }) {

        var W by remember { weather3 }

//        Column(modifier = Modifier.verticalScroll(scrollState)) {
//            @Composable
//            fun item(content: @Composable () -> Unit): Unit = content.invoke()

        ScalingLazyColumn(
            state = scalingLazyListState, contentPadding = PaddingValues(
                horizontal = 8.dp, vertical = 32.dp
            ), anchorType = ScalingLazyListAnchorType.ItemStart
        ) {

            val WW = W
            if (WW == null) {
                item { Text("Loading...") }
            } else {
                if (WW.address != null) item { Text(WW.address ?: "not set") }
                item {
                    TitleCard(onClick = {},
                        backgroundPainter = blackPainter,
                        modifier = Modifier.fillMaxWidth(),
                        title = { Text(sdffull.format(WW.current.currentDt)) }) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Text(WW.current.currentTemp.toInt().toString() + "\u00b0")
                            RenderBitmap(WW.current.currentBitmap)
                        }
                    }
                }
                fun needToBreak(today: Calendar, data: Calendar): Boolean {
                    return if (today.day == data.day) false
                    else if (today.day + 1 == data.day && data.hour == 0) false
                    else true
                }

                fun dataCol0Based(hour: Int): Int {
                    return (hour - 1) % 6
                }

                fun stringForHour(hour: Int): String {
                    return if (hour == 0) "12" else if (hour <= 12) hour.toString() else (hour - 12).toString()
                }
                item {

                    TitleCard(onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        backgroundPainter = blackPainter,
                        title = { Text("Today") }) {
                        val caltoday = Calendar.getInstance()
                        caltoday.timeInMillis = WW.current.currentDt
                        val today = caltoday.day
                        var inx = 0;
                        val caldata = Calendar.getInstance()
                        while (inx < WW.hourlys.size) {
                            val h = WW.hourlys.get(inx)
                            caldata.timeInMillis = h.date
                            val day = caldata.day
                            if (needToBreak(caltoday, caldata)) break;
                            Row(
                                horizontalArrangement = Arrangement.SpaceAround,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val skip = dataCol0Based(caldata.hour)
                                for (i in (0..skip - 1)) {
                                    Column(
                                        horizontalAlignment = CenterHorizontally,
                                    ) {
                                        // empty
                                    }
                                }
                                do {
                                    val w = WW.hourlys[inx]
                                    caldata.timeInMillis = w.date
                                    val hour = caldata.hour
                                    Column(
                                        horizontalAlignment = CenterHorizontally,
                                    ) {
                                        Text(stringForHour(hour))
                                        Text(w.temp.toInt().toString() + "\u00b0")
                                        RenderBitmap(w.bitmap)
                                    }
                                    inx++
                                } while (dataCol0Based(hour + 1) != 0)
                            }

                        }
                    }
                }
                item {
                    TitleCard(onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        backgroundPainter = blackPainter,
                        title = { Text("Tomorrow") }) {
                        val caltoday = Calendar.getInstance()
                        caltoday.timeInMillis = WW.current.currentDt
                        val caldate = Calendar.getInstance()
                        var inx = 0;
                        while (inx < WW.hourlys.size) {
                            val h = WW.hourlys[inx]
                            caldate.timeInMillis = h.date
                            if (needToBreak(caltoday, caldate)) break;
                            inx++
                        }
                        val tomorrowday = caldate.day
                        while (inx < WW.hourlys.size) {
                            val h = WW.hourlys.get(inx)
                            caldate.timeInMillis = h.date
                            val day = caldate.day
                            if (tomorrowday != day) break;
                            Row(
                                horizontalArrangement = Arrangement.SpaceAround,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val skip = dataCol0Based(caldate.hour)
                                for (i in (0..skip - 1)) {
                                    Column(
                                        horizontalAlignment = CenterHorizontally,
                                    ) {
                                        // empty
                                    }
                                }
                                do {
                                    val w = WW.hourlys[inx]
                                    caldate.timeInMillis = w.date
                                    val hour = caldate.hour
                                    Column(
                                        horizontalAlignment = CenterHorizontally,
                                    ) {
                                        Text(stringForHour(hour))
                                        Text(w.temp.toInt().toString() + "\u00b0")
                                        RenderBitmap(w.bitmap)
                                    }
                                    inx++
                                } while (dataCol0Based(hour + 1) != 0)
                            }

                        }
                    }
                }
                item {
                    TitleCard(onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        backgroundPainter = blackPainter,
                        title = { Text("Future") }) {
                        val cal = Calendar.getInstance()
                        var inx = 0;
                        while (inx < WW.dailys.size) {
                            val d = WW.dailys.get(inx)
                            cal.timeInMillis = d.date
                            Row(
                                horizontalArrangement = Arrangement.SpaceAround,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                do {
                                    val w = WW.dailys[inx]
                                    cal.timeInMillis = w.date
                                    val date = cal.date
                                    Column(
                                        horizontalAlignment = CenterHorizontally,
                                    ) {
                                        Text(date.toString())
                                        Text(w.tempMax.toInt().toString() + "\u00b0")
                                        Text(w.tempMin.toInt().toString() + "\u00b0")
                                        RenderBitmap(w.bitmap)
                                    }
                                    inx++
                                } while (inx % 6 != 0 && inx < WW.dailys.size)
                                while (inx % 6 != 0) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        //empty
                                    }
                                    inx++
                                }
                            }

                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderBitmap(bm: Bitmap?) {
    if (bm != null) Image(
        bm.asImageBitmap(), "", alignment = Alignment.Center, modifier = Modifier.size(24.dp)
//            .background(Color.Gray, CircleShape)
    )
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun MainAppPreviewEvents() {
    OpenWeatherMainApp(
    )
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun MainAppPreviewEmpty() {
    OpenWeatherMainApp(
    )
}
