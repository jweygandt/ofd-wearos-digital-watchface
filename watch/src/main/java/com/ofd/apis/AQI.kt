package com.ofd.apis

import com.ofd.watchface.location.ResolvedLocation
import com.thanglequoc.aqicalculator.AQICalculator
import com.thanglequoc.aqicalculator.Pollutant


private val calculator: AQICalculator = AQICalculator.getAQICalculatorInstance()

sealed class AQIResult {
    class AQI : AQIResult {

        constructor(
            source: String,
            metrics: APIMetrics,
            location: APILocation,
            fulljsonn: String,
            date: Long,
            color: Int,
            comps: Map<String, Float>
        ){
            this.source = source
            this.metrics = metrics
            this.location = location
            this.fulljsonn = fulljsonn
            this.date = date
            this.color = color
            this.comps = comps
            if(comps.containsKey("pm2_5")) {
                this.aqippm = calculator.getAQI(
                    Pollutant.PM25, comps.get("pm2_5")!!.toDouble()).aqi
                this.aqistr = aqippm.toString() + " ppm"
            }else{
                this.aqippm=0
                this.aqistr="??"
            }
        }

        val source: String
        val metrics: APIMetrics
        val location: APILocation
        val fulljsonn: String
        val date: Long
        val color: Int
        val comps: Map<String, Float>
        val aqippm: Int
        val aqistr: String

        val address: String? = null

        fun colorInxForComp(comp: String, v: Float): Int {
            fun inx(f0: Float, f1: Float, f2: Float, f3: Float, f4: Float = f3): Int {
                return if (v <= f0) 0 else if (v <= f1) 1 else if (v <= f2) 2 else if (v <= f3) 3 else if (v <= f4) 4 else 5
            }
            return when (comp) {
                "co" -> {
                    inx(1f, 2f, 10f, 17f, 34f)
                }
                "no" -> {
                    0
                }
                "no2" -> {
                    inx(50f, 100f, 200f, 400f)
                }
                "o3" -> {
                    inx(60f, 120f, 180f, 240f)
                }
                "so2" -> {
                    inx(40f, 80f, 380f, 800f, 1600f)
                }
                "pm2_5" -> {
                    inx(15f, 30f, 55f, 110f)
                }
                "pm10" -> {
                    inx(25f, 50f, 90f, 180f)
                }
                "nh3" -> {
                    inx(200f, 400f, 800f, 1200f, 1800f)
                }
                else -> 5
            }
        }

        fun statusString() =
            source + " " + metrics.metricNumCalls.get() + "/" + (metrics.metricNumSuccess.get() + metrics.metricNumBypass.get()) + ":" + metrics.metricNumBypass.get() + ":" + metrics.metricNumErrors.get() + ":" + metrics.metricNumConflict.get()
    }

    class Error(val source: String, val msg: String) : AQIResult()
}
