package com.ofd.apis

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ofd.watchface.location.ResolvedLocation
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class APIMetrics {
    val metricNumCalls = AtomicInteger()
    val metricNumSuccess = AtomicInteger()
    val metricNumErrors = AtomicInteger()
    val metricNumConflict = AtomicInteger()
    val metricNumBypass = AtomicInteger()
}


abstract class APIService<Result>() {

    val TAG = this.javaClass.simpleName

    val metrics = APIMetrics()

    private val callInProgress = AtomicBoolean(false)

    private var lastQueryTimeMs = 0L
    private var lastQueryData: Result = makeErrorResult("no data yet")

    private var appid: String? = null

    abstract val appidR: Int
    var valueRetentionMs = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)
    abstract fun makeURL(rlocation: ResolvedLocation, appid: String?): URL
    abstract fun makeErrorResult(s: String): Result
    abstract fun isErrorResult(r: Result): Boolean
    abstract suspend fun makeResult(
        resolvedLocation: ResolvedLocation, fulljson: String, top: JsonObject
    ): Result

    suspend fun get(context: Context, location: ResolvedLocation): Result {
        if (appid == null) appid = context.getString(appidR)
        metrics.metricNumCalls.incrementAndGet()
        Log.d(TAG, "Getting AQI: " + location.toString())

        val now = System.currentTimeMillis()
        if (now - lastQueryTimeMs < valueRetentionMs) {
            Log.d(TAG, "Bypassing AQI")
            metrics.metricNumBypass.incrementAndGet()
            return lastQueryData
        }

        if (!callInProgress.compareAndSet(false, true)) {
            Log.e(TAG, "Call in progress")
            metrics.metricNumConflict.incrementAndGet()
            return lastQueryData
        }

        return withContext(Dispatchers.IO) {
            getAQIInternal(location).apply {
                callInProgress.set(false)
                if (!isErrorResult(this)) {
                    lastQueryData = this
                    lastQueryTimeMs = now
                }
            }
        }
    }

    private suspend fun getAQIInternal(resolvedLocation: ResolvedLocation): Result {
        try {
            Log.d(TAG, "getAQI $resolvedLocation")

            val location = resolvedLocation.location!!
            val url = makeURL(resolvedLocation, appid)
            val reader = InputStreamReader(url.openConnection().getInputStream())
            val fulljson = reader.readText()
            reader.close()
            val top = JsonParser.parseString(fulljson).asJsonObject
            if (false) {
                Log.d(TAG, "JSON: $fulljson")
            } else if (false) {
                val gson = GsonBuilder().setPrettyPrinting().create()
                Log.d(TAG, "JSON: " + gson.toJson(top))
            }
            return makeResult(resolvedLocation, fulljson, top)
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Problems: " + e.message, e)
            return makeErrorResult(e.message ?: "null")
        }
    }

}
