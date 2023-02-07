package com.ofd.apis.purpleair

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.odbol.wear.airquality.purpleair.*
import com.ofd.apis.*
import com.ofd.watch.R
import com.thanglequoc.aqicalculator.AQICalculator
import com.thanglequoc.aqicalculator.Pollutant
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min

object PurpleAirService2 : AQIService<PurpleAirService2.PurpleAirAQIDetails>() {

    const val PURPLE_AIR_BASE_URL = "https://api.purpleair.com/v1/sensors?"
    val REQUIRED_FIELDS = listOf(
        "name", "location_type", "pm2.5_10minute", "latitude", "longitude", "last_seen"
    ).joinToString(",")

    class PurpleAirAQIDetails(
        location: APILocation,
        val address: String?,
        fulljsonn: String,
        val date: Long,
        val aqippm: Int,
        val medianInx: Int,
        val samples: List<PASensor>
    ) : AQIDetails {

        val aqistr: String
        val color: Int

        init {
            this.aqistr = aqippm.toString() + " ppm"
            this.color = AQIResult.AQI.colorInxForComp("aqi", aqippm.toFloat())
        }

        override val shortText get() = aqistr
        override val rangeValue get() = aqippm.toFloat()
        override val rangeText get() = shortText
    }

    const val milesForDegree = 69.172

    private const val initialFenceMiles = 1.0f
    private const val incrementalScale = 3f
    private const val maximumFenceMiles = 50f
    private const val minimumSensors = 10
    private const val maximumSensors = 30

    private var fenceSize = initialFenceMiles
    private var lastLocation: APILocation? = null

    private val NEED_TO_ITERATE = "NEED_TO_ITERATE"
    override fun needToIterate(r: AQIResult<PurpleAirAQIDetails>): Boolean {
        if (r is AQIResult.Error && NEED_TO_ITERATE.equals(r.msg)) {
            fenceSize *= incrementalScale
            return true
        }
        return false
    }

    override fun makeURL(context: Context, location: APILocation): URL? = null

    override fun makeInputStream(context: Context, location: APILocation): Reader {
        val movement = if (lastLocation == null) {
            100000
        } else {
            val r = floatArrayOf(0f)
            Location.distanceBetween(
                lastLocation!!.latitude,
                lastLocation!!.longitude,
                location.latitude,
                location.longitude,
                r
            )
            r[0].toInt()
        }
        if (movement > 20000) {
            lastLocation = location
            fenceSize = initialFenceMiles
        }
        lastLocation = location
        val latitudeDelta = fenceSize / 2 / milesForDegree
        val longitudeDelta = cos(location.latitude * Math.PI / 180)
        Log.d(TAG, "Fence size: $fenceSize")
        val fields: String = REQUIRED_FIELDS
        val nwlat: Double = location.latitude + latitudeDelta
        val nwlng: Double = location.longitude - longitudeDelta
        val selat: Double = location.latitude - latitudeDelta
        val selng: Double = location.longitude + longitudeDelta

        val conn =
            URL(PURPLE_AIR_BASE_URL + "fields=$fields&nwlat=$nwlat&nwlng=$nwlng&selat=$selat&selng=$selng").openConnection() as HttpURLConnection
        conn.setRequestProperty("X-API-Key", context.getString(R.string.purpleair_api_key_read))
        conn.setRequestProperty("Accept", "*/*")
        conn.requestMethod = "GET"
        conn.useCaches = false
        conn.doInput = true
        conn.doOutput = false
        if (conn.responseCode != 200) {
            Log.e(TAG, "Request response: " + conn.responseCode)
            for (ent in conn.headerFields) {
                Log.e(TAG, "Field: " + ent)
            }
            Log.e(TAG, "ERROR: " + InputStreamReader(conn.errorStream).readText())
        }
        return InputStreamReader(conn.inputStream)
    }

    data class PASensor(
        @SerializedName("sensor_index") val ID: Int,
        val name: String?,
        val location_type: String?,
        val latitude: Double?,
        val longitude: Double?,
        val stats: Statistics?,
        @SerializedName("last_seen") val lastSeenSeconds: Long?,
        val pm25Override: Double?
    ) {

        var distanceMeters = -1f

        val pm25: Double
            get() = stats?.avg10Min ?: pm25Override ?: 0.0
    }

