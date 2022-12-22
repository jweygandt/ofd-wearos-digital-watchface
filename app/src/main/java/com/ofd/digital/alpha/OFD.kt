package com.ofd.digital.alpha

import android.graphics.Color
import android.graphics.Paint
import androidx.wear.watchface.DrawMode
import com.ofd.digital.alpha.utils.*

class OFD {
    companion object {
        // date top margin percent
        public const val dateClockGapP = .057f

        // date bottom margin percent
        public const val dateBottomP = .114f

        // gap date to time percent
        public const val dateTimeGapP = .028f

        // time height percent
        public const val timeHeightP = .200f

        // diagnol margin to first complication
        public const val bottomRoundClockGapP = .000f

        // c1/c3 spacing
        public const val c1C3GapP = .014f

        // bottom icon gap
        public const val c7C8GapP = c1C3GapP * 2f

        // c1/c2 spaciing
        public const val C12GapP = .028f

        // c1/c2 extra bottom space
        public const val c1WatchGapExtraP = .08f

        // c1-4 width and height
        public const val c1WidthP = .270f
        public const val C1HeightP = .110f

        // c3/c5 spacing
        public const val c3C5GapP = .028f

        // c5/time spacing
        public const val timeC5GapP = .028f

        // c5 width
        public const val c5WidthP = .630f

        // c6,c7 radius
        public const val c6RadiusP = C1HeightP / 2f * 1.3f

        // c1,c6 gap
        public const val c1C6GapP = C12GapP / 4f

        // c6,c7 gap (see #3)
        public const val g67 = C12GapP * 1.5f

        public const val startAngle = -25f
        public const val sweepAngle = 40f

        public const val arcStroke = 34f
        public const val arcText = 32f

        // The arc sweeps up, text sweepsa down
        public const val c10startAngle = startAngle + sweepAngle
        public const val c10sweepAngle = -sweepAngle
        public const val c10textStartAngle = startAngle
        public const val c10textSweepAngle = sweepAngle

        public const val c11startAngle = 180f - 15f
        public const val c11sweepAngle = sweepAngle

//        @RequiresApi(Build.VERSION_CODES.Q)
//        val blend = BlendMode.

        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 40f
            isFakeBoldText = true
            color = 0xFF00FF00.toInt()
        }

//        val textPaintStroke = Paint().apply {
//            isAntiAlias = true
//            textSize = 40f
//            isFakeBoldText = true
//            color = Color.BLACK
//            style = Paint.Style.STROKE
//            strokeWidth = 2f
//        }

        val timePaint = Paint().apply {
            isAntiAlias = true
            textSize = 80f
            isFakeBoldText = true
            color = Color.WHITE
//            blendMode = blend
        }

        val blackBackground = Paint().apply {
            style = Paint.Style.FILL_AND_STROKE
            color = Color.BLACK
        }

        private val cts = 35f
        private val ctss = cts * .75f

        val iconPaint = mapOf(
            DrawMode.INTERACTIVE to Paint().apply {
                isAntiAlias = true
                textSize = cts
                isFakeBoldText = true
                color = 0xFFA0A0FF.toInt()
            },
            DrawMode.AMBIENT to Paint().apply {
                isAntiAlias = true
                textSize = cts
                isFakeBoldText = true
                color = 0xFFA0A0FF.toInt() / 2 and 0xFF7F7F7F.toInt()
            })

        val c11paint = Paint ().apply {
            isAntiAlias = true
            textSize = ctss
            isFakeBoldText = true
            color = 0xFFA0A0FF.toInt()
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = arcStroke
        }

