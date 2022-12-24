package com.ofd.digital.alpha.location

import android.content.Context
import android.util.Log
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import com.ofd.complications.Complications
import com.ofd.complications.LocationTest
import com.ofd.digital.alpha.WhereAmIActivity
import com.ofd.digital.alpha.utils.COMPLICATION_1
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
 */
class WatchLocationService {

    companion object {
        private val TAG = "WatchLocationService"
        private var locationViewModel: LocationViewModel? = null
        private val lastTime = AtomicLong(0)
        private val callcnt = AtomicInteger(0)
        private val successcnt = AtomicInteger(0)
        private val lastLocation = AtomicReference<WatchLocation>()

        // Seems it always fails once on startup, so try again quickly
        private val startUpMs = 15000

        // Currently sunset/sunrise and air quality don't really need
        // very frequent updates
        private val refreshPeriodMs = 10 * 60000

        class WatchLocation(
            val callcnt: Int, val successcnt: Int, val location: ResolvedLocation
        ) {
            val latitude = location.latitude
            val longitude = location.longitude
            val timeAgo = location.timeAgo
            val valid = location.valid
            suspend fun getAddressDescription() = location.getAddressDescription()
        }

        fun doOnRender(
            scope: CoroutineScope, context: Context, renderParameters: RenderParameters
        ) {
            var now = System.currentTimeMillis()
            var delay = if (lastLocation.get() == null) startUpMs else refreshPeriodMs
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
                        Log.e(TAG, "Problems getting location: " + location)
                    }
                    Log.d(WhereAmIActivity.TAG, "render:launch():location=" + location)
                    Complications.forceComplicationUpdate(context)
                }
            }
        }

        fun getLocation(): WatchLocation {
            val loc = lastLocation.get()
            return if (loc != null) loc else WatchLocation(
                callcnt.get(), successcnt.get(), ResolvedLocation(
                    null, null
                )
            )
        }
    }
}
