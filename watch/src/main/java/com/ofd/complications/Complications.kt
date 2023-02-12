package com.ofd.complications

import android.content.Context
import android.util.Log
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.ofd.watchface.vcomp.ComplicationSlotManagerHolder
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * After a location update, this will force all location aware complications an upate.
 *
 * TODO - is there a better way?
 */
class Complications {

    companion object {

        var TAG = "Complications"

        fun forceComplicationUpdate(
            applicationContext: Context, complicationSlotsManagerHolder:
            ComplicationSlotManagerHolder
        ) {
            complicationSlotsManagerHolder.watch.scope.launch {
//                CalendarListenerService.readInitialValues(applicationContext)
                Wearable.getCapabilityClient(applicationContext)
                    .getCapability("mobile", CapabilityClient.FILTER_REACHABLE)
                    .await().nodes.forEach({
                        Wearable.getMessageClient(applicationContext).sendMessage(
                            it.id,
                            "/calendar/refresh", byteArrayOf()
                        )
                    })
                complicationSlotsManagerHolder.slotManager.complicationSlots.forEach { cs ->
                    val dataSource = cs.value.complicationData.value.dataSource
                    if (dataSource != null) {
                        Log.d(
                            TAG, "Complication: " + cs.key + ": " + dataSource.toString()
                        )
                        if (dataSource.packageName.equals("com.ofd.watch")) {
                            Log.d(TAG, "updating " + dataSource.toString())
                            ComplicationDataSourceUpdateRequester.create(
                                applicationContext, dataSource
                            ).requestUpdateAll()
                        }
                    }
                }
            }
        }
    }
}
