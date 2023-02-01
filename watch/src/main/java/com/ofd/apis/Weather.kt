package com.ofd.apis

import android.graphics.Bitmap
import android.graphics.drawable.Icon
import java.util.concurrent.atomic.AtomicInteger

sealed class WeatherResult {
    data class Weather(
        val source: String,
        val metrics: APIMetrics,
        val fulljson: String,
        val lat: Double,
        val lon: Double,
        val address: String?,
        val current: Current,
        val hourlys: MutableList<Hourly>,
        val dailys: MutableList<Daily>,
    ) : WeatherResult() {
        data class Daily(
            val date: Long,
            val tempDay: Double,
            val tempMin: Double,
            val tempMax: Double,
            val iconStr: String,
            val icon: Icon?,
            val bitmap: Bitmap?,
            val pop: Double
        )

        data class Hourly(
            val date: Long,
            val temp: Double,
            val iconStr: String,
            val icon: Icon?,
            val bitmap: Bitmap?,
            val pop: Double
        )

        data class Current(
            val currentDt: Long,
            val currentTemp: Double,
            val descr: String,
            val currentIconStr: String,
            val currentIcon: Icon?,
            val currentBitmap: Bitmap?
        )

        fun statusString() =
            source + " " + metrics.metricNumCalls.get() + "/" + (metrics.metricNumSuccess.get() + metrics.metricNumBypass.get()) + ":" + metrics.metricNumBypass.get() + ":" + metrics.metricNumErrors.get() + ":" + metrics.metricNumConflict.get()
    }

    data class Error(val source: String, val msg: String) : WeatherResult()
}
