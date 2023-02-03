package com.ofd.apis.openweather

import android.content.Context
import com.google.gson.JsonObject
import com.ofd.apis.APILocation
import com.ofd.apis.APIService
import com.ofd.apis.AQIResult
import com.ofd.watch.R
import com.ofd.watchface.location.ResolvedLocation
import java.net.URL

suspend fun openWeatherAQI(context: Context, location: ResolvedLocation) =
    openWeatherAQIService.get(context, location)

val openWeatherAQIService =object : APIService<AQIResult>() {

    private val OPENWEATHER_AIR_QUALITY = "http://api.openweathermap.org/data/2.5/air_pollution?"

    override val appidR = R.string.openweather_appid

    override fun makeURL(location: APILocation, appid: String?): URL {
        return URL(
            OPENWEATHER_AIR_QUALITY + "lat=${location.latitude}&lon=${location.longitude}&appid=$appid"
        )
    }

    override fun makeErrorResult(s: String): AQIResult {
        return AQIResult.Error(TAG, s)
    }

    override fun isErrorResult(r: AQIResult): Boolean {
        return r is AQIResult.Error
    }

    override suspend fun makeResult(
        location: APILocation, fulljson: String, top: JsonObject
    ): AQIResult {

        val lst = top.getAsJsonArray("list")[0].asJsonObject
        val aqi = lst.getAsJsonObject("main").getAsJsonPrimitive("aqi").asInt

        val comps = lst.getAsJsonObject("components")
        val cmap = mutableMapOf<String, Float>()
        for ((comp, value) in comps.entrySet()) {
            cmap[comp] = value.asFloat
        }

        val date = lst.getAsJsonPrimitive("dt").asLong * 1000

        return AQIResult.AQI(
            "OpenWeather", metrics, location, fulljson, date, aqi, cmap
        )
    }

}
