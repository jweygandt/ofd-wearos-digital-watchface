package com.ofd.complications

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

class Complications {

    companion object{

        var TAG = "Complications"

        fun forceComplicationUpdate(applicationContext: Context) {
            Log.d(TAG, "updating SunriseSunset")
            ComplicationDataSourceUpdateRequester.create(
                applicationContext, ComponentName(
                    applicationContext, SunriseSunset::class.java
                )
            ).requestUpdateAll()

            Log.d(TAG, "updating LocationTest")
            ComplicationDataSourceUpdateRequester.create(
                applicationContext, ComponentName(
                    applicationContext, LocationTest::class.java
                )
            ).requestUpdateAll()
        }


    }

}
