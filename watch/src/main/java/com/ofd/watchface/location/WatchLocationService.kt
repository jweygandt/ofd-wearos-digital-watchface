package com.ofd.watchface.location

import android.content.Context
import android.util.Log
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import com.ofd.watchface.vcomp.ComplicationSlotManagerHolder
import com.ofd.complications.Complications
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/*
 * Everything is in static context
 *
 * I have found that location services in complications are iffy (on Galaxy Watch 5), so doing
 * it during the render call seems to be more reliable, and then simply keeping the results
 * in a static location.
 *
 * Since all is in static, simply in render calls, done frequently...
 *
 * WatchLocationService.doOnRender(...)
 *
 * Location will be pulled infrequently and Complications.forceComplicationUpdate(...) will be
 * called.
 */
class WatchLocationService {

    companion object {
        private const val TAG = "WatchLocationService"
        private var locationViewModel: LocationViewModel? = null
        private val lastTime = AtomicLong(0)
        private val callcnt: AtomicInteger = AtomicInteger(0)
        private val successcnt = AtomicInteger(0)
        private val lastLocation = AtomicReference<WatchLocation>()

        // Seems it always fails once on startup, so try again quickly
        private const val startUpMs: Int = 15000

        // Currently sunset/sunrise and air quality don't really need
        // very frequent updates
        private const val refreshPeriodMs = 10 * 60000

        fun reset() {
            lastTime.set(0)
            lastLocation.set(null)
        }

        class WatchLocation(
            val callcnt: Int, val successcnt: Int, val location: ResolvedLocation
        ) {
            val latitude = location.latitude
            val longitude = location.longitude
//            val timeAgo = location.timeAgo
            val valid = location.valid
            suspend fun getAddressDescription() = location.getAddressDescription()
            suspend fun getShortAddress() = location.getShortAddress()
        }

        fun doOnRender(
            scope: CoroutineScope, context: Context, renderParameters: RenderParameters,
            complicationSlotsManagerHolder: ComplicationSlotManagerHolder
        ) {
            val now = System.currentTimeMillis()
            val delay = if (lastLocation.get() == null) startUpMs else refreshPeriodMs
            if (now - lastTime.get() > delay && renderParameters.drawMode == DrawMode.INTERACTIVE) {
                lastTime.set(now)
                scope.launch {
                    Log.d(WhereAmIActivity.TAG, "render:launch()")
                    val cc = callcnt.incrementAndGet()
                    if (locationViewModel == null) {
                        locationViewModel = LocationViewModel("Watch.render", context)
                    }
                    val location = locationViewModel!!.readLocationResult()

                    if (location is ResolvedLocation) {
                        lastLocation.set(
                            WatchLocation(
                                cc, successcnt.incrementAndGet(), location
                            )
                        )
                    } else {
                        Log.e(TAG, "Problems getting location: $location")
                    }
                    Log.d(WhereAmIActivity.TAG, "render:launch():location=$location")
                    Complications.forceComplicationUpdate(context, complicationSlotsManagerHolder)
                }
            }
        }

        fun getLocation(): WatchLocation {
            return lastLocation.get()
                ?: WatchLocation(
                    callcnt.get(), successcnt.get(), ResolvedLocation(
                        null, null
                    )
                )
        }
    }
}