        val complicationPaint = mapOf(
//            COMPLICATION_12 to iconPaint,
            COMPLICATION_1 to mapOf(
                DrawMode.INTERACTIVE to Paint().apply {
                    isAntiAlias = true
                    textSize = cts
                    isFakeBoldText = true
                    color = Color.GREEN
                },
                DrawMode.AMBIENT to Paint().apply {
                    isAntiAlias = true
                    textSize = cts
                    isFakeBoldText = true
                    color = Color.GREEN / 2 and 0xFF7F7F7F.toInt()
                }),
            COMPLICATION_2 to mapOf(
                DrawMode.INTERACTIVE to Paint().apply {
                    isAntiAlias = true
                    textSize = cts
                    isFakeBoldText = true
                    color = Color.RED
                },
                DrawMode.AMBIENT to Paint().apply {
                    isAntiAlias = true
                    textSize = cts
                    isFakeBoldText = true
                    color = Color.RED / 2 and 0xFF7F7F7F.toInt()
                }),
            COMPLICATION_3 to mapOf(
                DrawMode.INTERACTIVE to Paint().apply {
                    isAntiAlias = true
                    textSize = cts
                    isFakeBoldText = true
                    color = Color.MAGENTA
                },
                DrawMode.AMBIENT to Paint().apply {
                    isAntiAlias = true
                    textSize = cts
                    isFakeBoldText = true
                    color = Color.MAGENTA / 2 and 0xFF7F7F7F.toInt()
                }),
            COMPLICATION_4 to mapOf(
                DrawMode.INTERACTIVE to Paint().apply {
                    isAntiAlias = true
                    textSize = cts
                    isFakeBoldText = true
                    color = 0xFFA0A0FF.toInt()
                },
                DrawMode.AMBIENT to Paint().apply {
                    isAntiAlias = true
                    textSize = cts
                    isFakeBoldText = true
                    color = 0xFFA0A0FF.toInt() / 2 and 0xFF7F7F7F.toInt()
                }),
            COMPLICATION_5 to mapOf(
                DrawMode.INTERACTIVE to Paint().apply {
                    isAntiAlias = true
                    textSize = ctss
                    isFakeBoldText = true
                    color = Color.GREEN
                },
                DrawMode.AMBIENT to Paint().apply {
                    isAntiAlias = true
                    textSize = ctss
                    isFakeBoldText = true
                    color = Color.GREEN / 2 and 0xFF7F7F7F.toInt()
                }),
            COMPLICATION_6 to iconPaint,
            COMPLICATION_7 to iconPaint,
            COMPLICATION_8 to iconPaint,
            COMPLICATION_9 to iconPaint,

            COMPLICATION_10 to mapOf(
                DrawMode.INTERACTIVE to Paint(c11paint).apply
                {
                    color = Color.LTGRAY
                },
                DrawMode.AMBIENT to Paint(c11paint).apply
                {
                    color = Color.LTGRAY / 2 and 0xFF7F7F7F.toInt()
                }),
            COMPLICATION_10 + 1000 to mapOf(
                DrawMode.INTERACTIVE to Paint().apply {
                    isAntiAlias = true
                    textSize = arcText
                    isFakeBoldText = false
                    color = Color.BLACK
                    textAlign = Paint.Align.CENTER
                    style = Paint.Style.FILL_AND_STROKE
                    strokeWidth = 1f
                },
                DrawMode.AMBIENT to Paint().apply {
                    isAntiAlias = true
                    textSize = arcText
                    isFakeBoldText = false
                    color = Color.BLACK / 2 and 0xFF7F7F7F.toInt()
                    textAlign = Paint.Align.CENTER
                    style = Paint.Style.FILL_AND_STROKE
                    strokeWidth = 1f
                }),

            COMPLICATION_10 + 2000 to mapOf(
                DrawMode.INTERACTIVE to Paint(c11paint).apply
                {
                    color = 0xFFA0A0FF.toInt()
                },
                DrawMode.AMBIENT to Paint(c11paint).apply
                {
                    color = 0xFFA0A0FF.toInt() / 2 and 0xFF7F7F7F.toInt()
                }),

            COMPLICATION_11 to mapOf(
                DrawMode.INTERACTIVE to Paint(c11paint).apply
                {
                    color = Color.LTGRAY
                },
                DrawMode.AMBIENT to Paint(c11paint).apply
                {
                    color = Color.LTGRAY / 2 and 0xFF7F7F7F.toInt()
                }),
            COMPLICATION_11 + 1000 to mapOf(
                DrawMode.INTERACTIVE to Paint().apply
                {
                    isAntiAlias = true
                    textSize = arcText
                    isFakeBoldText = false
                    color = Color.BLACK
                    textAlign = Paint.Align.CENTER
                    style = Paint.Style.FILL_AND_STROKE
                    strokeWidth = 1f
                },
                DrawMode.AMBIENT to Paint().apply
                {
                    isAntiAlias = true
                    textSize = arcText
                    isFakeBoldText = false
                    color = Color.BLACK / 2 and 0xFF7F7F7F.toInt()
                    textAlign = Paint.Align.CENTER
                    style = Paint.Style.FILL_AND_STROKE
                    strokeWidth = 1f
                }),
            COMPLICATION_11 + 2000 to mapOf(
                DrawMode.INTERACTIVE to Paint(c11paint).apply
                {
                    color = 0xFFA0A0FF.toInt()
                },
                DrawMode.AMBIENT to Paint(c11paint).apply
                {
                    color = 0xFFA0A0FF.toInt() / 2 and 0xFF7F7F7F.toInt()
                }),


            3000 to mapOf(
                DrawMode.INTERACTIVE to Paint(c11paint).apply
                {
                    color = Color.YELLOW
                },
                DrawMode.AMBIENT to Paint(c11paint).apply
                {
                    color = Color.YELLOW / 2 and 0xFF7F7F7F.toInt()
                }),
             4000 to mapOf(
                DrawMode.INTERACTIVE to Paint(c11paint).apply
                {
                    color = Color.YELLOW
                },
                DrawMode.AMBIENT to Paint(c11paint).apply
                {
                    color = Color.YELLOW / 2 and 0xFF7F7F7F.toInt()
                }),
             5000 to mapOf(
                DrawMode.INTERACTIVE to Paint(c11paint).apply
                {
                    color = 0xFFFFA500.toInt() // Orange
                },
                DrawMode.AMBIENT to Paint(c11paint).apply
                {
                    color = 0xFFFFA500.toInt() / 2 and 0xFF7F7F7F.toInt()
                }),
             6000 to mapOf(
                DrawMode.INTERACTIVE to Paint(c11paint).apply
                {
                    color = Color.RED
                },
                DrawMode.AMBIENT to Paint(c11paint).apply
                {
                    color = Color.RED / 2 and 0xFF7F7F7F.toInt()
                }),
             7000 to mapOf(
                DrawMode.INTERACTIVE to Paint(c11paint).apply
                {
                    color = 0xFFA020F0.toInt() // purple
                },
                DrawMode.AMBIENT to Paint(c11paint).apply
                {
                    color = 0xFFA020F0.toInt() / 2 and 0xFF7F7F7F.toInt()
                }),
             8000 to mapOf(
                DrawMode.INTERACTIVE to Paint(c11paint).apply
                {
                    color = 0xFF800000.toInt() // Maroon
                },
                DrawMode.AMBIENT to Paint(c11paint).apply
                {
                    color = 0xFF800000.toInt() / 2 and 0xFF7F7F7F.toInt()
                }),
        )
    }
}
