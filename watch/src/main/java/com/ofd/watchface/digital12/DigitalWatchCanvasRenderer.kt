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
package com.ofd.watchface.digital12

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.graphics.toRectF
import androidx.wear.watchface.*
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.WatchFaceLayer
import com.google.android.gms.wearable.Wearable
import com.ofd.complications.ComplicationWrapper
import com.ofd.complications.Complications
import com.ofd.complications.VirtualComplicationPlayPauseImpl
import com.ofd.watchface.location.WatchLocationService
import com.ofd.watchface.vcomp.ComplicationSlotManagerHolder
import com.ofd.watchface.vcomp.VirtualComplicationWatchRenderSupport
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.min
import kotlinx.coroutines.*


// Default for how long each frame is displayed at expected frame rate.
private const val FRAME_PERIOD_MS_DEFAULT: Long = 250

val dtf: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE dd")


/**
 * Renders watch face via data in Room database. Also, updates watch face state based on setting
 * changes by user via [userStyleRepository.addUserStyleListener()].
 */
class DigitalWatchCanvasRenderer(
    override val context: Context,
    surfaceHolder: SurfaceHolder,
    val watchState: WatchState,
    private val complicationSlotManagerHolder: ComplicationSlotManagerHolder,
    override val currentUserStyleRepository: CurrentUserStyleRepository,
    canvasType: Int
) : Renderer.CanvasRenderer2<DigitalWatchCanvasRenderer.DigitalSharedAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    canvasType,
    FRAME_PERIOD_MS_DEFAULT,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false
), VirtualComplicationWatchRenderSupport, WatchFace.TapListener {

    var dateBounds: RectF? = null
    var dateHighlight = false

    class DigitalSharedAssets : SharedAssets {
        override fun onDestroy() {
        }
    }

    override val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )

    //    val dataClient by lazy { Wearable.getDataClient(context) }
    override val messageClient by lazy { Wearable.getMessageClient(context) }
    override val capabilityClient by lazy { Wearable.getCapabilityClient(context) }

    init {
        val renderer = this
        complicationSlotManagerHolder.watch = this
        scope.launch {
            currentUserStyleRepository.userStyle.collect { userStyle ->
                updateWatchFaceData(userStyle)
            }
        }
        scope.launch {
            watchState.isVisible.collect { v ->
                VirtualComplicationPlayPauseImpl.setWatchState(
                    renderer, v
                )
            }
        }
    }

    override fun onTapEvent(
        tapType: Int, tapEvent: TapEvent, complicationSlot: ComplicationSlot?
    ) {
        Log.d(
            TAG,
            "onTap: " + tapType + ":" + tapEvent.toString() + ":" + (complicationSlot?.id
                ?: -1).toString()
        )
        if (complicationSlot == null) {
            if (dateBounds!!.contains(tapEvent.xPos.toFloat(), tapEvent.yPos.toFloat())) {
                WatchLocationService.forceLocationUpdate()
                Complications.forceComplicationUpdate(context, complicationSlotManagerHolder)
                dateHighlight = true
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

        for (complication in complicationSlotManagerHolder.slotWrappers) {
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

        WatchLocationService.doOnRender(
            scope, context, renderParameters, complicationSlotManagerHolder
        )

        val backgroundColor = if (renderParameters.drawMode == DrawMode.AMBIENT) {
            Color.BLACK
        } else {
            0xFF000040.toInt()
        }

        canvas.drawColor(backgroundColor)

        // CanvasComplicationDrawable already obeys rendererParameters.
        drawComplications(canvas, zonedDateTime)

        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS_OVERLAY)) {
            drawTime(canvas, bounds, zonedDateTime)
        }


//        if (renderParameters.drawMode == DrawMode.INTERACTIVE && renderParameters.watchFaceLayers.contains(
//                WatchFaceLayer.BASE
//            )
//        ) {
//            // Base layer, if any
//        }
    }

    // ----- All drawing functions -----
    private fun drawComplications(canvas: Canvas, zonedDateTime: ZonedDateTime) {
        for (complication in complicationSlotManagerHolder.slotWrappers) {
//            Log.d(TAG,"Complication: " + complication.id +":"+complication.enabled)
            if (complication.enabled) {
//                Log.d(
//                    TAG,
//                    "  Enabled: " + complication.id + ":" + complication.complicationData.value.type
//                )
                if (complication.id <= COMPLICATION_4) {
                    drawSmallTextAndIcon(complication, canvas, zonedDateTime)
                } else if (complication.id == COMPLICATION_5) {
                    drawLongText(complication, zonedDateTime, canvas)
                } else if (((complication.id in COMPLICATION_6..COMPLICATION_9) || complication.id == COMPLICATION_12) && renderParameters.drawMode == DrawMode.INTERACTIVE) {
                    drawIcon(complication, canvas)
                } else if (complication.id in COMPLICATION_10..COMPLICATION_11) {
                    drawArc(complication, zonedDateTime, canvas)
                } else if (COMPLICATION_14 == complication.id) {
                    // ignore all rendering
                } else if (renderParameters.drawMode == DrawMode.INTERACTIVE) {
                    Log.d(
                        TAG, "something else: bounds: + " + complication.computeBounds(
                            Rect(
                                0, 0, canvas.width, canvas.height
                            )
                        )
                    )
                    complication.defaultRender(canvas, zonedDateTime, renderParameters)
                }
            }
        }
    }

    private fun drawArc(
        complicationWrapper: ComplicationWrapper, zonedDateTime: ZonedDateTime, canvas: Canvas
    ) {
        val complication = complicationWrapper.virtualComplication(
            this, zonedDateTime.toInstant()
        )
        val type = complication.type
        if (type == ComplicationType.RANGED_VALUE) {
//            Log.d(TAG, "Range Value mono image: " + d.monochromaticImage)
//            Log.d(TAG, "Range Value text: " + getText(d.text, zonedDateTime))
//            Log.d(TAG, "Range Value title: " + getText(d.title, zonedDateTime))
//            Log.d(TAG, "Range Value description: " + getText(d.contentDescription, zonedDateTime))
            val cbounds =
                complicationWrapper.computeBounds(Rect(0, 0, canvas.width, canvas.height)).toRectF()
            val arcBackgroundPaint = getCompPaint(complicationWrapper.id)

            val cinx = complication.color
            val offset = if (cinx >= 0) {
//                Log.d(TAG, "part:"+instructions.substring(inx+"#Color:".length))
                (cinx + 2) * 1000
            } else {
                2000
            }
            val selector = if (offset == 2000) {
                complicationWrapper.id + 2000
            } else {
                offset
            }
            val arcValuePaint = getCompPaint(selector)
            val off =
                arcValuePaint.strokeWidth / 2f * 1.1f * if (complicationWrapper.id == COMPLICATION_10) 1 else 1
            val bounds = RectF(
                cbounds.left + off,
                cbounds.top + off,
                cbounds.right - 1f * off,
                cbounds.bottom - 1f * off
            )
            val valueSweepAngle = D12.sweepAngle * min(
                1f,
                (complication.rangeValue - complication.rangeMin) / (complication.rangeMax - complication.rangeMin)
            )

            val (backgroundStart, backgroundSweep) = if (complicationWrapper.id == COMPLICATION_10) {
                arrayOf(D12.c10startAngle, D12.c10sweepAngle)
            } else {
                arrayOf(D12.c11startAngle, D12.c11sweepAngle)
            }

            val (valueStart, valueSweep) = if (complicationWrapper.id == COMPLICATION_10) {
                arrayOf(backgroundStart, -valueSweepAngle)
            } else {
                arrayOf(backgroundStart, valueSweepAngle)
            }

            val (textStart, textSweep) = if (complicationWrapper.id == COMPLICATION_10) {
                arrayOf(D12.c10textStartAngle, D12.c10textSweepAngle)
            } else {
                arrayOf(backgroundStart, backgroundSweep)
            }

            // Draw it on the canvas.
            canvas.drawArc(bounds, backgroundStart, backgroundSweep, false, arcBackgroundPaint)
            canvas.drawArc(bounds, valueStart, valueSweep, false, arcValuePaint)

            val textPaint2 = getCompPaint(complicationWrapper.id + 1000)
            val textPath = Path()
//            Log.d(TAG, "Path: " + bounds + ":" + textStart + ":" + textSweep)
            textPath.addArc(bounds, textStart, textSweep)
            var t = complication.text
            val ix = t.indexOf("?")
            if (ix > 0) t = t.substring(0, ix)
            canvas.drawTextOnPath(
                t, textPath, 0f, 8f, textPaint2
            )
        }
    }


    private fun drawIcon(
        complicationWrapper: ComplicationWrapper, canvas: Canvas
    ) {
        val complication = complicationWrapper.virtualComplication(
            this, null
        )
        val bounds = complicationWrapper.computeBounds(Rect(0, 0, canvas.width, canvas.height))
        //Log.d(TAG, "bounds: " + complicationWrapper.id + ":"+ bounds)
        val h = bounds.bottom - bounds.top
        if (complication.type == ComplicationType.SMALL_IMAGE) {
            val image = complication.image
            val drawable = image?.loadDrawable(context)
            if (drawable != null) {
                val imagebounds = if (complicationWrapper.id % 2 == 1) {
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
                canvas.drawCircle(x, y, r, D12.blackBackground)
                drawable.draw(canvas)
                canvas.restore()
            }
        }
    }

    private fun drawLongText(
        complicationWrapper: ComplicationWrapper, zonedDateTime: ZonedDateTime, canvas: Canvas
    ) {
        //                    Log.d(TAG, "Complication 5: " + complication.javaClass)
        //                    Log.d(TAG, "\t" + complication.complicationData.javaClass)
        //                    Log.d(TAG, "\t" + complication.complicationData.toString())
        //                    Log.d(TAG, "\t" + d.toString())
        //                    Log.d(TAG, "\t" + d.javaClass)
        //                    Log.d(TAG, "\t" + d.dataSource)
        val complication = complicationWrapper.virtualComplication(
            this, zonedDateTime.toInstant()
        )
        if (complication.type == ComplicationType.LONG_TEXT) {
            //                        Log.d(TAG, "\t" + ld.contentDescription!!.getTextAt(context.resources, Instant.now()))
            //                        Log.d(TAG, "\t" + ld.title)
            //                        Log.d(TAG, "\t" + ld.asWireComplicationData())
            val text = complication.text
            val bounds = complicationWrapper.computeBounds(Rect(0, 0, canvas.width, canvas.height))
            val textPaint = getCompPaint(complicationWrapper.id)
            canvas.save()
            canvas.clipRect(bounds)
            val textBounds = Rect()
            var bottom = bounds.top
            if (!text.contains("\n")) {
                var s = text
                while (s.isNotEmpty()) {
                    val l = textPaint.breakText(s, true, bounds.width().toFloat(), null)
                    textPaint.getTextBounds(s, 0, l, textBounds)
                    bottom += textBounds.bottom - textBounds.top
                    canvas.drawText(
                        s.substring(0, l), bounds.left.toFloat(), bottom.toFloat(), textPaint
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
                        t, bounds.left.toFloat(), bottom.toFloat(), textPaint
                    )
                    bottom += 4
                    if (bottom > bounds.bottom) break
                }
            }
            canvas.restore()
            //                        Log.d(TAG, "Long Text: " + text);
            //                        complication.render(canvas, zonedDateTime, renderParameters)

        } else {
            //                        Log.d(TAG, "value: " + d)
            //                        Log.d(TAG, "value2:" + d.dataSource)
            complicationWrapper.defaultRender(canvas, zonedDateTime, renderParameters)
        }
    }

    private fun drawSmallTextAndIcon(
        complicationWrapper: ComplicationWrapper, canvas: Canvas, zonedDateTime: ZonedDateTime
    ) {
        val bounds = complicationWrapper.computeBounds(Rect(0, 0, canvas.width, canvas.height))

        canvas.drawRoundRect(
            bounds.toRectF(), bounds.height() * .2f, bounds.height() * .2f, D12.blackBackground
        )

//        Log.d(TAG, "smallTextAndIcon: " + complication.complicationData.value.dataSource?.className)
        //Log.d(TAG, "bounds: " + complicationWrapper.id + ":"+ bounds)

        val left = bounds.left.toFloat()
        val bottom = bounds.bottom.toFloat()
        val h = bounds.bottom - bounds.top
        val complication = complicationWrapper.virtualComplication(
            this, zonedDateTime.toInstant()
        )
        if (complication.type == ComplicationType.SHORT_TEXT) {
            val textAt = complication.text
            val image = complication.image
            val drawable = image?.loadDrawable(context)
            var textClipBounds = bounds
            if (complication.customDrawable(
                    canvas, left, bounds.top.toFloat(), bottom, h.toFloat()
                )
            ) {
                textClipBounds = if (complicationWrapper.id % 2 == 1) {
                    Rect(bounds.left, bounds.top, bounds.right - h, bounds.bottom)
                } else {
                    Rect(bounds.left + h, bounds.top, bounds.right, bounds.bottom)
                }
            } else if (drawable != null) {
                val imagebounds = if (complicationWrapper.id % 2 == 1) {
                    Rect(bounds.right - h, bounds.top, bounds.right, bounds.bottom)
                } else {
                    Rect(bounds.left, bounds.top, bounds.left + h, bounds.bottom)
                }
                drawable.bounds = imagebounds
                drawable.draw(canvas)
                textClipBounds = if (complicationWrapper.id % 2 == 1) {
                    Rect(bounds.left, bounds.top, bounds.right - h, bounds.bottom)
                } else {
                    Rect(bounds.left + h, bounds.top, bounds.right, bounds.bottom)
                }
            }
            val textBounds = Rect()
            var textPaint = getCompPaint(complicationWrapper.id)
            if (textAt.length > 3) {
                val size = textPaint.textSize * .8f
                textPaint = Paint(textPaint).apply { textSize = size }
            }
            textPaint.getTextBounds(
                textAt, 0, textAt.length, textBounds
            )
            val v = h + h / 4
            val y = bounds.bottom - h / 2 + textBounds.height() / 2
            val x = if (complicationWrapper.id % 2 == 1) {
                bounds.right - v - textBounds.width()
            } else {
                bounds.left + v
            }
            canvas.save()
            canvas.clipRect(textClipBounds)
            canvas.drawText(
                textAt, x.toFloat(), y.toFloat(), textPaint
            )
            canvas.restore()
        }
    }

    private fun getCompPaint(complicationId: Int): Paint {
//        val r = OFD.complicationPaint.get(complicationId)!!.get(drawMode)
//        if (r != null)
//            return r
        val m1 = D12.complicationPaint[complicationId]
        if (m1 != null) {
            val m2 = m1[DrawMode.INTERACTIVE]
            if (m2 != null) return m2
        }
        return Paint()
    }

    private fun drawTime(
        canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime
    ) {
        val cx = bounds.exactCenterX()
        val h = cx * 2
        val g5 = D12.dateTimeGapP * h
        val datetop = D12.dateClockGapP * h
        val datebottom = D12.dateBottomP * h
        val datecy = (datebottom - datetop) / 2 + datetop

        val tt = datebottom + g5
        val th = D12.timeHeightP * h

        val textBounds = Rect()
        val date = zonedDateTime.format(dtf)
        D12.textPaint.getTextBounds(date, 0, date.length, textBounds)
        dateBounds = RectF(
            cx - textBounds.width() / 1.8f,
            datecy - textBounds.height() / 1.6f,
            cx + textBounds.width() / 1.7f,
            datecy + textBounds.height() / 1.6f
        )
        canvas.drawRoundRect(
            dateBounds!!,
            textBounds.height() * .2f,
            textBounds.height() * .2f,
            if (dateHighlight) D12.grayBackground else D12.blackBackground
        )
        dateHighlight=false
        canvas.drawText(
            date, cx - textBounds.width() / 2.0f, datecy + textBounds.height() / 2.0f, D12.textPaint
        )
//        canvas.drawText(
//            date,
//            datecx - textBounds.width() / 2.0f,
//            datecy + textBounds.height() / 2.0f,
//            OFD.textPaintStroke
//        )

        val tcy = th / 2 + tt
        var time = zonedDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM))
        time = time.substring(0, time.length - 3)
        D12.timePaint.getTextBounds("00:00:00", 0, time.length, textBounds)
        val fullWidth = textBounds.width()
        val fullHeight = textBounds.height()
        D12.timePaint.getTextBounds(time, 0, time.length, textBounds)
        canvas.drawRoundRect(
            cx - fullWidth / 1.8f,
            tcy - fullHeight / 1.6f,
            cx + fullWidth / 1.7f,
            tcy + fullHeight / 1.6f,
            fullHeight * .2f,
            fullHeight * .2f,
            D12.blackBackground
        )
        canvas.drawText(
            time, cx - fullWidth / 2.0f, tcy + textBounds.height() / 2.0f, D12.timePaint
        )
    }

    companion object {
        private const val TAG = "OFDDigitalRenderer"

    }
}
