/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ofd.digital.alpha

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.graphics.toRectF
import androidx.wear.watchface.*
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.WatchFaceLayer
import com.ofd.complications.LocationTest
import com.ofd.digital.alpha.location.LocationViewModel
import com.ofd.digital.alpha.location.ResolvedLocation
import com.ofd.digital.alpha.utils.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*

// Default for how long each frame is displayed at expected frame rate.
private const val FRAME_PERIOD_MS_DEFAULT: Long = 250

val dtf = DateTimeFormatter.ofPattern("EEE dd")


/**
 * Renders watch face via data in Room database. Also, updates watch face state based on setting
 * changes by user via [userStyleRepository.addUserStyleListener()].
 */
class DigitalWatchCanvasRenderer(
    private val context: Context,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    private val complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<DigitalWatchCanvasRenderer.DigitalSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    FRAME_PERIOD_MS_DEFAULT,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
) {

    private var locationViewModel: LocationViewModel? = null

    class DigitalSharedAssets : SharedAssets {
        override fun onDestroy() {
        }
    }

    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch {
            currentUserStyleRepository.userStyle.collect { userStyle ->
                updateWatchFaceData(userStyle)
            }
        }
    }

    override suspend fun createSharedAssets(): DigitalSharedAssets {
        return DigitalSharedAssets()
    }

    /*
     * Triggered when the user makes changes to the watch face through the settings activity. The
     * function is called by a flow.
     */
    private fun updateWatchFaceData(userStyle: UserStyle) {
        Log.d(TAG, "updateWatchFace(): $userStyle")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        scope.cancel("AnalogWatchCanvasRenderer scope clear() request")
        super.onDestroy()
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: DigitalSharedAssets
    ) {
        val hl = renderParameters.highlightLayer
        if (hl != null) {
            canvas.drawColor(hl.backgroundTint)
        }

        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: DigitalSharedAssets
    ) {
//        Log.d(TAG, "render()")
        var now = System.currentTimeMillis()
        var delay = if(lastLocation.get()==null) 15000 else 60000
        if (now - lastTime.get() > delay && renderParameters.drawMode == DrawMode.INTERACTIVE) {
            lastTime.set(now)
            scope.launch {
                Log.d(WhereAmIActivity.TAG, "render:launch()")
                val cc = callcnt.incrementAndGet()
                if (locationViewModel == null) {
                    locationViewModel = LocationViewModel("Watch.render", context)
                }
                val location = locationViewModel!!.readLocationResult()

                val text = if (location is ResolvedLocation) {
                    lastLocation.set(WatchLocation(cc, successcnt.incrementAndGet(), location))
                    "You are at: " + location.getAddressDescription() + ":" +
                        location.getTimeAgo()
                } else {
                    val ll = lastLocation.get()
                    if(ll!=null) lastLocation.set(WatchLocation(cc, ll.successcnt, ll.location))
                    "Error getting location"
                }
                Log.d(WhereAmIActivity.TAG, "render:launch():location=" + text)
                LocationTest.forceComplicationUpdate(context)
            }
        }
        val backgroundColor = if (renderParameters.drawMode == DrawMode.AMBIENT) {
            Color.BLACK
        } else {
            0xFF000040.toInt()
        }

        canvas.drawColor(backgroundColor)

        // CanvasComplicationDrawable already obeys rendererParameters.
        drawComplications(canvas, bounds, zonedDateTime)

        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS_OVERLAY)) {
            drawTime(canvas, bounds, zonedDateTime)
        }



        if (renderParameters.drawMode == DrawMode.INTERACTIVE &&
            renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)
        ) {
            // Base layer, if any
        }
    }

    // ----- All drawing functions -----
    private fun drawComplications(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
        for ((_, complication) in complicationSlotsManager.complicationSlots) {
//            Log.d(TAG,"Complication: " + complication.id +":"+complication.enabled)
            if (complication.enabled) {
//                Log.d(
//                    TAG,
//                    "  Enabled: " + complication.id + ":" + complication.complicationData.value.type
//                )
                if (complication.id <= COMPLICATION_4) {
                    val v = complication.complicationData.value
                    if (v is SmallImageComplicationData) {
                        Log.d(TAG, "SI = ")
                    } else {
                        drawSmallTextAndIcon(complication, canvas, zonedDateTime)
                    }
                } else if (complication.id == COMPLICATION_5) {
                    drawLongText(complication, zonedDateTime, canvas)
                } else if (
                    ((COMPLICATION_6 <= complication.id && complication.id <= COMPLICATION_9)
                        || complication.id == COMPLICATION_12)
                    && renderParameters.drawMode == DrawMode.INTERACTIVE
                ) {
                    drawIcon(complication, canvas)
                } else if (COMPLICATION_10 <= complication.id && complication.id <= COMPLICATION_11) {
                    drawArc(complication, zonedDateTime, canvas)
                } else if (renderParameters.drawMode == DrawMode.INTERACTIVE) {
                    Log.d(
                        TAG,
                        "something else: bounds: + " + complication.computeBounds(
                            Rect(
                                0,
                                0,
                                canvas.width,
                                canvas.height
                            )
                        )
                    )
                    complication.render(canvas, zonedDateTime, renderParameters)
                }
            }
        }
    }

    private fun drawArc(
        complication: ComplicationSlot,
        zonedDateTime: ZonedDateTime,
        canvas: Canvas
    ) {
        val d = complication.complicationData.value
        if (d is RangedValueComplicationData) {
//            Log.d(TAG, "Range Value mono image: " + d.monochromaticImage)
//            Log.d(TAG, "Range Value text: " + getText(d.text, zonedDateTime))
//            Log.d(TAG, "Range Value title: " + getText(d.title, zonedDateTime))
//            Log.d(TAG, "Range Value dexcription: " + getText(d.contentDescription, zonedDateTime))
            val cbounds =
                complication.computeBounds(Rect(0, 0, canvas.width, canvas.height)).toRectF()
            val arcBackgroundPaint = getCompPaint(complication.id, renderParameters.drawMode)

            val instructions = getText(d.contentDescription, zonedDateTime)
            val inx = instructions.indexOf("#Color:")
//            Log.d(TAG, "txt="+instructions+":"+inx)
            val offset = if (inx > 0) {
//                Log.d(TAG, "part:"+instructions.substring(inx+"#Color:".length))
                (Integer.parseInt(instructions.substring(inx + "#Color:".length)) + 2) * 1000
            } else {
                2000
            }
            val selector = if (offset == 2000) {
                complication.id + 2000
            } else {
                offset
            }
            val arcValuePaint = getCompPaint(selector, renderParameters.drawMode)
            val off = arcValuePaint.strokeWidth / 2f * 1.1f *
                if (complication.id == COMPLICATION_10) 1 else 1
            val bounds = RectF(
                cbounds.left + off,
                cbounds.top + off,
                cbounds.right - 1f * off,
                cbounds.bottom - 1f * off
            )
            val valueSweepAngle = OFD.sweepAngle * Math.min(1f, (d.value - d.min) / (d.max - d.min))

            val (backgroundStart, backgroundSweep) = if (complication.id == COMPLICATION_10) {
                arrayOf(OFD.c10startAngle, OFD.c10sweepAngle)
            } else {
                arrayOf(OFD.c11startAngle, OFD.c11sweepAngle)
            }

            val (valueStart, valueSweep) = if (complication.id == COMPLICATION_10) {
                arrayOf(backgroundStart, -valueSweepAngle)
            } else {
                arrayOf(backgroundStart, valueSweepAngle)
            }

            val (textStart, textSweep) = if (complication.id == COMPLICATION_10) {
                arrayOf(OFD.c10textStartAngle, OFD.c10textSweepAngle)
            } else {
                arrayOf(backgroundStart, backgroundSweep)
            }

            // Draw it on the canvas.
            canvas.drawArc(bounds, backgroundStart, backgroundSweep, false, arcBackgroundPaint)
            canvas.drawArc(bounds, valueStart, valueSweep, false, arcValuePaint)

            val textPaint2 = getCompPaint(complication.id + 1000, renderParameters.drawMode)
            val textPath = Path()
//            Log.d(TAG, "Path: " + bounds + ":" + textStart + ":" + textSweep)
            textPath.addArc(bounds, textStart, textSweep)
            var t = getText(d.contentDescription, zonedDateTime)
            if (t.length == 0)
                t = getText(d.text, zonedDateTime)
            val ix = t.indexOf("#")
            if (ix > 0) t = t.substring(0, ix)
            canvas.drawTextOnPath(
                t,
                textPath,
                0f,
                8f,
                textPaint2
            )
        }
    }

    private fun getText(ct: ComplicationText?, zonedDateTime: ZonedDateTime) =
        if (ct == null) {
            "null"
        } else {
            ct.getTextAt(context.resources, zonedDateTime.toInstant()).toString()
        }
