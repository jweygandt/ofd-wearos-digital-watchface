package com.ofd.apis.airvisual

import android.content.Context
import com.google.gson.JsonObject
import com.ofd.apis.*
import com.ofd.apis.openweather.OpenWeatherAPI
import com.ofd.watch.R
import com.thanglequoc.aqicalculator.AQICalculator
import com.thanglequoc.aqicalculator.Pollutant
import java.net.URL
import java.text.SimpleDateFormat

object AirVisualAQIService : APIService<AQIResult<AirVisualAQIService.AirVisualAQIDetails>>() {

    val appidR = R.string.airvisual_appid
    var appid: String? = null

    private val AIRVISUALURL = "https://api.airvisual.com/v2/nearest_city?"

    class AirVisualAQIDetails(
        val location: APILocation,
        val fulljsonn: String,
        val date: Long,
        val color: Int,
        val comps: Map<String, Float>
    ) : AQIDetails {

        val aqippm: Int
        val aqistr: String

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
            AIRVISUALURL + "lat=${location.latitude}&lon=${location.longitude}&appid=$appid"
        )
    }

    override fun makeErrorResult(s: String): AQIResult<AirVisualAQIDetails> {
        return AQIResult.Error(TAG, s)
    }

    override fun isErrorResult(r: AQIResult<AirVisualAQIDetails>): Boolean {
        return r is AQIResult.Error
    }

    private val longsdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    override suspend fun makeResult(
        location: APILocation,
        fulljson: String,
        top: JsonObject
    ): AQIResult<AirVisualAQIDetails> {
        val status = top.getAsJsonPrimitive("status").asString
        if ("success".equals(status)) {
            val data = top.getAsJsonObject("data")
            val city = data.getAsJsonPrimitive("city").asString
            val pollution = data.getAsJsonObject("current").getAsJsonObject("pollution")
            val timestr = pollution.getAsJsonPrimitive("ts").asString
            val aqi = pollution.getAsJsonPrimitive("aqius").asInt
            val cmap = mutableMapOf<String, Float>()
            val date = longsdf.parse(timestr).time

            return AQIResult.AQI(
                TAG, AirVisualAQIDetails(location, fulljson, date, aqi, cmap), metrics
            )
        } else {
            return AQIResult.Error("AirVisualAQI", status)
        }
    }
}
