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

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.wearable.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Manages Wearable clients to showcase the [DataClient], [MessageClient], [CapabilityClient] and
 * [NodeClient].
 *
 * While resumed, this activity periodically sends a count through the [DataClient], and offers
 * the ability for the user to take and send a photo over the [DataClient].
 *
 * This activity also allows the user to launch the companion wear activity via the [MessageClient].
 *
 * While resumed, this activity also logs all interactions across the clients, which includes events
 * sent from this activity and from the watch(es).
 */
class MainActivity : ComponentActivity() {

    private val dataClient by lazy { Wearable.getDataClient(this) }
    private val messageClient by lazy { Wearable.getMessageClient(this) }
    private val capabilityClient by lazy { Wearable.getCapabilityClient(this) }

    private var clientDataViewModel: ClientDataViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAudioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        clientDataViewModel = ClientDataViewModel()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
//                // Set the initial trigger such that the first count will happen in one second.
//                var lastTriggerTime = Instant.now() - (countInterval - Duration.ofSeconds(1))
//                while (isActive) {
//                    // Figure out how much time we still have to wait until our next desired trigger
//                    // point. This could be less than the count interval if sending the count took
//                    // some time.
//                    delay(
//                        Duration.between(Instant.now(), lastTriggerTime + countInterval).toMillis()
//                    )
//                    // Update when we are triggering sending the count
//                    lastTriggerTime = Instant.now()
//                    sendCount(count)
//
//                    // Increment the count to send next time
//                    count++
//                }
            }
        }

        setContent {
            MaterialTheme {
                MainApp(
                    events = clientDataViewModel!!.events,
                    image = clientDataViewModel!!.image,
                    onStartWearableActivityClick = ::startWearableActivity,
                    onPauseMusicClick = ::pauseMusic,
                    onResumeMusicClick = ::resumeMusic
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dataClient.addListener(clientDataViewModel!!)
        messageClient.addListener(clientDataViewModel!!)
        capabilityClient.addListener(
            clientDataViewModel!!, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE
        )
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


    companion object {


        private const val TAG = "MainActivity"

        private const val START_ACTIVITY_PATH = "/start-activity"
        private const val WEAR_CAPABILITY = "wear"

        var mAudioManager: AudioManager? = null
        private var mFocusRequest: AudioFocusRequest? = null

        fun togglePlayback() {
            if(mFocusRequest==null)
                pauseMusic()
            else
                resumeMusic()
        }

        fun resumeMusic() {
            if (mFocusRequest == null) {
                Log.d(TAG, "No pause to resume from")
            } else {
                Log.d(TAG, "Resuming")
                mAudioManager!!.abandonAudioFocusRequest(mFocusRequest!!)
            }
            mFocusRequest = null
        }

        fun pauseMusic() {
            Log.d(TAG, "Pausing")
            if (mAudioManager!!.isMusicActive) Log.d(TAG, "Music active")

            Log.d(TAG, "Pausing by focus")
            val mPlaybackAttributes =
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            mFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(mPlaybackAttributes)
//                        .setAcceptsDelayedFocusGain(true)
//                        .setWillPauseWhenDucked(true)
//                        .setOnAudioFocusChangeListener(this, Handler(){d -> })
                .build()
            when (mAudioManager!!.requestAudioFocus(mFocusRequest!!)) {
                AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                    Log.d(TAG, "Failed")
                }
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    Log.d(TAG, "OK")
                }
                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                    Log.d(TAG, "Delayed")
                }
            }
        }
    }
}
