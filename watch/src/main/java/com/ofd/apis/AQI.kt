package com.ofd.apis

import androidx.compose.ui.graphics.Color
import com.thanglequoc.aqicalculator.AQICalculator

interface AQIDetails {
    val shortText: String
    val rangeValue: Float
    val rangeText: String
}

sealed class AQIResult<T : AQIDetails> {

    abstract val shortText: String
    abstract val rangeValue: Float
    abstract val rangeText: String
    abstract val color: Int

    class AQI<T : AQIDetails>(val source: String, val details: T, val metrics: APIMetrics) :
        AQIResult<T>() {

        fun statusString() =
            source + " " + metrics.metricNumCalls.get() + "/" + (metrics.metricNumSuccess.get() + metrics.metricNumBypass.get()) + ":" + metrics.metricNumBypass.get() + ":" + metrics.metricNumErrors.get() + ":" + metrics.metricNumConflict.get()

        override val shortText get() = details.shortText
        override val rangeValue get() = details.rangeValue
        override val rangeText get() = details.rangeText
        override val color get() = colorInxForComp("pm2_5", rangeValue)

        companion object {
            val aqiCalculator: AQICalculator = AQICalculator.getAQICalculatorInstance()

            fun colorInxForComp(comp: String, v: Float): Int {
                fun inx(f0: Float, f1: Float, f2: Float, f3: Float, f4: Float = f3): Int {
                    return if (v <= f0) 0 else if (v <= f1) 1 else if (v <= f2) 2 else if (v <= f3) 3 else if (v <= f4) 4 else 5
                }
                return when (comp) {
                    "aqi" -> {
                        inx(50f, 100f, 150f, 200f, 300f)
                    }
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
        }
    }

    class Error<T : AQIDetails>(val source: String, val msg: String) : AQIResult<T>() {
        override val shortText get() = msg
        override val rangeValue get() = 0f
        override val rangeText get() = "--"
        override val color get() = 0
    }
}

abstract class AQIService<T : AQIDetails> : APIService<AQIResult<T>>() {

    override fun makeErrorResult(s: String): AQIResult<T> {
        return AQIResult.Error(TAG, s)
    }

    override fun isErrorResult(r: AQIResult<T>): Boolean {
        return r is AQIResult.Error
    }
}
