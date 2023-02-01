package com.ofd.openweather

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.ofd.watch.R
import com.ofd.watchface.location.ResolvedLocation
import com.thanglequoc.aqicalculator.AQICalculator
import com.thanglequoc.aqicalculator.Pollutant
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


private const val TAG = "OpenWeatherAQIService"

private const val OPENWEATHER_AIR_QUALITY = "http://api.openweathermap.org/data/2.5/air_pollution?"

private val metricNumCalls = AtomicInteger()
private val metricNumSuccess = AtomicInteger()
private val metricNumErrors = AtomicInteger()
private val metricNumConflict = AtomicInteger()
private val metricNumBypass = AtomicInteger()


private val calculator: AQICalculator = AQICalculator.getAQICalculatorInstance()

sealed class AQIResult() {
    class AQI(val rlocation: ResolvedLocation, val fulljsonn: String, val date: Long, val color: Int, val comps: Map<String, Float>) :
        AQIResult() {

        public val address: String? = null

        public val aqi = if (comps.containsKey("pm2_5")) calculator.getAQI(
            Pollutant.PM25, comps.get("pm2_5")!!.toDouble()
        ).aqi.toString() else "??"

        fun colorInxForComp(comp: String, v: Float): Int {
            fun inx(f0: Float, f1: Float, f2: Float, f3: Float, f4: Float = f3): Int {
                return if (v <= f0) 0 else if (v <= f1) 1 else if (v <= f2) 2 else if (v <= f3) 3 else if (v <= f4) 4 else 5
            }
            return when (comp) {
                "co" -> {
                    inx(1f, 2f, 10f, 17f, 34f)
                }
                "no" -> {
                    0
                }
                "no2" -> {
                    inx(50f, 100f, 200f, 400f)
                }
                "o3" -> {
                    inx(60f, 120f, 180f, 240f)
                }
                "so2" -> {
                    inx(40f, 80f, 380f, 800f, 1600f)
                }
                "pm2_5" -> {
                    inx(15f, 30f, 55f, 110f)
                }
                "pm10" -> {
                    inx(25f, 50f, 90f, 180f)
                }
                "nh3" -> {
                    inx(200f, 400f, 800f, 1200f, 1800f)
                }
                else -> 5
            }
        }
        fun statusString() =
            "" + metricNumCalls.get() + "/" + (metricNumSuccess.get() + metricNumBypass.get()) + ":" + metricNumBypass.get() + ":" + metricNumErrors.get() + ":" + metricNumConflict.get()
    }

    class Error(val msg: String) : AQIResult()
}

private val valueRetentionMs = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)

private val callInProgress = AtomicBoolean(false)

private var lastQueryTimeMs = 0L
private var lastQueryData: AQIResult = AQIResult.Error("no data yet")


private var appid: String? = null

suspend fun getAQI(context: Context, location: ResolvedLocation): AQIResult {
    if (appid == null) appid = context.getString(R.string.openweather_appid)
    metricNumCalls.incrementAndGet()
    Log.d(TAG, "Getting AQI: " + location.toString())

    val now = System.currentTimeMillis()
    if (now - lastQueryTimeMs < valueRetentionMs) {
        Log.d(TAG, "Bypassing AQI")
        metricNumBypass.incrementAndGet()
        return lastQueryData
    }

    if (!callInProgress.compareAndSet(false, true)) {
        Log.e(TAG, "Call in progress")
        metricNumConflict.incrementAndGet()
        return lastQueryData
    }

    return withContext(Dispatchers.IO) {
        getAQIInternal(location).apply {
            callInProgress.set(false)
            lastQueryData = this
            lastQueryTimeMs = now
        }
    }
}

private suspend fun getAQIInternal(resolvedLocation: ResolvedLocation): AQIResult {
    Log.d(TAG, "getAQI $resolvedLocation")

    val location = resolvedLocation.location!!
    val url = URL(
        OPENWEATHER_AIR_QUALITY + "lat=${location.latitude}&lon=${location.longitude}&appid=$appid"
    )
    val reader = InputStreamReader(url.openConnection().getInputStream())
    val fulljsonn = reader.readText()
    reader.close()
    val top = JsonParser.parseString(fulljsonn).asJsonObject
    if (false) {
        Log.d(TAG, "JSON: $fulljsonn")
    } else if (false) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        Log.d(TAG, "JSON: " + gson.toJson(top))
    }

    val lst = top.getAsJsonArray("list")[0].asJsonObject
    val aqi = lst.getAsJsonObject("main").getAsJsonPrimitive("aqi").asInt

    val comps = lst.getAsJsonObject("components")
    val cmap = mutableMapOf<String, Float>()
    for ((comp, value) in comps.entrySet()) {
        cmap[comp] = value.asFloat
    }

    val date = lst.getAsJsonPrimitive("dt").asLong * 1000

    return AQIResult.AQI(resolvedLocation, fulljsonn, date, aqi, cmap)
}
