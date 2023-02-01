package com.ofd.openweather

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.location.Address
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.ofd.watch.R
import com.ofd.watchface.location.ResolvedLocation
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


private const val TAG = "OpenWeatherService3"
private const val OPENWEATHER3 = "https://api.openweathermap.org/data/3.0/onecall?"

private val metricNumCalls = AtomicInteger()
private val metricNumSuccess = AtomicInteger()
private val metricNumErrors = AtomicInteger()
private val metricNumConflict = AtomicInteger()
private val metricNumBypass = AtomicInteger()

sealed class WeatherResult {
    data class Weather(
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
            val currentIconStr: String,
            val currentIcon: Icon?,
            val currentBitmap: Bitmap?
        )
        fun statusString() =
            "" + metricNumCalls.get() + "/" + (metricNumSuccess.get() + metricNumBypass.get()) + ":" + metricNumBypass.get() + ":" + metricNumErrors.get() + ":" + metricNumConflict.get()
    }

    data class Error(val msg: String) : WeatherResult()
}

private val valueRetentionMs = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)

private val callInProgress = AtomicBoolean(false)

private var lastQueryTimeMs = 0L
private var lastQueryData: WeatherResult = WeatherResult.Error("no query")


private var appid: String? = null

suspend fun getWeather(context: Context, location: ResolvedLocation): WeatherResult {
    if (appid == null) appid = context.getString(R.string.openweather_appid)
    metricNumCalls.incrementAndGet()
    Log.d(TAG, "Getting : " + location.toString())

    val now = System.currentTimeMillis()
    if (now - lastQueryTimeMs < valueRetentionMs) {
        Log.d(TAG, "Bypassing ")
        metricNumBypass.incrementAndGet()
        return lastQueryData
    }

    if (!callInProgress.compareAndSet(false, true)) {
        Log.e(TAG, "Call in progress")
        metricNumConflict.incrementAndGet()
        return lastQueryData
    }

    return withContext(Dispatchers.IO) {
        getInternal(location).also {
            callInProgress.set(false)
            lastQueryData = it
            lastQueryTimeMs = now
        }
    }
}

suspend private fun getInternal(location: ResolvedLocation): WeatherResult {
    Log.d(TAG, "get $location")

    val url = URL(
        OPENWEATHER3 + "lat=${location.latitude}&lon=${location.longitude}&appid=$appid&exclude=minutely&units=imperial"
    )
    val reader = InputStreamReader(url.openConnection().getInputStream())
    val fulljsonn = reader.readText()
    reader.close()
    val top = JsonParser.parseString(fulljsonn).asJsonObject
    if (false) {
        Log.d(TAG, "JSON: $fulljsonn")
    } else if(false){
        val gson = GsonBuilder().setPrettyPrinting().create()
        Log.d(TAG, "JSON: " + gson.toJson(top))
    }
    val lat = top.getAsJsonPrimitive("lat").asDouble
    val lon = top.getAsJsonPrimitive("lon").asDouble

    //https://maps.googleapis.com/maps/api/geocode/json?latlng=37.4219983,-122.084&sensor=true&key=xxx
    val address = try{location.getShortAddress()}catch(e:Exception){null}

    val current = top.getAsJsonObject("current")
    val currentDt = current.getAsJsonPrimitive("dt").asLong * 1000
    val currentTemp = current.getAsJsonPrimitive("temp").asDouble
    val currentIconStr =
        current.getAsJsonArray("weather").get(0).asJsonObject.getAsJsonPrimitive("icon").asString
    val currentIcon = getIcon(currentIconStr)
    val currentBitmap = getBitmap(currentIconStr)
    val CURRENT = WeatherResult.Weather.Current(
        currentDt, currentTemp, currentIconStr, currentIcon, currentBitmap
    )

    val hourlys = mutableListOf<WeatherResult.Weather.Hourly>()
    val hourly = top.getAsJsonArray("hourly")
    for (hourp in hourly) {
        val hour = hourp.asJsonObject
        val date = hour.getAsJsonPrimitive("dt").asLong * 1000
        val temp = hour.getAsJsonPrimitive("temp").asDouble
        val iconStr = hour.getAsJsonArray("weather").get(0).asJsonObject.getAsJsonPrimitive(
            "icon"
        ).asString
        val icon = getIcon(iconStr)
        val bitmap = getBitmap(iconStr)
        val pop = hour.getAsJsonPrimitive("pop").asDouble
        val HOURLY = WeatherResult.Weather.Hourly(date, temp, iconStr, icon, bitmap, pop)
        hourlys.add(HOURLY)
    }

    val dailys = mutableListOf<WeatherResult.Weather.Daily>()
    val daily = top.getAsJsonArray("daily")
    for (dailyp in daily) {
        val daily = dailyp.asJsonObject
        val date = daily.getAsJsonPrimitive("dt").asLong * 1000
        val tempo = daily.getAsJsonObject("temp")
        val tempDay = tempo.getAsJsonPrimitive("day").asDouble
        val tempMin = tempo.getAsJsonPrimitive("min").asDouble
        val tempMax = tempo.getAsJsonPrimitive("max").asDouble
        val iconStr =
            daily.getAsJsonArray("weather").get(0).asJsonObject.getAsJsonPrimitive("icon").asString
        val icon = getIcon(iconStr)
        val bitmap = getBitmap(iconStr)
        val pop = daily.getAsJsonPrimitive("pop").asDouble
        val DAILY = WeatherResult.Weather.Daily(
            date, tempDay, tempMin, tempMax, iconStr, icon, bitmap, pop
        )
        dailys.add(DAILY)
    }

    metricNumSuccess.incrementAndGet()

    return WeatherResult.Weather(fulljsonn, lat, lon, address, CURRENT, hourlys, dailys)
}

fun getShortAddress(address: Address?): String {
    if (address == null) return "Null Island"
    val city = address.locality
    if (city != null) return city
    val state = address.adminArea
    if (state != null) return state
    return "Null Island"
}

