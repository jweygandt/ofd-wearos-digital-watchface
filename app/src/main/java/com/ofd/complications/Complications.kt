package com.ofd.complications

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

class Complications {

    companion object {

        var TAG = "Complications"

        fun forceComplicationUpdate(
            applicationContext: Context, complicationSlotsManager: ComplicationSlotsManager
        ) {
            complicationSlotsManager.complicationSlots.forEach { cs ->
                val dataSource = cs.value.complicationData.value.dataSource
                if (dataSource != null) {
                    Log.d(
                        TAG, "Complication: " + cs.key + ": " + dataSource.toString()
                    )
                    if (dataSource.packageName.equals("com.ofd.digital.alpha")) {
                        Log.d(TAG, "updating " + dataSource.toString())
                        ComplicationDataSourceUpdateRequester.create(
                            applicationContext, dataSource
                        ).requestUpdateAll()
                    }
                }
            }
//            Log.d(TAG, "updating SunriseSunset")
//            ComplicationDataSourceUpdateRequester.create(
//                applicationContext, ComponentName(
//                    applicationContext, SunriseSunset::class.java
//                )
//            ).requestUpdateAll()
//
//            Log.d(TAG, "updating LocationTest")
//            ComplicationDataSourceUpdateRequester.create(
//                applicationContext, ComponentName(
//                    applicationContext, LocationTest::class.java
//                )
//            ).requestUpdateAll()
//
//            Log.d(TAG, "updating AirQuality")
//            ComplicationDataSourceUpdateRequester.create(
//                applicationContext, ComponentName(
//                    applicationContext, AirQuality::class.java
//                )
//            ).requestUpdateAll()
        }


    }

}
