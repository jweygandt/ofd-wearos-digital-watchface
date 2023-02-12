package com.ofd.apis

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.InputStreamReader
import java.io.Reader
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

abstract class APIService<Result> {

    val TAG = this.javaClass.simpleName

    val metrics = APIMetrics()

    private val callInProgress = AtomicBoolean(false)

    private var lastQueryTimeMs = 0L
    private var lastQueryData: Result = makeErrorResult("no data yet")

    var valueRetentionMs = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES)

    // makeURL is called first, if not null it is used, else makeInputStream is called
    abstract fun makeURL(context: Context, location: APILocation): URL?
    open fun makeInputStream(context: Context, location: APILocation): Reader {
        throw Exception("need to define makeInputStream")
    }

    abstract fun makeErrorResult(s: String): Result
    abstract fun isErrorResult(r: Result): Boolean
    abstract suspend fun makeResult(
        location: APILocation, fulljson: String, top: JsonObject
    ): Result

    /*
     * if this returns true, makeURL and makeResult will be called again, ideally inside this method
     * something will change. Eventually this needs to return false, or iteration will continue.
     */
    open fun needToIterate(r: Result): Boolean = false

    suspend fun forceget(context: Context, location: APILocation): Result {
        lastQueryTimeMs=0
        return get(context, location)
    }

    suspend fun get(context: Context, location: APILocation): Result {
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

        try {
            return withContext(Dispatchers.IO) {
                getInternal(context, location).apply {
                    if (!isErrorResult(this)) {
                        lastQueryData = this
                        lastQueryTimeMs = now
                    }
                }
            }
        } finally {
            Log.d(TAG, "returning result")
            callInProgress.set(false)
        }
    }

    private suspend fun getInternal(context: Context, location: APILocation): Result {
        try {
            var result: Result
            var iter = 0;
            do {
                if (++iter > 5) {
                    Log.e(TAG, "Too many API Service iterations")
                    return makeErrorResult("Too many API iterations")
                }
                val url = makeURL(context, location)
                val reader = if (url != null) {
                    Log.d(TAG, "fetching: " + url.toString())
                    InputStreamReader(url.openConnection().getInputStream())
                } else {
                    Log.d(TAG, "fetching by input stream")
                    makeInputStream(context, location)
                }
                val fulljson = reader.readText()
                reader.close()
                val top = JsonParser.parseString(fulljson).asJsonObject
                if (false) {
                    Log.d(TAG, "JSON: $fulljson")
                } else if (false) {
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    Log.d(TAG, "JSON: " + gson.toJson(top))
                }
                result = makeResult(location, fulljson, top)
            } while (needToIterate(result))
            return result
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Problems: " + e.message, e)
            return makeErrorResult(e.message ?: "null")
        }
    }
}