//    @SuppressLint("RestrictedApi")
//    private fun drawRangeIcon(canvas: Canvas, complicationData: ComplicationData) {
//        var i: Icon? = null
//        Log.d(TAG, "Range Icon: " + complicationData.icon)
//        Log.d(TAG, "Range smallImage: " + complicationData.smallImage)
//        Log.d(TAG, "Range largeImage: " + complicationData.largeImage)
//        Log.d(TAG, "Range burnInProtectionSmallImage: " + complicationData.burnInProtectionIcon)
//        Log.d(TAG, "Range burnInProtectionIcon: " + complicationData.hasBurnInProtectionIcon())
//        if (complicationData.icon != null) {
//            i = complicationData.icon
//        } else if (complicationData.smallImage != null) {
//            i = complicationData.smallImage
//        } else if (complicationData.largeImage != null) {
//            i = complicationData.largeImage
//        } else if (complicationData.burnInProtectionSmallImage != null) {
//            i = complicationData.burnInProtectionSmallImage
//        } else if (complicationData.burnInProtectionIcon != null) {
//            i = complicationData.burnInProtectionIcon
//        }
////        if (i != null) {
////            i.setTint(primaryPaint.getColor())
////            val icon = i.loadDrawable(context)
////            icon!!.bounds = iconBounds
////            icon.draw(canvas)
////        }
//    }


    private fun drawIcon(
        complication: ComplicationSlot,
        canvas: Canvas
    ) {
        val textPaint = getCompPaint(complication.id, renderParameters.drawMode)
        val bounds = complication.computeBounds(Rect(0, 0, canvas.width, canvas.height))
        //Log.d(TAG, "bounds: " + complication.id + ":"+ bounds)
        val left = bounds.left.toFloat()
        val bottom = bounds.bottom.toFloat()
        val d = complication.complicationData.value
        val h = bounds.bottom - bounds.top
        if (d is SmallImageComplicationData) {
            val image = d.smallImage.image
            val drawable = image?.loadDrawable(context)
            if (drawable != null) {
                var imagebounds =
                    if (complication.id % 2 == 1) {
                        Rect(bounds.right - h, bounds.top, bounds.right, bounds.bottom)
                    } else {
                        Rect(bounds.left, bounds.top, bounds.left + h, bounds.bottom)
                    }
                drawable.bounds = imagebounds
                canvas.save()
                val r = h.toFloat() / 2
                val x = imagebounds.right.toFloat() - r
                val y = imagebounds.bottom.toFloat() - r
                val path = Path().apply {
                    addCircle(x, y, r, Path.Direction.CW)
                }
                //                            Log.d(TAG, "Path: " + path + "\n  bounds=" + imagebounds)
                canvas.clipPath(path)
                canvas.drawCircle(x, y, r, OFD.blackBackground)
                drawable.draw(canvas)
                canvas.restore()
            }
        }
    }

    private fun drawLongText(
        complication: ComplicationSlot,
        zonedDateTime: ZonedDateTime,
        canvas: Canvas
    ) {
        //                    Log.d(TAG, "Complication 5: " + complication.javaClass)
        //                    Log.d(TAG, "\t" + complication.complicationData.javaClass)
        //                    Log.d(TAG, "\t" + complication.complicationData.toString())
        val d = complication.complicationData.value
        //                    Log.d(TAG, "\t" + d.toString())
        //                    Log.d(TAG, "\t" + d.javaClass)
        //                    Log.d(TAG, "\t" + d.dataSource)
        if (d is LongTextComplicationData) {
            val ld = d as LongTextComplicationData
            //                        Log.d(TAG, "\t" + ld.contentDescription!!.getTextAt(context.resources, Instant.now()))
            //                        Log.d(TAG, "\t" + ld.title)
            //                        Log.d(TAG, "\t" + ld.asWireComplicationData())
            val text = d.text.getTextAt(context.resources, zonedDateTime.toInstant())
                .toString()
            val bounds =
                complication.computeBounds(Rect(0, 0, canvas.width, canvas.height))
            val textPaint = getCompPaint(complication.id, renderParameters.drawMode)
            canvas.save()
            canvas.clipRect(bounds)
            val textBounds = Rect()
            var bottom = bounds.top
            if (!text.contains("\n")) {
                var s = text
                while (s.length > 0) {
                    val l = textPaint.breakText(s, true, bounds.width().toFloat(), null)
                    textPaint.getTextBounds(s, 0, l, textBounds)
                    bottom += textBounds.bottom - textBounds.top
                    canvas.drawText(
                        s.substring(0, l),
                        bounds.left.toFloat(),
                        bottom.toFloat(),
                        textPaint
                    )
                    bottom += 4
                    s = s.substring(l)
                }
            } else {
                for (t in text.split("\n")) {
                    textPaint.getTextBounds(t, 0, t.length, textBounds)
                    bottom += textBounds.bottom - textBounds.top
                    //                            Log.d(TAG, "Line: " + bottom + ":" + textBounds + ":" + bounds +":"+t)
                    canvas.drawText(
                        t,
                        bounds.left.toFloat(),
                        bottom.toFloat(),
                        textPaint
                    )
                    bottom += 4
                    if (bottom > bounds.bottom)
                        break
                }
            }
            canvas.restore()
            //                        Log.d(TAG, "Long Text: " + text);
            //                        complication.render(canvas, zonedDateTime, renderParameters)

        } else {
            //                        Log.d(TAG, "value: " + d)
            //                        Log.d(TAG, "value2:" + d.dataSource)
            complication.render(canvas, zonedDateTime, renderParameters)
        }
    }

    private fun drawSmallTextAndIcon(
        complication: ComplicationSlot,
        canvas: Canvas,
        zonedDateTime: ZonedDateTime
    ) {
        val bounds = complication.computeBounds(Rect(0, 0, canvas.width, canvas.height))

        canvas.drawRoundRect(
            bounds.toRectF(),
            bounds.height() * .2f,
            bounds.height() * .2f,
            OFD.blackBackground
        )

//        Log.d(TAG, "smallTextAndIcon: " + complication.complicationData.value.dataSource?.className)
        //Log.d(TAG, "bounds: " + complication.id + ":"+ bounds)

        val left = bounds.left.toFloat()
        val bottom = bounds.bottom.toFloat()
        val d = complication.complicationData.value
        val h = bounds.bottom - bounds.top
        if (d is ShortTextComplicationData) {
            val textAt = d.text.getTextAt(context.resources, zonedDateTime.toInstant()).toString()
            val image = d.monochromaticImage?.image
            val drawable = image?.loadDrawable(context)
            var textClipBounds = bounds
            val ds = complication.complicationData.value.dataSource
            if (ds != null && ds.className.endsWith("BatteryProviderService")) {
                val batteryPaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.GRAY
                    style = Paint.Style.FILL_AND_STROKE
                    strokeCap = Paint.Cap.BUTT
                    strokeWidth = h / 4f
                }

                val pct = Integer.parseInt(textAt).toFloat()
                val cut = (1f - pct / 100f) * .8f * h + .1f * h
                val fillcolor =
                    if (pct > 25f) Color.GREEN else if (pct > 10f) Color.YELLOW else Color.RED

                if (cut < .3f * h) {
                    canvas.drawLine(
                        bounds.left + h / 2f, bounds.top.toFloat() + .1f * h,
                        bounds.left + h / 2f, bounds.top.toFloat() + cut, batteryPaint
                    )
                    batteryPaint.color = fillcolor
                    canvas.drawLine(
                        bounds.left + h / 2f, bounds.top.toFloat() + cut,
                        bounds.left + h / 2f, bounds.top.toFloat() + .4f * h, batteryPaint
                    )
                    batteryPaint.strokeWidth = h / 2.5f
                    canvas.drawLine(
                        bounds.left + h / 2f, bounds.top.toFloat() + .3f * h,
                        bounds.left + h / 2f, bounds.bottom.toFloat() - .1f * h, batteryPaint
                    )
                } else {
                    canvas.drawLine(
                        bounds.left + h / 2f, bounds.top.toFloat() + .1f * h,
                        bounds.left + h / 2f, bounds.top.toFloat() + .4f * h, batteryPaint
                    )
                    batteryPaint.strokeWidth = h / 2.5f
                    canvas.drawLine(
                        bounds.left + h / 2f, bounds.top.toFloat() + .3f * h,
                        bounds.left + h / 2f, bounds.top.toFloat() + cut, batteryPaint
                    )
                    batteryPaint.color = fillcolor
                    canvas.drawLine(
                        bounds.left + h / 2f, bounds.top.toFloat() + cut,
                        bounds.left + h / 2f, bounds.bottom.toFloat() - .1f * h, batteryPaint
                    )
                }

                textClipBounds = if (complication.id % 2 == 1) {
                    Rect(bounds.left, bounds.top, bounds.right - h, bounds.bottom)
                } else {
                    Rect(bounds.left + h, bounds.top, bounds.right, bounds.bottom)
                }
            } else if (drawable != null) {
                var imagebounds =
                    if (complication.id % 2 == 1) {
                        Rect(bounds.right - h, bounds.top, bounds.right, bounds.bottom)
                    } else {
                        Rect(bounds.left, bounds.top, bounds.left + h, bounds.bottom)
                    }
                drawable.bounds = imagebounds
                drawable.draw(canvas)
                textClipBounds = if (complication.id % 2 == 1) {
                    Rect(bounds.left, bounds.top, bounds.right - h, bounds.bottom)
                } else {
                    Rect(bounds.left + h, bounds.top, bounds.right, bounds.bottom)
                }
            }
            var textBounds = Rect()
            var textPaint = getCompPaint(complication.id, renderParameters.drawMode)
            if (textAt.toString().length > 3) {
                val size = textPaint.textSize * .8f
                textPaint = Paint(textPaint).apply { textSize = size }
            }
            textPaint.getTextBounds(
                textAt.toString(),
                0,
                textAt.toString().length,
                textBounds
            )
            var v = h + h / 4
            var y = bounds.bottom - h / 2 + textBounds.height() / 2
            var x = if (complication.id % 2 == 1) {
                bounds.right - v - textBounds.width()
            } else {
                bounds.left + v
            }
            canvas.save()
            canvas.clipRect(textClipBounds)
            canvas.drawText(
                textAt.toString(),
                x.toFloat(),
                y.toFloat(),
                textPaint
            )
            canvas.restore()
        }
    }

    private fun getCompPaint(complicationId: Int, drawMode: DrawMode): Paint {
//        val r = OFD.complicationPaint.get(complicationId)!!.get(drawMode)
//        if (r != null)
//            return r
        val m1 = OFD.complicationPaint.get(complicationId)
        if (m1 != null) {
            val m2 = m1.get(DrawMode.INTERACTIVE)
            if (m2 != null) return m2
        }
        return Paint()
    }

    private fun drawTime(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime
    ) {
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        val h = cx * 2
        val g5 = OFD.dateTimeGapP * h
        val datetop = OFD.dateClockGapP * h
        val datebottom = OFD.dateBottomP * h
        val datecx = cx
        val datecy = (datebottom - datetop) / 2 + datetop

        val tt = datebottom + g5
        val th = OFD.timeHeightP * h
        val tb = tt + th

        val textBounds = Rect()
        val date = zonedDateTime.format(dtf)
        OFD.textPaint.getTextBounds(date, 0, date.length, textBounds)
        canvas.drawRoundRect(
            datecx - textBounds.width() / 1.8f,
            datecy - textBounds.height() / 1.6f,
            datecx + textBounds.width() / 1.7f,
            datecy + textBounds.height() / 1.6f,
            textBounds.height() * .2f,
            textBounds.height() * .2f,
            OFD.blackBackground
        )
        canvas.drawText(
            date,
            datecx - textBounds.width() / 2.0f,
            datecy + textBounds.height() / 2.0f,
            OFD.textPaint
        )
//        canvas.drawText(
//            date,
//            datecx - textBounds.width() / 2.0f,
//            datecy + textBounds.height() / 2.0f,
//            OFD.textPaintStroke
//        )

        val tcx = cx
        val tcy = th / 2 + tt
        var time = zonedDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM))
        time = time.substring(0, time.length - 3)
        OFD.timePaint.getTextBounds("00:00:00", 0, time.length, textBounds)
        val fullWidth = textBounds.width()
        val fullHeight = textBounds.height()
        OFD.timePaint.getTextBounds(time, 0, time.length, textBounds)
        canvas.drawRoundRect(
            tcx - fullWidth / 1.8f,
            tcy - fullHeight / 1.6f,
            tcx + fullWidth / 1.7f,
            tcy + fullHeight / 1.6f,
            fullHeight * .2f,
            fullHeight * .2f,
            OFD.blackBackground
        )
        canvas.drawText(
            time,
            tcx - fullWidth / 2.0f,
            tcy + textBounds.height() / 2.0f,
            OFD.timePaint
        )
    }

    class WatchLocation(
        val callcnt: Int,
        val successcnt: Int,
        val location: ResolvedLocation
    )

    companion object {
        private const val TAG = "OFDDigitalRenderer"

        val lastTime = AtomicLong(0)
        val callcnt = AtomicInteger(0)
        val successcnt = AtomicInteger(0)
        val lastLocation = AtomicReference<WatchLocation>()
    }
}
