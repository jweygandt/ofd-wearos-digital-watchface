package com.ofd.apis.openweather

import android.content.Context
import android.location.Address
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ofd.apis.APILocation
import com.ofd.apis.APIService
import com.ofd.apis.WeatherResult
import com.ofd.watch.R
import com.ofd.watchface.location.ResolvedLocation
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun openWeather(context: Context, location: APILocation) = openWeatherAPI.get(context, location)

val openWeatherAPI = object : APIService<WeatherResult>() {

    override val appidR = R.string.openweather_appid

    override fun makeURL(location :APILocation, appid: String?): URL {
        return URL(
            OPENWEATHER3 + "lat=${location.latitude}&lon=${location.longitude}&appid=$appid&exclude=minutely&units=imperial"
        )
    }

    override fun makeErrorResult(s: String): WeatherResult {
       return WeatherResult.Error(TAG, s)
    }

    override fun isErrorResult(r: WeatherResult): Boolean {
        return r is WeatherResult.Error
    }

    private val OPENWEATHER3 = "https://api.openweathermap.org/data/3.0/onecall?"
    override suspend fun makeResult(location: APILocation, fulljson: String, top: JsonObject): WeatherResult {
            val lat = top.getAsJsonPrimitive("lat").asDouble
            val lon = top.getAsJsonPrimitive("lon").asDouble

            //https://maps.googleapis.com/maps/api/geocode/json?latlng=37.4219983,-122.084&sensor=true&key=xxx
            val address = try {
                location.getShortAddress()
            } catch (e: Exception) {
                null
            }

            val current = top.getAsJsonObject("current")
            val currentDt = current.getAsJsonPrimitive("dt").asLong * 1000
            val currentTemp = current.getAsJsonPrimitive("temp").asDouble
            val weather = current.getAsJsonArray("weather").get(0).asJsonObject
            val descr = weather.getAsJsonPrimitive("main").asString
            val currentIconStr = weather.getAsJsonPrimitive("icon").asString
            val currentIcon = getIcon(currentIconStr)
            val currentBitmap = getBitmap(currentIconStr)
            val CURRENT = WeatherResult.Weather.Current(
                currentDt, currentTemp, descr, currentIconStr, currentIcon, currentBitmap
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
                val iconStr = daily.getAsJsonArray("weather")
                    .get(0).asJsonObject.getAsJsonPrimitive("icon").asString
                val icon = getIcon(iconStr)
                val bitmap = getBitmap(iconStr)
                val pop = daily.getAsJsonPrimitive("pop").asDouble
                val DAILY = WeatherResult.Weather.Daily(
                    date, tempDay, tempMin, tempMax, iconStr, icon, bitmap, pop
                )
                dailys.add(DAILY)
            }

            metrics.metricNumSuccess.incrementAndGet()

            return WeatherResult.Weather(
                "OpenWeather", metrics, fulljson, lat, lon, address, CURRENT, hourlys, dailys
            )
    }

}
