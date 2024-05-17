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
package com.ofd.apis.sunrisesunset

import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.PersistableBundle
import android.text.Layout
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import com.ofd.watch.R
import com.ofd.watchface.location.WatchLocationService
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment

class SunriseSunsetActivity : ComponentActivity() {

//    enum class SSTYPE { ASTRONOMICAL, NAUTICAL, CIVIL, TOTAL, MOON }
//    enum class SSDIR { UP, DOWN }

    val iconsUp = mutableListOf<Int>()
    val iconsDown = mutableListOf<Int>()
    init{
        iconsUp.add(R.drawable.ic_sunrise_astronomical)
        iconsUp.add(R.drawable.ic_sunrise_nautical)
        iconsUp.add(R.drawable.ic_sunrise_civil)
        iconsUp.add(R.drawable.ic_sunrise_total)
        iconsUp.add(R.drawable.ic_moon)
        iconsDown.add(R.drawable.ic_sunset_astronomical)
        iconsDown.add(R.drawable.ic_sunset_nautical)
        iconsDown.add(R.drawable.ic_sunset_civil)
        iconsDown.add(R.drawable.ic_sunset_total)
        iconsDown.add(R.drawable.ic_moon)
    }
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
    }

    val address = mutableStateOf<String>("not set")

    override fun onResume() {
        super.onResume()

        val loc = WatchLocationService.getLocation()

        lifecycleScope.launch {
            address.value = loc.location.getShortAddress()
        }

        val data = if (loc.valid) compute(
            ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS),
            loc.latitude,
            loc.longitude
        ) else null

        setContent { doContent(data, address) }
    }

    override fun onPause() {
        super.onPause()
    }

    @Composable
    fun doContent(
        data: List<SSData>?,
        address: MutableState<String>
    ) {
        val scalingLazyListState = rememberScalingLazyListState(0, 0)
        val lazyRowState = rememberLazyListState()

        val sdffull = SimpleDateFormat("hh:mmaa")

        Scaffold(vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
            positionIndicator = { PositionIndicator(scalingLazyListState = scalingLazyListState) },
            modifier = Modifier
//                .border(5.dp, Color.White)
                .background(Color.DarkGray)
                .fillMaxWidth(),
            timeText = { TimeText() }) {

            var A by remember { address }
            val AA = A
            var WW = data

            if (WW == null) {
                Text("No Location")
            } else {
                ScalingLazyColumn(
                    state = scalingLazyListState,
                    contentPadding = PaddingValues(
                        horizontal = 8.dp, vertical = 32.dp
                    ), anchorType = ScalingLazyListAnchorType.ItemCenter,
                    modifier = Modifier
//                        .border(5.dp, Color.Green)
                ) {
                    item { Text(AA) }
                    item {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
//                                .border(5.dp, Color.Red)
                        ) {

                            Column(
                                modifier = Modifier
                                    .weight(.5f)
                                    .padding(horizontal = 5.dp)
//                                    .border(5.dp, Color.Green)
//                                    .fillMaxWidth(.5f)
                                , horizontalAlignment = Alignment.End
                            ) {
                                WW.forEach {
                                    val d = if (it.time == null) "----" else sdffull.format(
                                        it.time.toInstant().toEpochMilli()
                                    )
                                    val l = it.type.ordinal
                                    Log.d("DATA", it.toString())
                                    if (it.dir == SSDIR.UP) {
                                        Text(
                                            d, softWrap = false,
                                            textAlign = TextAlign.Right
                                        )
//                                    Text(l)
                                    } else {
                                        Image(painterResource(id = iconsUp.get(l)),null)
//                                        Text(l, softWrap = false,
//                                        textAlign = TextAlign.Right)
//                                    Text(d)
                                    }
                                }
                            }


                            Column(
                                modifier = Modifier
                                    .weight(.5f)
                                    .padding(horizontal = 5.dp)
//                                    .border(5.dp, Color.Yellow)
//                                    .fillMaxWidth(.5f)
                                , horizontalAlignment = Alignment.Start
                            ) {
                                WW.forEach {
                                    val d = if (it.time == null) "----" else sdffull.format(
                                        it.time.toInstant().toEpochMilli()
                                    )
                                    val l = it.type.ordinal
                                    Log.d("DATA", it.toString())
                                    if (it.dir == SSDIR.UP) {
                                        Image(painterResource(id = iconsDown.get(l)),null)
//                                        Text(d)
//                                        Text(l, softWrap = false)
                                    } else {
//                                        Text(l)
                                        Text(d, softWrap = false)
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    val testaddress = mutableStateOf<String>("Los Gatos Test")

    @Preview(device = Devices.DEFAULT, showSystemUi = true)
    @Composable
    fun PreviewSunsetSunrise() {
        //    val dateTime = ZonedDateTime.now(ZoneId.of("CET")).truncatedTo(ChronoUnit.DAYS)
        val dateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
        println("now=" + dateTime)
        println()
//    val lat = 69.65 //37.22
//    val lng = 18.95 //-121.967
        val lat = 37.22
        val lng = -121.967
        val data = compute(dateTime, lat, lng)

        doContent(data, testaddress)
    }
}

//
//@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
//@Composable
//fun AQIMainAppPreviewEvents() {
//    OpenWeatherAQIMainApp(
//    )
//}
//
//@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
//@Composable
//fun AQIMainAppPreviewEmpty() {
//    OpenWeatherAQIMainApp(
//    )
//}
