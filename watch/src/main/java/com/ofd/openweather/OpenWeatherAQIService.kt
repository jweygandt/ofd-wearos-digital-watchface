package com.ofd.openweather

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.gson.*
import com.ofd.digital.alpha.R
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


class OpenWeatherAQIService {

    companion object {
        private const val TAG = "OpenWeatherAQIService"

        private const val OPENWEATHER_AIR_QUALITY = "http://api.openweathermap.org/data/2.5/"
    }

    interface AQIService {
        @GET("air_pollution")
        fun getAQI(
            @Query("lat") lat: String,
            @Query("lon") lon: String,
            @Query("appid") appid: String
        ): Call<OWAQIResult>?
    }

    class OWAQIResult(
        val fulljson: String,
        val date: Long,
        val aqi: Float,
        val hasRealData: Boolean
    )

/* example:
200 success
{
  "coord": [
    50.0,
    50.0
  ],
  "list": [
    {
      "dt": 1606147200,
      "main": {
        "aqi": 4.0
      },
      "components": {
        "co": 203.609,
        "no": 0.0,
        "no2": 0.396,
        "o3": 75.102,
        "so2": 0.648,
        "pm2_5": 23.253,
        "pm10": 92.214,
        "nh3": 0.117
      }
    }
  ]
}
*/

    private class AQIResultDeserializer : JsonDeserializer<OWAQIResult> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): OWAQIResult? {
            if (json == null) return null

            try {
                val obj = json.asJsonObject
                val fulljson = obj.toString()
                Log.d(TAG, "JSON: " + fulljson)

                val ary = obj.getAsJsonArray("list")
                if (ary.size() > 0) OWAQIResult(fulljson, 0L, 0.0f, false)

                val elt = ary.get(0).asJsonObject
                val date = elt.getAsJsonPrimitive("dt").asLong
                val main = elt.getAsJsonObject("main")
//                Log.d(TAG, "main: " + main.toString())
                val aqi = main.get("aqi").asFloat

                return OWAQIResult(fulljson, date, aqi, true)
            } catch (e: Exception) {
                throw JsonParseException(e)
            }
        }
    }


    class OpenWeatherAQIAPI(context: Context) {
        companion object {
            val valueRetentionMs = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)

            val callInProgress = AtomicBoolean(false)

            val metricNumCalls = AtomicInteger()
            val metricNumSuccess = AtomicInteger()
            val metricNumErrors = AtomicInteger()
            val metricNumConflict = AtomicInteger()
            val metricNumBypass = AtomicInteger()

            var lastQueryTimeMs = 0L
            var lastQueryData = OWAQIResult("initial", 0, 0f, false)

            fun statusString() =
                "" + metricNumCalls.get() +
                    "/" + (metricNumSuccess.get() + metricNumBypass.get()) +
                    ":" + metricNumBypass.get() + ":" +
                    metricNumErrors.get() + ":" + metricNumConflict.get()
        }

        private val appid = context.getString(R.string.openweather_appid)

        private val gson = GsonBuilder()
            .registerTypeAdapter(OWAQIResult::class.java, AQIResultDeserializer())
            .create()

        private val retrofit = Retrofit.Builder()
            .baseUrl(OPENWEATHER_AIR_QUALITY)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        private val service: AQIService =
            retrofit.create(AQIService::class.java)

        fun getAQI(location: Location, callback: Consumer<OWAQIResult>) {
            metricNumCalls.incrementAndGet()
            Log.d(TAG, "Getting AQI: " + location.toString())

            val now = System.currentTimeMillis()
            if (now - lastQueryTimeMs < valueRetentionMs) {
                Log.d(TAG, "Bypassing AQI")
                metricNumBypass.incrementAndGet()
                callback.accept(lastQueryData)
            }

            if (!callInProgress.compareAndSet(false, true)) {
                Log.e(TAG, "Call in progress")
                metricNumConflict.incrementAndGet()
                callback.accept(lastQueryData)
            }

            getAQIInternal(location) {
                callInProgress.set(false)
                lastQueryData = it
                lastQueryTimeMs = now
                callback.accept(it)
            }
        }

        private fun getAQIInternal(location: Location, callback: Consumer<OWAQIResult>) {
            Log.d(TAG, "getAQI $location")

            val call = service.getAQI(
                lat = location.latitude.toString(),
                lon = location.longitude.toString(),
                appid = appid
            )!!
            Log.d(TAG, "Calling: " + call.request().url().toString())
            call.enqueue(object : Callback<OWAQIResult> {
                override fun onResponse(call: Call<OWAQIResult>, response: Response<OWAQIResult>) {
                    Log.d(TAG, "onResponse")
                    if (response.isSuccessful && response.body() != null) {
                        metricNumSuccess.incrementAndGet()
                        callback.accept(response.body()!!)
                    } else {
                        metricNumErrors.incrementAndGet()
                        Log.d(
                            TAG, "Issues: " + response.message() + ":" + response.body() + ":"
                                + response.code()
                        )
                        callback.accept(OWAQIResult(response.message(), 0, 0f, false))
                    }
                }

                override fun onFailure(call: Call<OWAQIResult>, t: Throwable) {
                    metricNumErrors.incrementAndGet()
                    Log.e("TAG", "Problems on call: " + t.message, t)
                    callback.accept(OWAQIResult(t.message ?: "", 0, 0f, false))
                }
            })
        }
    }

}
