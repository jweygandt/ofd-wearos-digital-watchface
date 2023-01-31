package com.ofd.openweather

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.location.Location
import android.util.Log
import com.google.gson.*
import com.ofd.watch.R
import com.ofd.watchface.location.ResolvedLocation
import java.lang.reflect.Type
import java.net.URL
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import okhttp3.ResponseBody
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


class OpenWeatherService {

    companion object {
        private const val TAG = "OpenWeatherService"

        private const val OPENWEATHER = "http://api.openweathermap.org/data/2.5/"
        private const val OPENWEATHERICON = "https://openweathermap.org/img/wn/"

        private val BITMAP_CACHE = ConcurrentHashMap<String, Bitmap>()
        private val ICON_CACHE = ConcurrentHashMap<String, Icon>()

        val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm:ss")

        fun getBitmap(iname: String):Bitmap?{
            var bm = BITMAP_CACHE[iname]
            if(bm==null){
                loadImage(iname)
                bm = BITMAP_CACHE[iname]
            }
            return bm
        }

        fun getIcon(iname: String):Icon?{
            var icon = ICON_CACHE[iname]
            if(icon==null){
                loadImage(iname)
                icon = ICON_CACHE[iname]
            }
            return icon
        }

        private fun loadImage(iname: String) {
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

    interface Service {
        //http://api.openweathermap.org/data/2.5/weather
        @GET("weather")
        fun weatherNow(
            @Query("lat") lat: String,
            @Query("lon") lon: String,
            @Query("appid") appid: String,
            @Query("units") units: String = "imperial",
        ): Call<OWNowResult>?

        @GET("forecast")
        fun forecast(
            @Query("lat") lat: String,
            @Query("lon") lon: String,
            @Query("appid") appid: String,
            @Query("units") units: String = "imperial",
        ): Call<OWForecastResult>?
    }

    interface OWIconService {
        //https://openweathermap.org/img/wn/$iname@2x.png
        @GET("{iname}@2x.png")
        fun icon(@Path("iname") iname: String): Call<Bitmap>
    }

    class BitmapConverter : Converter.Factory() {
        override fun responseBodyConverter(
            type: Type, annotations: Array<out Annotation>, retrofit: Retrofit
        ): Converter<ResponseBody, *>? {
            return Converter { resp -> BitmapFactory.decodeStream(resp.byteStream()) }
        }
    }

    class OWNowResult(
        val fulljson: String,
        val temp: Float,
        val iconName: String,
        val hasRealData: Boolean,
        var icon: Icon? = null
    )

    class FD(
        val time: Long, val temp: Float, val iconName: String, var bitmap: Bitmap? = null
    )

    class OWForecastResult(
        val fulljson: String, val data: List<FD>, val location: String,
        val hasRealData: Boolean,
    )

    private class WeatherNowDeserializer : JsonDeserializer<OWNowResult> {
        override fun deserialize(
            json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?
        ): OWNowResult? {
            if (json == null) return null

            try {
                val obj = json.asJsonObject
                val fulljson = obj.toString()
                Log.d(TAG, "JSON: " + fulljson)

                val weather = obj.getAsJsonArray("weather")
                if (weather.size() > 0) OWNowResult(fulljson, 0f, "", false)

                val elt = weather.get(0).asJsonObject
                val icon = elt.getAsJsonPrimitive("icon").asString
                val main = obj.getAsJsonObject("main")
                Log.d(TAG, "main: " + main.toString())
                val temp = main.get("temp").asFloat

                return OWNowResult(fulljson, temp, icon, true)
            } catch (e: Exception) {
                throw JsonParseException(e)
            }
        }
    }


    private class ForecastDeserializer : JsonDeserializer<OWForecastResult> {
        override fun deserialize(
            json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?
        ): OWForecastResult? {
            if (json == null) return null

            try {
                val obj = json.asJsonObject
                val fulljson = obj.toString()
                Log.d(TAG, "JSON: " + fulljson)

                val list = obj.getAsJsonArray("list")
                val data = mutableListOf<FD>()
                for (inx in (0..list.size() - 1)) {
                    val d = list.get(inx).asJsonObject
                    val dt = d.get("dt").asLong * 1000
                    val sd = d.get("main").asJsonObject
                    val temp = sd.get("temp").asFloat
                    val iname =
                        d.get("weather").asJsonArray.get(0).asJsonObject.get("icon").asString
                    val fd = FD(dt, temp, iname)
                    Log.d(TAG, "FD:" + sdf.format(fd.time) + " " + fd.temp + " " + fd.iconName)
                    data.add(fd)
                }
                val city = obj.get("city").asJsonObject.get("name").asString
                Log.d(TAG, "City: " + city)

                return OWForecastResult(fulljson, data, city, true)
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
            var lastQueryData = OWNowResult("initial", 0f, "", false)

            fun statusString() =
                "" + metricNumCalls.get() + "/" + (metricNumSuccess.get() + metricNumBypass.get()) + ":" + metricNumBypass.get() + ":" + metricNumErrors.get() + ":" + metricNumConflict.get()
        }

        private val appid = context.getString(R.string.openweather_appid)

        private val gson =
            GsonBuilder().registerTypeAdapter(OWNowResult::class.java, WeatherNowDeserializer())
                .registerTypeAdapter(OWForecastResult::class.java, ForecastDeserializer()).create()

        private val nowRetrofit = Retrofit.Builder().baseUrl(OPENWEATHER)
            .addConverterFactory(GsonConverterFactory.create(gson)).build()

        private val service: Service = nowRetrofit.create(Service::class.java)

        private val iconService = Retrofit.Builder().baseUrl(OPENWEATHERICON).addConverterFactory(
            BitmapConverter()
        ).build().create(OWIconService::class.java)

        fun get(location: ResolvedLocation, callback: Consumer<OWNowResult>) {
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

        private fun getInternal(location: ResolvedLocation, callback: Consumer<OWNowResult>) {
            Log.d(TAG, "get $location")

            val call = service.weatherNow(
                lat = location.latitude.toString(),
                lon = location.longitude.toString(),
                appid = appid
            )!!
            Log.d(TAG, "Calling: " + call.request().url().toString())
            call.enqueue(object : Callback<OWNowResult> {
                override fun onResponse(call: Call<OWNowResult>, response: Response<OWNowResult>) {
                    Log.d(TAG, "onResponse")
                    if (response.isSuccessful && response.body() != null) {
                        metricNumSuccess.incrementAndGet()
                        val ow = response.body()!!
                        ow.icon = ICON_CACHE[ow.iconName]
                        if (ow.icon != null) {
                            callback.accept(ow)
                        } else {
                            Log.d(TAG, "Fetching icon: " + ow.iconName)
                            iconService.icon(ow.iconName).enqueue(object : Callback<Bitmap> {
                                override fun onResponse(
                                    call: Call<Bitmap>, response: Response<Bitmap>
                                ) {
                                    val bm = response.body()!!
                                    ow.icon = Icon.createWithBitmap(bm)
                                    BITMAP_CACHE[ow.iconName] = bm
                                    ICON_CACHE[ow.iconName] = ow.icon!!
                                    callback.accept(ow)
                                }

                                override fun onFailure(call: Call<Bitmap>, t: Throwable) {
                                    Log.e(
                                        TAG,
                                        "Issues2: " + response.message() + ":" + response.body() + ":" + response.code()
                                    )
                                    callback.accept(ow)
                                }
                            })
                        }
                    } else {
                        metricNumErrors.incrementAndGet()
                        Log.d(
                            TAG,
                            "Issues: " + response.message() + ":" + response.body() + ":" + response.code()
                        )
                        callback.accept(OWNowResult(response.message(), 0f, "", false))
                    }
                }

                override fun onFailure(call: Call<OWNowResult>, t: Throwable) {
                    metricNumErrors.incrementAndGet()
                    Log.e("TAG", "Problems on call: " + t.message, t)
                    callback.accept(OWNowResult(t.message ?: "", 0f, "", false))
                }
            })
        }

        fun getForecast(location: ResolvedLocation) : OWForecastResult {
            Log.d(TAG, "getf $location")

            val call = service.forecast(
                lat = location.latitude.toString(),
                lon = location.longitude.toString(),
                appid = appid
            )!!
            Log.d(TAG, "Callingf: " + call.request().url().toString())
            val owForecastResult = call.execute().body()!!
            for( t in owForecastResult.data){
                t.bitmap = getBitmap(t.iconName)
            }
            return owForecastResult
        }
    }

}
