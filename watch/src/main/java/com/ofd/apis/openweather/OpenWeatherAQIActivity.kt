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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.ofd.apis.APIActivity
import com.ofd.apis.AQIResult
import java.text.SimpleDateFormat


class OpenWeatherAQIActivity : APIActivity<AQIResult<OpenWeatherAQIService.OpenWeatherAQIDetails>>(OpenWeatherAQIService) {


    @Composable
    override fun doContent(data: MutableState<AQIResult<OpenWeatherAQIService.OpenWeatherAQIDetails>?>) {
        val scalingLazyListState = rememberScalingLazyListState(0, 0)

        val lazyRowState = rememberLazyListState()

        val sdffull = SimpleDateFormat("MM/dd HH:mm")

        val blackPainter =
            CardDefaults.cardBackgroundPainter(Color.Black, Color.Black, LayoutDirection.Ltr)

        val colors = listOf<Color>(
            Color.Green,
            Color.Yellow,
            Color(0xFFFFA500) /*orange*/,
            Color.Red,
            Color(0xFFA020F0) /*purple*/,
            Color(0xFF800000) /*maroon*/
        )
        val textcolors = listOf<Color>(
            Color.Black,
            Color.Black,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
        )

        Scaffold(vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
            positionIndicator = { PositionIndicator(scalingLazyListState = scalingLazyListState) },
            modifier = Modifier.background(Color.DarkGray),
            timeText = { TimeText() }) {

            var W by remember { data }

            ScalingLazyColumn(
                state = scalingLazyListState, contentPadding = PaddingValues(
                    horizontal = 8.dp, vertical = 32.dp
                ), anchorType = ScalingLazyListAnchorType.ItemStart
            ) {
                val WW = W
                if (WW == null) {
                    item { Text("Loading...") }
                } else if (WW is AQIResult.Error) {
                    item {
                        Text(
                            WW.source + " " + WW.msg,
                            modifier = Modifier.padding(10.dp, 50.dp, 0.dp, 0.dp)
                        )
                    }
                } else if (WW is AQIResult.AQI) {
                    item { Text(WW.details.address ?: "not set") }
                    item {
                        TitleCard(onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            title = { Text(sdffull.format(WW.details.date)) }) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("aqi: ")
                                Text(WW.details.aqistr + "ppm")
                            }
                            for ((comp, v) in WW.details.comps) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("${comp}: ")
                                    Text(
                                        v.toString(), color = textcolors[AQIResult.AQI.colorInxForComp(
                                            comp, v
                                        )], modifier = Modifier.background(
                                            colors[AQIResult.AQI.colorInxForComp(
                                                comp, v
                                            )]
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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
