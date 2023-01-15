package com.ofd.openweather

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.gson.*
import com.ofd.watch.R
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


class OpenWeatherService {

    companion object {
        private const val TAG = "OpenWeatherService"

        private const val OPENWEATHER = "http://api.openweathermap.org/data/2.5/"
    }

    interface Service {
        @GET("weather")
        fun get(
            @Query("lat") lat: String,
            @Query("lon") lon: String,
            @Query("appid") appid: String,
            @Query("units") units: String = "imperial",
        ): Call<OWResult>?
    }

    class OWResult(
        val fulljson: String,
        val temp: Float,
        val icon: String,
        val hasRealData: Boolean
    )

/*
{
  "coord": {
    "lon": 10.99,
    "lat": 44.34
  },
  "weather": [
    {
      "id": 501,
      "main": "Rain",
      "description": "moderate rain",
      "icon": "10d"
    }
  ],
  "base": "stations",
  "main": {
    "temp": 298.48,
    "feels_like": 298.74,
    "temp_min": 297.56,
    "temp_max": 300.05,
    "pressure": 1015,
    "humidity": 64,
    "sea_level": 1015,
    "grnd_level": 933
  },
  "visibility": 10000,
  "wind": {
    "speed": 0.62,
    "deg": 349,
    "gust": 1.18
  },
  "rain": {
    "1h": 3.16
  },
  "clouds": {
    "all": 100
  },
  "dt": 1661870592,
  "sys": {
    "type": 2,
    "id": 2075663,
    "country": "IT",
    "sunrise": 1661834187,
    "sunset": 1661882248
  },
  "timezone": 7200,
  "id": 3163858,
  "name": "Zocca",
  "cod": 200
}
*/

    private class ResultDeserializer : JsonDeserializer<OWResult> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): OWResult? {
            if (json == null) return null

            try {
                val obj = json.asJsonObject
                val fulljson = obj.toString()
                Log.d(TAG, "JSON: " + fulljson)

                val weather = obj.getAsJsonArray("weather")
                if (weather.size() > 0) OWResult(fulljson, 0f, "", false)

                val elt = weather.get(0).asJsonObject
                val icon = elt.getAsJsonPrimitive("icon").asString
                val main = obj.getAsJsonObject("main")
                Log.d(TAG, "main: " + main.toString())
                val temp = main.get("temp").asFloat

                return OWResult(fulljson, temp, icon, true)
            } catch (e: Exception) {
                throw JsonParseException(e)
            }
        }
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
            var lastQueryData = OWResult("initial",  0f, "", false)

            fun statusString() =
                "" + metricNumCalls.get() +
                    "/" + (metricNumSuccess.get() + metricNumBypass.get()) +
                    ":" + metricNumBypass.get() + ":" +
                    metricNumErrors.get() + ":" + metricNumConflict.get()
        }

        private val appid = context.getString(R.string.openweather_appid)

        private val gson = GsonBuilder()
            .registerTypeAdapter(OWResult::class.java, ResultDeserializer())
            .create()

        private val retrofit = Retrofit.Builder()
            .baseUrl(OPENWEATHER)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        private val service: Service =
            retrofit.create(Service::class.java)

        fun get(location: Location, callback: Consumer<OWResult>) {
            metricNumCalls.incrementAndGet()
            Log.d(TAG, "Getting : " + location.toString())

            val now = System.currentTimeMillis()
            if (now - lastQueryTimeMs < valueRetentionMs) {
                Log.d(TAG, "Bypassing ")
                metricNumBypass.incrementAndGet()
                callback.accept(lastQueryData)
            }

            if (!callInProgress.compareAndSet(false, true)) {
                Log.e(TAG, "Call in progress")
                metricNumConflict.incrementAndGet()
                callback.accept(lastQueryData)
            }

            getInternal(location) {
                callInProgress.set(false)
                lastQueryData = it
                lastQueryTimeMs = now
                callback.accept(it)
            }
        }

        private fun getInternal(location: Location, callback: Consumer<OWResult>) {
            Log.d(TAG, "get $location")

            val call = service.get(
                lat = location.latitude.toString(),
                lon = location.longitude.toString(),
                appid = appid
            )!!
            Log.d(TAG, "Calling: " + call.request().url().toString())
            call.enqueue(object : Callback<OWResult> {
                override fun onResponse(call: Call<OWResult>, response: Response<OWResult>) {
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
                        callback.accept(OWResult(response.message(),  0f, "", false))
                    }
                }

                override fun onFailure(call: Call<OWResult>, t: Throwable) {
                    metricNumErrors.incrementAndGet()
                    Log.e("TAG", "Problems on call: " + t.message, t)
                    callback.accept(OWResult(t.message ?: "",  0f, "", false))
                }
            })
        }
    }

}
