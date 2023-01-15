package com.odbol.wear.airquality.purpleair

import android.location.Location
import android.util.Log
import com.thanglequoc.aqicalculator.AQICalculator
import com.thanglequoc.aqicalculator.AQIResult
import com.thanglequoc.aqicalculator.Pollutant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.math.min
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AirQualitySearch {

    // Searching for a set of sensors to "median value" the AQI
    //
    // We will cache some values based on location. One must move a minimum of
    // minLocationMovementMiles to invalidate the cache
    //
    // The search will start out at initialFenceMiles and attempt to get a minimum of
    // minimumSensors. Should that fail we increment the fenceMiles by incrementalScale
    // to a maximumFenceMiles and do one more call.
    //
    // If we reach minimumSensors or maximumFenceMiles and have at least one sensor we
    // will do the "median value" for the results, else no results.
    //
    // The fenceMiles value will be cached based on location. It can increase any time needed,
    // but will not decrease till one moves beyond minLocationMovementMiles.
    //
    // The computed value will be kept for valueRetentionMs or if one moves beyond
    // minLocationMovementMiles, however the maximum frequence of the calls will be no more
    // than minPeriodMs

    companion object {
        const val TAG = "AirQualitySearch"
        private const val initialFenceMiles = 1.0f
        private const val incrementalScale = 1.4f
        private const val maximumFenceMiles = 50f
        private const val minimumSensors = 10
        private const val maximumSensors = 30
        private const val minLocationMovementMiles = 1.0f
        val valueRetentionMs = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)

        private const val metersInMile = 1609.344

        val callInProgress = AtomicBoolean(false)

        val metricNumCalls = AtomicInteger()
        val metricNumSuccess = AtomicInteger()
        val metricLastResultSize = AtomicInteger()
        val metricNumErrors = AtomicInteger()
        val metricNumConflict = AtomicInteger()
        val metricNumBypass = AtomicInteger()

        var lastLocation: Location? = null
        var currentFenceMiles = initialFenceMiles

        var lastQueryTimeMs = 0L
        var lastQueryData: AirQualityData = AirQualityData(emptyList())

        fun statusString() =
            metricNumCalls.get().toString() + "/" + (metricNumSuccess.get() + metricNumBypass.get()) +
                "(" + metricLastResultSize.get() + ")" + ":" + metricNumBypass.get() + ":" +
                metricNumErrors.get() + ":" + metricNumConflict.get()

        class AirQualityData(sensors: List<Sensor>) {

            private val pm25: Double
            val hasRealData: Boolean
            private val sortedSensors: List<Sensor>

            init {
                if (sensors.isNotEmpty()) {
                    metricLastResultSize.set(sensors.size)
                    val s0 = sensors.sortedWith(sortByClosest(lastLocation!!))
                    sortedSensors = if (s0.size > maximumSensors) s0.subList(0, maximumSensors - 1)
                    else s0
                    val pm25s: MutableList<Double> = mutableListOf()
                    sortedSensors.forEach { s -> pm25s.add(s.pm25) }
                    pm25s.sort()
                    val inx = (pm25s.size / 2f + .5f).toInt()
                    pm25 = pm25s[min(inx, pm25s.size - 1)]
                    hasRealData = true

                } else {
                    metricLastResultSize.set(sensors.size)
                    pm25 = 0.0
                    hasRealData = false
                    sortedSensors = sensors
                }
            }

            fun aqi() = convertPm25ToAqi(this.pm25).aqi.toFloat()

            private fun convertPm25ToAqi(pm25: Double): AQIResult {
                val calculator: AQICalculator = AQICalculator.getAQICalculatorInstance()
                return calculator.getAQI(Pollutant.PM25, pm25)
            }

        }
    }

    fun getAirQualitityData(
        purple: PurpleAir,
        location: Location,
        callback: Consumer<AirQualityData>
    ) {
        val now = System.currentTimeMillis()
        metricNumCalls.incrementAndGet()

        // If we have a good value, then keep it for a while
        if (now - lastQueryTimeMs < valueRetentionMs && lastQueryData.hasRealData) {
            metricNumBypass.incrementAndGet()
            callback.accept(lastQueryData)
            return
        }

        // Just in case calls come in fast
        if (!callInProgress.compareAndSet(false, true)) {
            Log.d(TAG, "Call in progress")
            metricNumConflict.incrementAndGet()
            callback.accept(lastQueryData)
            return
        }

        // If we have moved more than minLocationMovementMiles
        // reset the fence
        if ((lastLocation == null) || (abs(location.distanceTo(lastLocation!!) / metersInMile) > minLocationMovementMiles)) {
            currentFenceMiles = initialFenceMiles
            lastLocation = location
        }

        getAirQualitityDataInternal(
            now,
            purple,
            location,
        ) { aqd ->
            callInProgress.set(false)
            callback.accept(aqd)
        }
    }

    private fun getAirQualitityDataInternal(
        now: Long, purple: PurpleAir, location: Location, callback: Consumer<AirQualityData>
    ) {
        purple.getAllSensorsRetrofit(location, currentFenceMiles)
            .enqueue(object : Callback<SensorsResult?> {
                override fun onResponse(
                    call: Call<SensorsResult?>, response: Response<SensorsResult?>
                ) {
                    val body = response.body()
                    if (body != null) {
                        val sensors = body.sensors.filter { s -> s.location_type.equals("outside") }
                        if (sensors.size >= minimumSensors) {
                            lastQueryTimeMs = now
                            lastQueryData = AirQualityData(sensors)
                            metricNumSuccess.incrementAndGet()
                            callback.accept(lastQueryData)
                            return
                        } else {
                            currentFenceMiles *= incrementalScale
                            if (currentFenceMiles < maximumFenceMiles) {
                                getAirQualitityDataInternal(
                                    now, purple, location, callback
                                )
                                return
                            } else {
                                lastQueryTimeMs = now
                                lastQueryData = AirQualityData(sensors)
                                metricNumSuccess.incrementAndGet()
                                callback.accept(lastQueryData)
                                return
                            }
                        }
                    } else {
                        Log.w(TAG, "Issue with purple air, empty body: $response")
                        lastQueryTimeMs = now
                        lastQueryData = AirQualityData(emptyList())
                        metricNumErrors.incrementAndGet()
                        callback.accept(lastQueryData)
                        return
                    }
                }

                override fun onFailure(call: Call<SensorsResult?>, t: Throwable) {
                    Log.w(TAG, "Issue with purple air: " + t.message, t)
                    lastQueryTimeMs = now
                    metricNumErrors.incrementAndGet()
                    lastQueryData = AirQualityData(emptyList())
                    callback.accept(lastQueryData)
                }

            })
    }


}
