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
package com.ofd.digital.watchface.datalayer

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.android.gms.wearable.*
import com.ofd.watch.R

/**
 * A state holder for the client data.
 */
class ClientDataViewModel : ViewModel(), DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener, CapabilityClient.OnCapabilityChangedListener {

    private val _events = mutableStateListOf<Event>()

    private val TAG = "ClientDataViewModel"

    /**
     * The list of events from the clients.
     */
    val events: List<Event> = _events

    /**
     * The currently captured image (if any), available to send to the wearable devices.
     */
    var image by mutableStateOf<Bitmap?>(null)
        private set

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        _events.addAll(dataEvents.map { dataEvent ->
            val title = when (dataEvent.type) {
                DataEvent.TYPE_CHANGED -> R.string.data_item_changed
                DataEvent.TYPE_DELETED -> R.string.data_item_deleted
                else -> R.string.data_item_unknown
            }

            Event(
                title = title, text = dataEvent.dataItem.toString()
            )
        })
        if (_events.size > 8) {
            _events.removeRange(0, _events.size - 8)
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(
            TAG, "Msg: ${messageEvent.sourceNodeId}:${messageEvent.path}:${
                messageEvent.data
            }:${messageEvent.requestId}"
        )
        _events.add(
            Event(
                title = R.string.message_from_watch, text = messageEvent.toString()
            )
        )
        val path = messageEvent.path
        if(path.contains("stop"))
            MainActivity.pauseMusic()
        else if(path.contains("start"))
            MainActivity.resumeMusic()
        else if (path.contains("toggle"))
            MainActivity.togglePlayback()

        if (_events.size > 8) {
            _events.removeRange(0, _events.size - 8)
        }

    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        _events.add(
            Event(
                title = R.string.capability_changed, text = capabilityInfo.toString()
            )
        )
        if (_events.size > 8) {
            _events.removeRange(0, _events.size - 8)
        }

    }

    fun onPictureTaken(bitmap: Bitmap?) {
        image = bitmap ?: return
    }
}

/**
 * A data holder describing a client event.
 */
data class Event(
    @StringRes val title: Int, val text: String
)
