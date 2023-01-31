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


class Hourly(public val hour: Int, public val temp: Int, public val bitmap: Bitmap?)

//val row1 = mutableStateListOf<Hourly>()

val row1 = mutableStateOf(listOf<Hourly>())

val weather3 = mutableStateOf<OpenWeatherService3.WeatherResult.Weather?>(null)

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

    val sdffull = SimpleDateFormat("MM/dd HH:mm")

    val blackPainter =
        CardDefaults.cardBackgroundPainter(Color.Black, Color.Black, LayoutDirection.Ltr)

    Scaffold(vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = scalingLazyListState) },
        modifier = Modifier.background(Color.DarkGray),
        timeText = { TimeText() }) {

        var W by remember { weather3 }

        ScalingLazyColumn(
            state = scalingLazyListState, contentPadding = PaddingValues(
                horizontal = 8.dp, vertical = 32.dp
            )
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
                item {
                    TitleCard(onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        backgroundPainter = blackPainter,
                        title = { Text("Today") }) {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = WW.current.currentDt
                        val today = cal.day
                        var inx = 0;
                        while (inx < WW.hourlys.size) {
                            val h = WW.hourlys.get(inx)
                            cal.timeInMillis = h.date
                            val day = cal.day
                            if (today != day) break;
                            Row(
                                horizontalArrangement = Arrangement.SpaceAround,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val skip = (cal.hour - 1) % 6
                                for (i in (0..skip - 1)) {
                                    Column(
                                        horizontalAlignment = CenterHorizontally,
                                    ) {
                                        // empty
                                    }
                                }
                                do {
                                    val w = WW.hourlys[inx]
                                    cal.timeInMillis = w.date
                                    val hour = cal.hour
                                    Column(
                                        horizontalAlignment = CenterHorizontally,
                                    ) {
                                        Text(if (hour <= 12) hour.toString() else (hour - 12).toString())
                                        Text(w.temp.toInt().toString() + "\u00b0")
                                        RenderBitmap(w.bitmap)
                                    }
                                    inx++
                                } while ((hour + 1) % 6 != 0)
                            }

                        }
                    }
                }
                item {
                    TitleCard(onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        backgroundPainter = blackPainter,
                        title = { Text("Tomorrow") }) {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = WW.current.currentDt
                        var baseday = cal.day
                        var inx = 0;
                        while (inx < WW.hourlys.size) {
                            val h = WW.hourlys[inx]
                            cal.timeInMillis = h.date
                            if (baseday != cal.day) break;
                            inx++
                        }
                        baseday = cal.day
                        while (inx < WW.hourlys.size) {
                            val h = WW.hourlys.get(inx)
                            cal.timeInMillis = h.date
                            val day = cal.day
                            if (baseday != day) break;
                            Row(
                                horizontalArrangement = Arrangement.SpaceAround,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val skip = (cal.hour - 1) % 6
                                for (i in (0..skip - 1)) {
                                    Column(
                                        horizontalAlignment = CenterHorizontally,
                                    ) {
                                        // empty
                                    }
                                }
                                do {
                                    val w = WW.hourlys[inx]
                                    cal.timeInMillis = w.date
                                    val hour = cal.hour
                                    Column(
                                        horizontalAlignment = CenterHorizontally,
                                    ) {
                                        Text(if (hour <= 12) hour.toString() else (hour - 12).toString())
                                        Text(w.temp.toInt().toString() + "\u00b0")
                                        RenderBitmap(w.bitmap)
                                    }
                                    inx++
                                } while ((hour + 1) % 6 != 0)
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
//                            for (h in r) {
//                                Column(
//                                    horizontalAlignment = CenterHorizontally,
//                                ) {
//                                    Text(h.hour.toString())
//                                    Text("foo")
//                                    val bm = h.bitmap
//                                    if (bm != null) Image(
//                                        bitmap = h.bitmap.asImageBitmap(),
//                                        "",
//                                        alignment = Alignment.Center,
//                                        modifier = Modifier.size(24.dp)
//                                    )
//                                }
//                            }
//                        }
//                        Row(
//                            horizontalArrangement = Arrangement.SpaceAround,
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            for (h in r) {
//                                Column(
//                                    horizontalAlignment = CenterHorizontally,
//                                ) {
//                                    Text(
//                                        text = h.hour.toString(),
//                                        modifier = Modifier
//                                            .padding(0.dp, 0.dp, 0.dp, 0.dp)
//                                            .border(1.dp, Color.White)
//                                    )
//                                    Text("foo", modifier = Modifier.border(1.dp, Color.White))
//                                    val bm = h.bitmap
//                                    if (bm != null) Image(
//                                        bitmap = h.bitmap.asImageBitmap(),
//                                        "",
//                                        alignment = Alignment.Center,
//                                        modifier = Modifier
//                                            .size(24.dp)
//                                            .background(Color.Gray, CircleShape)
//                                    )
//                                }
//                            }
            }
//            item {
//                val r by remember { row1 }
//                LazyRow() {
//                    items(r.size) { i ->
//                        Column(
//                            horizontalAlignment = CenterHorizontally,
//                            modifier = Modifier.padding(4.dp, 0.dp, 4.dp, 0.dp)
//                        ) {
//                            val h = r[i]
//                            Text(
//                                text = h.hour.toString(),
//                            )
//                            Text(text = h.temp.toString(), textAlign = TextAlign.Center)
//                            val bm = h.bitmap
//                            if (bm != null) Image(
//                                bitmap = h.bitmap.asImageBitmap(),
//                                "",
//                                alignment = Alignment.Center,
//                                modifier = Modifier.size(24.dp)
//                            )
//                        }
//                    }
//                }
//            }

//            item {
//                Button(
//                    onClick = { }, modifier = Modifier.fillMaxWidth()
//                ) {
//                    Text(text = stringResource(id = R.string.query_mobile_camera))
//                }
//            }
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
