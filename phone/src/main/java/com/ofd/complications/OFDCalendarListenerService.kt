/*
 * Copyright 2021 The Android Open Source Project
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
package com.ofd.complications

import android.util.Log
import com.google.android.gms.wearable.*

class OFDCalendarListenerService : WearableListenerService() {

    private val TAG = this.javaClass.simpleName

    override fun onCapabilityChanged(ci: CapabilityInfo) {
        Log.d(TAG, "Capabilities changed: " + ci.nodes)
        Wearable.getCapabilityClient(applicationContext)
            .getCapability("wear", CapabilityClient.FILTER_REACHABLE).addOnCompleteListener {
                if (it.result.nodes.size > 0) OFDCalendarSyncJob.register(applicationContext)
            }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "!!!!!! Message Received: $messageEvent")

        when (messageEvent.path) {

            "/calendar/refresh" -> OFDCalendar.setEventData(
                contentResolver, Wearable.getDataClient(applicationContext)
            )

            else -> Log.e(TAG, "Unknown event: " + messageEvent.path)
        }
    }
}
