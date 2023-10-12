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
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.MeasureClient
import androidx.health.services.client.data.*
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import java.util.concurrent.atomic.AtomicInteger
import com.ofd.watch.R

/**
 * This is essentially the same service that is in standalone, however for some
 * reason the callback is getting deactivated, and never reactivated. So let's
 * try in the process of the watchface itself.
 */
class HonestHeartRate2 : SuspendingComplicationDataSourceService() {


    companion object {
        private var svcHeartRateCallback: MeasureCallback? = null
        private var svcMeasureClient: MeasureClient? = null

        private const val TAG = "HonestHR2"

        @Volatile
        var heartRate = 0

        var uctr = AtomicInteger(0)
    }


    /*
     * Called when a complication has been activated. The method is for any one-time
     * (per complication) set-up.
     *
     * You can continue sending data for the active complicationId until onComplicationDeactivated()
     * is called.
     */
    override fun onComplicationActivated(
        complicationInstanceId: Int,
        type: ComplicationType
    ) {
        Log.d(TAG, "onComplicationActivated(): $complicationInstanceId")

        if (svcMeasureClient == null) {
            val component = ComponentName(applicationContext, HonestHeartRate2::class.java)

            val updater = ComplicationDataSourceUpdateRequester.create(
                applicationContext,
                component
            )

            svcHeartRateCallback = object : MeasureCallback {
                override fun onAvailabilityChanged(
                    dataType: DeltaDataType<*, *>,
                    availability: Availability
                ) {
                    if (availability is DataTypeAvailability) {
                    }
                }

                override fun onDataReceived(data: DataPointContainer) {
                    val heartRateBpm = data.getData(DataType.HEART_RATE_BPM)
                    heartRate = heartRateBpm.get(0).value.toInt()
                    updater!!.requestUpdateAll();
//                    Log.d(TAG, "Data: " + heartRateBpm)
                }
            }

            svcMeasureClient = HealthServices.getClient(this /*context*/).measureClient
            svcMeasureClient!!.registerMeasureCallback(
                DataType.HEART_RATE_BPM,
                svcHeartRateCallback as MeasureCallback
            )
            Log.d(TAG, "Callback registered2");
        }

    }

    /*
     * A request for representative preview data for the complication, for use in the editor UI.
     * Preview data is assumed to be static per type. E.g. for a complication that displays the
     * date and time of an event, rather than returning the real time it should return a fixed date
     * and time such as 10:10 Aug 1st.
     *
     * This will be called on a background thread.
     */
    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "6?").build(),
            contentDescription = PlainComplicationText.Builder(text = "Short Text version of Number.")
                .build()
        )
            .setTapAction(null)
            .build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
//        Log.d(TAG, "onComplicationRequest() id: ${request.complicationInstanceId}")

        return when (request.complicationType) {

            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(
                    text = heartRate.toString()
//                    text = heartRate.toString() + ":" + uctr.incrementAndGet().toString()
//                    text = heartRate.toString() + if (uctr.incrementAndGet() % 2 == 0) "" else "."
                ).build(),
                contentDescription = PlainComplicationText.Builder(text = "Short Text version of Number.")
                    .build()
            ).setMonochromaticImage(
                MonochromaticImage.Builder(
                    image = Icon.createWithResource(
                        this,
                        if (uctr.incrementAndGet() % 2 == 0)
                            R.drawable.ic_baseline_monitor_heart_24line_red
                        else
                            R.drawable.ic_baseline_monitor_heart_24line
                    )
                ).build()
            ) .setTapAction(tapAction())
                .build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = heartRate.toFloat(),
                min = 50F,
                max = 130F,
                contentDescription = PlainComplicationText.Builder("Heart Rate").build()
            ).setText(PlainComplicationText.Builder(heartRate.toString()).build())
                .setMonochromaticImage(
                MonochromaticImage.Builder(
                    image = Icon.createWithResource(
                        this,
                        if (uctr.incrementAndGet() % 2 == 0)
                            R.drawable.ic_baseline_monitor_heart_24
                        else
                            R.drawable.ic_baseline_monitor_heart_24line
                    )
                ).build()
            ).setTapAction(tapAction())
                .build()

            else -> {
                Log.w(TAG, "Unexpected complication type ${request.complicationType}")
                null
            }
        }
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        Log.d(TAG, "Deactivated")
        if (svcMeasureClient != null) {
            svcMeasureClient!!.unregisterMeasureCallbackAsync(
                DataType.HEART_RATE_BPM,
                svcHeartRateCallback as MeasureCallback
            )
            Log.d(TAG, "Callback unregistered")
        }
        svcHeartRateCallback = null
        svcMeasureClient = null
    }

    override fun onCreate() {
        super.onCreate()
//        Log.d(TAG, "onCreate()")
    }

    override fun onDestroy() {
        super.onDestroy()
//        Log.d(TAG, "onDestroy()")
    }

    //com.samsung.android.wear.shealth/com.samsung.android.wear.shealth.tile.heartrate.HeartRateTileProviderService
    fun Context.tapAction(): PendingIntent? {
//        val intent = Intent()
//        intent.setClassName("com.samsung.android.wear.shealth","com.samsung.android.wear.shealth.tile.heartrate.HeartRateTileProviderService")
//            Intent(Intent.ACTION_VIEW)
//            .setData(Uri.Builder().scheme("content").path("com.samsung.android.app.calendar.view.daily.DailyActivity").build()) //builder.build())
//            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val intent = applicationContext.packageManager.getLaunchIntentForPackage("com.samsung.android.wear.shealth")
//        intent?.setClassName("com.samsung.android.wear.shealth","com.samsung.android.wear.shealth.tile.heartrate.HeartRateTileProviderService")
//        Log.e(TAG, "Intent: " + intent)
        return PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

}
