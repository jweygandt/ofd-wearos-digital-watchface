package com.ofd.openweather

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.location.Address
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.ofd.openweather.OpenWeatherService3.WeatherResult.Weather
import com.ofd.watch.R
import com.ofd.watchface.location.ResolvedLocation
import com.patloew.colocation.CoGeocoder
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class OpenWeatherService3 {

    companion object {
        private const val TAG = "OpenWeatherService3"

        private const val OPENWEATHER3 = "https://api.openweathermap.org/data/3.0/onecall?"
        private const val OPENWEATHERICON = "https://openweathermap.org/img/wn/"

        private val BITMAP_CACHE = ConcurrentHashMap<String, Bitmap>()
        private val ICON_CACHE = ConcurrentHashMap<String, Icon>()

        val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm:ss")

        suspend fun getBitmap(iname: String): Bitmap? {
            var bm = BITMAP_CACHE[iname]
            if (bm == null) {
                loadImage(iname)
                bm = BITMAP_CACHE[iname]
            }
            return bm
        }

        suspend fun getIcon(iname: String): Icon? {
            var icon = ICON_CACHE[iname]
            if (icon == null) {
                loadImage(iname)
                icon = ICON_CACHE[iname]
            }
            return icon
        }

        suspend private fun loadImage(iname: String) {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "Reading image: " + iname)
                try {
                    val url = URL("https://openweathermap.org/img/wn/$iname@2x.png")
                    val bm = BitmapFactory.decodeStream(
                        url.openConnection().getInputStream()
                    )
                    val icon = Icon.createWithBitmap(bm)
                    BITMAP_CACHE[iname] = bm
                    ICON_CACHE[iname] = icon
                } catch (e: Exception) {
                    Log.e(TAG, "URL: $iname", e)
                }
            }
        }

    }

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

    sealed class WeatherResult {
        data class Weather(
            val fulljson: String,
            val lat: Double,
            val lon: Double,
            val address: String?,
            val current: Current,
            val hourlys: MutableList<Hourly>,
            val dailys: MutableList<Daily>,
        ) : WeatherResult()

        data class Error(val msg: String) : WeatherResult()
    }

    class OpenWeatherAPI(context: Context) {
        companion object {
            val valueRetentionMs = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)

            val callInProgress = AtomicBoolean(false)

            val metricNumCalls = AtomicInteger()
            val metricNumSuccess = AtomicInteger()
            val metricNumErrors = AtomicInteger()
            val metricNumConflict = AtomicInteger()
            val metricNumBypass = AtomicInteger()

            var lastQueryTimeMs = 0L
            var lastQueryData: WeatherResult = WeatherResult.Error("no query")

            fun statusString() =
                "" + metricNumCalls.get() + "/" + (metricNumSuccess.get() + metricNumBypass.get()) + ":" + metricNumBypass.get() + ":" + metricNumErrors.get() + ":" + metricNumConflict.get()
        }

        private val appid = context.getString(R.string.openweather_appid3)
        private val coGeocoder = CoGeocoder.from(context)

        suspend fun getWeather(location: ResolvedLocation): WeatherResult {
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
            } else {
                val gson = GsonBuilder().setPrettyPrinting().create()
                Log.d(TAG, "JSON: " + gson.toJson(top))
            }
            val lat = top.getAsJsonPrimitive("lat").asDouble
            val lon = top.getAsJsonPrimitive("lon").asDouble

            //https://maps.googleapis.com/maps/api/geocode/json?latlng=37.4219983,-122.084&sensor=true&key=xxx
            val address =
                null // try{coGeocoder.getAddressFromLocation(lat, lon)}catch(e:Exception){null}
            val city = if (address == null) "no city" else getShortAddress(address)

            val current = top.getAsJsonObject("current")
            val currentDt = current.getAsJsonPrimitive("dt").asLong * 1000
            val currentTemp = current.getAsJsonPrimitive("temp").asDouble
            val currentIconStr = current.getAsJsonArray("weather")
                .get(0).asJsonObject.getAsJsonPrimitive("icon").asString
            val currentIcon = getIcon(currentIconStr)
            val currentBitmap = getBitmap(currentIconStr)
            val CURRENT =
                Current(currentDt, currentTemp, currentIconStr, currentIcon, currentBitmap)

            val hourlys = mutableListOf<Hourly>()
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
                val HOURLY = Hourly(date, temp, iconStr, icon, bitmap, pop)
                hourlys.add(HOURLY)
            }

            val dailys = mutableListOf<Daily>()
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
                val DAILY = Daily(date, tempDay, tempMin, tempMax, iconStr, icon, bitmap, pop)
                dailys.add(DAILY)
            }

            metricNumSuccess.incrementAndGet()

            return Weather(fulljsonn, lat, lon, address, CURRENT, hourlys, dailys)
        }

        fun getShortAddress(address: Address?): String {
            if (address == null) return "Null Island"
            val city = address.locality
            if (city != null) return city
            val state = address.adminArea
            if (state != null) return state
            return "Null Island"
        }

    }

}
