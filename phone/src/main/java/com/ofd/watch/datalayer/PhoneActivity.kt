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
package com.ofd.watch.datalayer

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFocusRequest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.ofd.complications.OFDCalendar
import com.ofd.complications.OFDCalendarSyncJob
import com.ofd.complications.PlayPause
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PhoneActivity : ComponentActivity() {

    private val dataClient by lazy { Wearable.getDataClient(this) }
    private val messageClient by lazy { Wearable.getMessageClient(this) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(this) }

    private var clientDataViewModel: ClientDataViewModel? = null

    private val playPause by lazy {
        PlayPause(
            applicationContext, AtomicReference<AudioFocusRequest?>(null)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clientDataViewModel = ClientDataViewModel(playPause)


        setContent {
            MaterialTheme {
                MainApp(
                    events = clientDataViewModel!!.events,
                    image = clientDataViewModel!!.image,
                    onStartWearableActivityClick = ::startWearableActivity,
                    onPauseMusicClick = playPause::pauseMusic,
                    onResumeMusicClick = playPause::resumeMusic,
                    doItClick = { doIt() },
                )
            }
        }

        if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Request permission")
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(
                Manifest.permission.READ_CALENDAR
            )
        }

        lifecycleScope.launch { OFDCalendar.setEventData(contentResolver, dataClient) }
        lifecycleScope.launch { OFDCalendarSyncJob.register(applicationContext) }
    }

    override fun onResume() {
        super.onResume()
        dataClient.addListener(clientDataViewModel!!)
        messageClient.addListener(clientDataViewModel!!)
        capabilityClient.addListener(
            clientDataViewModel!!, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE
        )

        dataClient.putDataItem(PutDataRequest.create("/data").apply {
            data = "Count: ${callcnt.incrementAndGet()}".toByteArray(Charset.defaultCharset())
        })
        Log.d(TAG, "Data item: ${callcnt.get()}")
    }

    private fun startWearableActivity() {
        lifecycleScope.launch {
            try {
                val caps = capabilityClient.getAllCapabilities(CapabilityClient.FILTER_REACHABLE)
//                    .getCapability(WEAR_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                    .await()

                Log.d(TAG, "Found caps: " + caps.keys.toString())
                val nodes = caps[WEAR_CAPABILITY]?.nodes ?: emptyList()

                Log.d(TAG, "Found nodes: " + nodes.size)

                // Send a message to all nodes in parallel
                nodes.map { node ->
                    Log.d(TAG, "Found node: " + node.id + ":" + node.toString())
                    async {
                        messageClient.sendMessage(node.id, START_ACTIVITY_PATH, byteArrayOf())
                            .await()
                    }
                }.awaitAll()

                Log.d(TAG, "Starting activity requests sent successfully")
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                Log.d(TAG, "Starting activity failed: $exception")
            }
        }
    }

    fun doIt() {
        Log.d(TAG, "doIt()")
        OFDCalendar.getCalendars(contentResolver)
        OFDCalendar.getEvents(contentResolver)
//        OFDCalendar.setEventData(contentResolver, dataClient)
    }

    companion object {

        private const val TAG = "MainActivity"

        private val callcnt = AtomicInteger(0)

        private const val START_ACTIVITY_PATH = "/start-activity"
        private const val WEAR_CAPABILITY = "wear"
    }
}
