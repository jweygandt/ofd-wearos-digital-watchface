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
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.google.gson.JsonParser
import com.ofd.apis.APIActivity
import com.ofd.apis.FakeLocation
import com.ofd.apis.WeatherResult
import com.ofd.watch.R
import com.ofd.watchface.location.ResolvedLocation
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.runBlocking


class OpenWeatherActivity : APIActivity<WeatherResult>(openWeatherAPI) {

    @Composable
    override fun doContent(data: MutableState<WeatherResult?>) {
        doStaticContent(true, data)
    }

    companion object {
        private val Calendar.hour
            get() = get(Calendar.HOUR_OF_DAY)

        private val Calendar.day
            get() = get(Calendar.DAY_OF_YEAR)

        private val Calendar.date
            get() = get(Calendar.DAY_OF_MONTH)

        val LineLeft: Shape = object : Shape {
            override fun createOutline(
                size: Size, layoutDirection: LayoutDirection, density: Density
            ): Outline {
                val p = Path()
                val r = size.toRect()
                val width = .05f
                p.moveTo(r.left + width, r.top)
                p.lineTo(r.left, r.top)
                p.lineTo(r.left, r.bottom)
                p.lineTo(r.left + width, r.bottom)
                return Outline.Generic(p)
            }

            override fun toString(): String = "PathShape"
        }

        val BoxLeft: Shape = object : Shape {
            override fun createOutline(
                size: Size, layoutDirection: LayoutDirection, density: Density
            ): Outline {
                val p = Path()
                val r = size.toRect()
                p.moveTo(r.right, r.top)
                p.lineTo(r.left, r.top)
                p.lineTo(r.left, r.bottom)
                p.lineTo(r.right, r.bottom)
                return Outline.Generic(p)
            }

            override fun toString(): String = "PathShape"
        }

        val BoxRight: Shape = object : Shape {
            override fun createOutline(
                size: Size, layoutDirection: LayoutDirection, density: Density
            ): Outline {
                val p = Path()
                val r = size.toRect()
                p.moveTo(r.left, r.top)
                p.lineTo(r.right, r.top)
                p.lineTo(r.right, r.bottom)
                p.lineTo(r.left, r.bottom)
                return Outline.Generic(p)
            }

            override fun toString(): String = "PathShape"
        }

        @Composable
        fun doStaticContent(live: Boolean, data: MutableState<WeatherResult?>) {
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
                    fun itemy(content: @Composable () -> Unit): Unit {}

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
                            item() {
                                Text(
                                    text = WW.address,
                                    modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 10.dp),
                                    fontSize = 20.sp
                                )
                            }
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
                                    RenderBitmap(live, WW.current.currentBitmap)
                                }
                            }
                        }
                        fun needToBreak(today: Calendar, data: Calendar): Boolean {
                            return if (today.day == data.day) false
                            else !(today.day + 1 == data.day && data.hour == 0)
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
                                var inx = 0
                                val caldata = Calendar.getInstance()
                                var row = 0
                                while (inx < WW.hourlys.size) {
                                    val h = WW.hourlys.get(inx)
                                    caldata.timeInMillis = h.date
                                    val day = caldata.day
                                    if (needToBreak(caltoday, caldata)) break
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                0.dp, (if (row != 0) 7.dp else 0.dp), 0.dp, 0.dp
                                            )
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
                                                    RenderBitmap(live, w.bitmap, 18)
                                                }
                                                Text(w.temp.toInt().toString() + "\u00b0")
                                                Text((w.pop * 100).toInt().toString() + "%")
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
                                var inx = 0
                                var row = 0
                                while (inx < WW.hourlys.size) {
                                    val h = WW.hourlys[inx]
                                    caldate.timeInMillis = h.date
                                    if (needToBreak(caltoday, caldate)) break
                                    inx++
                                }
                                val tomorrowday = caldate.day
                                while (inx < WW.hourlys.size) {
                                    val h = WW.hourlys.get(inx)
                                    caldate.timeInMillis = h.date
                                    val day = caldate.day
                                    if (tomorrowday != day) break
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                0.dp, (if (row != 0) 7.dp else 0.dp), 0.dp, 0.dp
                                            )
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
                                                    RenderBitmap(live, w.bitmap, 18)
                                                }
                                                Text(w.temp.toInt().toString() + "\u00b0")
                                                Text((w.pop * 100).toInt().toString() + "%")
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
                                var inx = 0
                                val rowsize = 3
                                while (inx < WW.dailys.size) {
                                    val d = WW.dailys.get(inx)
                                    cal.timeInMillis = d.date
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {

                                        do {
                                            val w = WW.dailys[inx]
                                            cal.timeInMillis = w.date
                                            val date = cal.date
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                            ) {
                                                Text(date.toString())
                                                Text(w.tempMax.toInt().toString() + "\u00b0")
                                                Text(w.tempMin.toInt().toString() + "\u00b0")
                                            }
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                            ) {
                                                RenderBitmap(live, w.bitmap)
                                                Text((w.pop * 100).toInt().toString() + "%")
                                            }
                                            if (true || inx % rowsize != 0) {
                                                Column(
                                                    Modifier
                                                        .width(2.dp)
                                                        .background(Color.White)
                                                        .fillMaxWidth()
                                                ) {}
                                            }
                                            inx++
                                        } while (inx % rowsize != 0 && inx < WW.dailys.size)
                                        while (inx % rowsize != 0) {
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
        private fun RenderBitmap(live: Boolean, bm: Bitmap?, size: Int = 24) {
            if (bm != null) Image(
                bm.asImageBitmap(),
                "",
                alignment = Alignment.Center,
                modifier = Modifier.size(size.dp)
//            .background(Color.Gray, CircleShape)
            ) else if (!live) {
                val bm =
                    androidx.compose.ui.graphics.ImageBitmap.Companion.imageResource(R.drawable.default_openweather_image)
                Image(bitmap = bm, contentDescription = "fake", modifier = Modifier.size(size.dp))
            }
        }
    }
}


@Preview(device = Devices.WEAR_OS_LARGE_ROUND, showSystemUi = true)
@Composable
fun MainAppPreviewEvents() {
    val data = mutableStateOf<WeatherResult?>(WeatherResult.Error("ABC", "FOOBAR"))
    runBlocking {
        val f = File("/TEMP/openweather.txt")
        val fulljson = f.readText()
        val jsonobj = JsonParser.parseString(fulljson).asJsonObject
        data.value = openWeatherAPI.makeResult(FakeLocation(), fulljson, jsonobj)
    }
    val x = stringResource(id = R.string.openweather_appid)
    OpenWeatherActivity.doStaticContent(false, data)
}


//@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
//@Composable
//fun MainAppPreviewEmpty() {
//
//}
