package com.ofd.watchface2

import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.wear.watchface.*
import androidx.wear.watchface.Renderer.CanvasRenderer2
import androidx.wear.watchface.style.CurrentUserStyleRepository
import java.time.ZonedDateTime

class Watch2 : WatchFaceService() {
    class Watch2SharedAssets : Renderer.SharedAssets {
        override fun onDestroy() {
        }
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        return WatchFace(
            WatchFaceType.ANALOG,
            object : CanvasRenderer2<Watch2SharedAssets>(
                surfaceHolder,
                currentUserStyleRepository,
                watchState,
                CanvasType.HARDWARE,
                1000L,
                true
            ) {

                val ticks = TicksLayout1(applicationContext)
                val hands = Hands(GetHandData(applicationContext))

                override suspend fun createSharedAssets(): Watch2SharedAssets {
                    return Watch2SharedAssets()
                }

                override fun renderHighlightLayer(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime,
                    sharedAssets: Watch2SharedAssets
                ) {
                }

                override fun render(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime,
                    sharedAssets: Watch2SharedAssets
                ) {
                    val center = VPoint(canvas.width / 2f, canvas.height / 2f)

//                    layouts.background.draw(canvas, renderParameters.drawMode, center)

//                    if (renderParameters.drawMode != DrawMode.AMBIENT || state.complicationsState.hasInAmbientMode) {
//                        drawComplications(canvas, zonedDateTime)
//                    }

                    ticks.draw(canvas, renderParameters.drawMode, center)

                    hands.draw(canvas, zonedDateTime, renderParameters.drawMode, center)
                }
            })
    }
}
