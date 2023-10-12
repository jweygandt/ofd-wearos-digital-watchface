package com.ofd.watchface2

import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import java.util.concurrent.atomic.AtomicInteger

class WallpaperWatch : WallpaperService() {

    val ctr = AtomicInteger(0)

    override fun onCreateEngine(): Engine {
        return object : Engine() {

            override fun onCreate(surfaceHolder: SurfaceHolder?) {
                Log.d(TAG, "onCreate: " + ctr.incrementAndGet())
            }

            override fun onSurfaceRedrawNeeded(holder: SurfaceHolder?) {
                Log.d(TAG, "onSurfaceRedrawNeeded: " + ctr.incrementAndGet())
            }
        }
    }

    companion object {
        const val TAG = "WallpaperWatch"
    }
}