    override suspend fun makeResult(
        location: APILocation, fulljson: String, top: JsonObject
    ): AQIResult<PurpleAirAQIDetails> {
        val fields = top.getAsJsonArray("fields").map { it.asString }
        val locationTypes = top.getAsJsonArray("location_types").map { it.asString }
        val data = top.getAsJsonArray("data").map { it.asJsonArray }

        // Yes, we could use reflection here, but we want speed.
        var sensorIndexIndex: Int = -1
        var nameIndex: Int = -1
        var locationTypeIndex: Int = -1
        var latitudeIndex: Int = -1
        var longitudeIndex: Int = -1
        var pm25Index: Int = -1
        var lastSeenIndex: Int = -1
        fields.forEachIndexed { index: Int, fieldName: String ->
            when (fieldName) {
                "sensor_index" -> sensorIndexIndex = index
                "name" -> nameIndex = index
                "location_type" -> locationTypeIndex = index
                "latitude" -> latitudeIndex = index
                "longitude" -> longitudeIndex = index
                "pm2.5_10minute" -> pm25Index = index
                "last_seen" -> lastSeenIndex = index
            }
        }

        val nowSecs = System.currentTimeMillis() / 1000

        val sensors = data.mapNotNull {
            try {
                PASensor(
                    ID = it[sensorIndexIndex].asInt,
                    name = it[nameIndex].asString,
                    location_type = locationTypes[it[locationTypeIndex].asInt],
                    latitude = it[latitudeIndex].asDouble,
                    longitude = it[longitudeIndex].asDouble,
                    stats = null,
                    lastSeenSeconds = nowSecs - it[lastSeenIndex].asLong,
                    pm25Override = it[pm25Index].asDouble
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse sensor $it", e)
                null
            }
        }

        if (sensors.size < minimumSensors && fenceSize < maximumFenceMiles) {
            Log.w(TAG, "Only found ${sensors.size} sensors")
            return AQIResult.Error(
                TAG, NEED_TO_ITERATE
            )
        }

        if (sensors.isNotEmpty()) {
            //metricLastResultSize.set(sensors.size)
            val calculator = DistanceCalculator(location)
            val s00 = sensors.filter { s -> s.location_type.equals("outside") }
//            Log.d(TAG, "Sorting ${s00.size}")
            val s0 = s00.sortedWith(sortByClosest(calculator))
            val shortList = if (s0.size > maximumSensors) s0.subList(
                0, maximumSensors - 1
            )
            else s0
            if (s0.size < minimumSensors && fenceSize < maximumFenceMiles) {
                return AQIResult.Error(
                    TAG, NEED_TO_ITERATE
                )
            }
            val sortedByPM25 = shortList.sortedWith(sortByPM25())
            val inx = (sortedByPM25.size / 2f + .5f).toInt()
            val medianInx = min(inx, sortedByPM25.size - 1)
            val pm25 = sortedByPM25[medianInx].pm25
            Log.d(TAG, "Found ${s0.size} sensors, pm25=$pm25")

            val address = try {
                location.getShortAddress()
            } catch (e: Exception) {
                null
            }

//            val samples = sortedByPM25.map { s ->
//                object : Samples {
//                    override val name: String
//                        get() = s.name ?: "null"
//                    override val distanceMeters: Int
//                        get() = calculator.calculateDistanceTo(s).toInt()
//                    override val aqi: Int
//                        get() = convertPm25ToAqi(s.pm25)
//                }
//            }
//
            return AQIResult.AQI(
                TAG, PurpleAirAQIDetails(
                    location,
                    address,
                    fulljson,
                    System.currentTimeMillis(),
                    convertPm25ToAqi(pm25),
                    medianInx,
                    sortedByPM25
                ), metrics
            )
        } else {
            Log.d(TAG, "no sensors")
            AirQualitySearch.metricLastResultSize.set(sensors.size)
            return AQIResult.Error(TAG, "no sensors")
        }
    }

    private fun convertPm25ToAqi(pm25: Double): Int {
        val calculator: AQICalculator = AQICalculator.getAQICalculatorInstance()
        return calculator.getAQI(Pollutant.PM25, pm25).aqi
    }

    private fun sortByClosest(calculator: DistanceCalculator): Comparator<PASensor> {
//        Log.d(TAG, "sortByCloset")
        return Comparator<PASensor> { a, b ->
            // Multiply so small differences are counted after rounding.
            val dist =
                (calculator.calculateDistanceTo(a) - calculator.calculateDistanceTo(b)) * 10000
            if (dist > 0) {
                ceil(dist).toInt()
            } else if (dist < 0) {
                floor(dist).toInt()
            } else if (a.equals(b)) {
                0
            } else if (a.hashCode() - b.hashCode() > 0) 1 else -1
        }
    }

    private fun sortByPM25(): Comparator<PASensor> {
        return Comparator { a, b ->
            // Multiply so small differences are counted after rounding.
            val diff = a.pm25 - b.pm25
            if (diff > 0) {
                1
            } else if (diff < 0) {
                -1
            } else if (a.equals(b)) {
                0
            } else if (a.hashCode() - b.hashCode() > 0) 1 else -1
        }
    }

    private class DistanceCalculator(private val location: APILocation) {
        private val tempLocation = Location("PurpleAir")

        fun calculateDistanceTo(sensor: PASensor): Float {
            try {
                val dist = sensor.distanceMeters
                if (dist >= 0) return dist

                tempLocation.latitude = sensor!!.latitude!!
                tempLocation.longitude = sensor.longitude!!
                tempLocation.time = System.currentTimeMillis()
                val distanceTo = floatArrayOf(0f)
                Location.distanceBetween(
                    location.latitude,
                    location.longitude,
                    tempLocation.latitude,
                    tempLocation.longitude,
                    distanceTo
                )

                sensor.distanceMeters = distanceTo[0]
//                Log.d(TAG, "sensor: ${sensor.name} ${sensor.distanceMeters}")
                return distanceTo[0]
            } catch (e: NullPointerException) {
                Log.e(
                    TAG, "Got invalid sensor coordinates for $sensor"
                )
                return Float.MAX_VALUE
            }
        }
    }
}
