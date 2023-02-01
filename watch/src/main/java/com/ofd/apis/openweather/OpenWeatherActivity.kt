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
package com.ofd.apis.openweather

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.ofd.apis.APIActivity
import com.ofd.apis.WeatherResult
import com.ofd.watchface.location.ResolvedLocation
import java.text.SimpleDateFormat
import java.util.*


class OpenWeatherActivity : APIActivity<WeatherResult>() {

    override fun makeErrorResult(s: String): WeatherResult {
        return WeatherResult.Error(TAG, s)
    }

    override suspend fun getData(context: Context, location: ResolvedLocation): WeatherResult {
        return openWeather(context, location)
    }

    private val Calendar.hour
        get() = get(Calendar.HOUR_OF_DAY)

    private val Calendar.day
        get() = get(Calendar.DAY_OF_YEAR)

    private val Calendar.date
        get() = get(Calendar.DAY_OF_MONTH)

    @Composable
    override fun doContent() {
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

            var W by remember { data }

            // It seems a static column is better than lazy
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(0.dp, 50.dp, 0.dp, 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                @Composable
                fun item(content: @Composable () -> Unit): Unit = content.invoke()

//        ScalingLazyColumn(
//            state = scalingLazyListState, contentPadding = PaddingValues(
//                horizontal = 8.dp, vertical = 32.dp
//            ), anchorType = ScalingLazyListAnchorType.ItemStart
//        ) {

                val WW = W
                if (WW == null) {
                    item {
                        Text(
                            "Loading...", modifier = Modifier.padding(10.dp, 50.dp, 0.dp, 0.dp)
                        )
                    }
                } else if (WW is WeatherResult.Error) {
                    item {
                        Text(
                            WW.source + " " + WW.msg,
                            modifier = Modifier.padding(10.dp, 50.dp, 0.dp, 0.dp)
                        )
                    }
                } else if (WW is WeatherResult.Weather) {
                    if (WW.address != null) item {
                        Text(
                            text = (WW.address ?: "not set"),
                            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 10.dp),
                            fontSize = 20.sp
                        )
                    }
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
                                Text(WW.current.descr)
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
                            var row = 0
                            while (inx < WW.hourlys.size) {
                                val h = WW.hourlys.get(inx)
                                caldata.timeInMillis = h.date
                                val day = caldata.day
                                if (needToBreak(caltoday, caldata)) break;
                                Row(
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(0.dp, (if (row != 0) 7.dp else 0.dp), 0.dp, 0.dp)
                                ) {
                                    val skip = dataCol0Based(caldata.hour)
                                    for (i in (0..skip - 1)) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            // empty
                                        }
                                    }
                                    do {
                                        val w = WW.hourlys[inx]
                                        caldata.timeInMillis = w.date
                                        val hour = caldata.hour
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
//                                        Text(stringForHour(hour))
//                                        Text(w.temp.toInt().toString() + "\u00b0")
//                                        RenderBitmap(w.bitmap)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(stringForHour(hour), fontSize = 12.sp)
                                                RenderBitmap(w.bitmap, 18)
                                            }
                                            Text(w.temp.toInt().toString() + "\u00b0")
                                            Text((w.pop*100).toInt().toString() + "%")
                                        }
                                        inx++
                                    } while (dataCol0Based(hour + 1) != 0)
                                    row++
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
                            var row = 0;
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(0.dp, (if (row != 0) 7.dp else 0.dp), 0.dp, 0.dp)
                                ) {
                                    val skip = dataCol0Based(caldate.hour)
                                    for (i in (0..skip - 1)) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            // empty
                                        }
                                    }
                                    do {
                                        val w = WW.hourlys[inx]
                                        caldate.timeInMillis = w.date
                                        val hour = caldate.hour
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
//                                        Text(stringForHour(hour))
//                                        Text(w.temp.toInt().toString() + "\u00b0")
//                                        RenderBitmap(w.bitmap)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(stringForHour(hour), fontSize = 12.sp)
                                                RenderBitmap(w.bitmap, 18)
                                            }
                                            Text(w.temp.toInt().toString() + "\u00b0")
                                            Text((w.pop*100).toInt().toString() + "%")
                                        }
                                        inx++
                                    } while (dataCol0Based(hour + 1) != 0)
                                    row++
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
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(date.toString())
                                            Text(w.tempMax.toInt().toString() + "\u00b0")
                                            Text(w.tempMin.toInt().toString() + "\u00b0")
                                            Text((w.pop*100).toInt().toString() + "%")
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
    private fun RenderBitmap(bm: Bitmap?, size: Int = 24) {
        if (bm != null) Image(
            bm.asImageBitmap(), "", alignment = Alignment.Center, modifier = Modifier.size(size.dp)
//            .background(Color.Gray, CircleShape)
        )
    }
}
//
//
//@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
//@Composable
//fun MainAppPreviewEvents() {
//
//}
//
//@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
//@Composable
//fun MainAppPreviewEmpty() {
//    OpenWeatherMainApp(
//    )
//}
