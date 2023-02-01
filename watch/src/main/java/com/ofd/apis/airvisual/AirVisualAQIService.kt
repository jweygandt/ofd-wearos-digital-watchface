package com.ofd.apis.airvisual

import com.google.gson.JsonObject
import com.ofd.apis.APIService
import com.ofd.apis.AQIResult
import com.ofd.watch.R
import com.ofd.watchface.location.ResolvedLocation
import java.net.URL
import java.text.SimpleDateFormat

class AirVisualAQIService : APIService<AQIResult>() {

    override val appidR = R.string.airvisual_appid

    private val AIRVISUALURL = "https://api.airvisual.com/v2/nearest_city?"
    override fun makeURL(rlocation: ResolvedLocation, appid: String?): URL {
        val location = rlocation.location!!
        return URL(
            AIRVISUALURL + "lat=${location.latitude}&lon=${location.longitude}&appid=$appid"
        )
    }

    override fun makeErrorResult(s: String): AQIResult {
        return AQIResult.Error(TAG, s)
    }

    override fun isErrorResult(r: AQIResult): Boolean {
        return r is AQIResult.Error
    }

    private val longsdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    override suspend fun makeResult(rlocation: ResolvedLocation, fulljson: String, top: JsonObject): AQIResult {
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
                "AirVisualAQI", metrics, rlocation, fulljson, date, aqi, cmap
            )
        } else {
            return AQIResult.Error("AirVisualAQI", status)
        }
    }
}
