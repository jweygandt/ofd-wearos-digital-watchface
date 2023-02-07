package com.ofd.apis.openweather

import android.content.Context
import com.google.gson.JsonObject
import com.ofd.apis.*
import com.ofd.watch.R
import com.ofd.watchface.location.ResolvedLocation
import com.thanglequoc.aqicalculator.AQICalculator
import com.thanglequoc.aqicalculator.Pollutant
import java.net.URL

suspend fun openWeatherAQI(context: Context, location: ResolvedLocation) =
    OpenWeatherAQIService.get(context, location)

object OpenWeatherAQIService : AQIService<OpenWeatherAQIService.OpenWeatherAQIDetails>() {

    private val OPENWEATHER_AIR_QUALITY = "http://api.openweathermap.org/data/2.5/air_pollution?"

    val appidR = R.string.openweather_appid
    var appid: String? = null

    class OpenWeatherAQIDetails(
        val location: APILocation,
        val fulljsonn: String,
        val date: Long,
        val color: Int,
        val comps: Map<String, Float>
    ) : AQIDetails {

        val aqippm: Int
        val aqistr: String
        val address: String? = null

        init {
            if (comps.containsKey("pm2_5")) {
                aqippm = AQIResult.AQI.aqiCalculator.getAQI(
                    Pollutant.PM25, comps.get("pm2_5")!!.toDouble()
                ).aqi
                aqistr = aqippm.toString() + " ppm"
            } else {
                this.aqippm = 0
                this.aqistr = "??"
            }
        }

        override val shortText get() = aqistr
        override val rangeValue get() = aqippm.toFloat()
        override val rangeText get() = shortText


    }

    override fun makeURL(context: Context, location: APILocation): URL {
        if (appid == null) {
            appid = context.getString(OpenWeatherAPI.appidR)
        }
        return URL(
            OPENWEATHER_AIR_QUALITY + "lat=${location.latitude}&lon=${location.longitude}&appid=$appid"
        )
    }

    override suspend fun makeResult(
        location: APILocation, fulljson: String, top: JsonObject
    ): AQIResult<OpenWeatherAQIDetails> {

        val lst = top.getAsJsonArray("list")[0].asJsonObject
        val aqi = lst.getAsJsonObject("main").getAsJsonPrimitive("aqi").asInt

        val comps = lst.getAsJsonObject("components")
        val cmap = mutableMapOf<String, Float>()
        for ((comp, value) in comps.entrySet()) {
            cmap[comp] = value.asFloat
        }

        val date = lst.getAsJsonPrimitive("dt").asLong * 1000

        return AQIResult.AQI(
            "OpenWeather", OpenWeatherAQIDetails(location, fulljson, date, aqi, cmap), metrics
        )
    }
}
