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
package com.ofd.digital.alpha.datalayer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.MaterialTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.wearable.*
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.*
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

    private val isCameraSupported by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    private val clientDataViewModel by viewModels<ClientDataViewModel>()

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        clientDataViewModel.onPictureTaken(bitmap = bitmap)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var count = 0

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // Set the initial trigger such that the first count will happen in one second.
                var lastTriggerTime = Instant.now() - (countInterval - Duration.ofSeconds(1))
                while (isActive) {
                    // Figure out how much time we still have to wait until our next desired trigger
                    // point. This could be less than the count interval if sending the count took
                    // some time.
                    delay(
                        Duration.between(Instant.now(), lastTriggerTime + countInterval).toMillis()
                    )
                    // Update when we are triggering sending the count
                    lastTriggerTime = Instant.now()
                    sendCount(count)

                    // Increment the count to send next time
                    count++
                }
            }
        }

        setContent {
            MaterialTheme {
                MainApp(
                    events = clientDataViewModel.events,
                    image = clientDataViewModel.image,
                    isCameraSupported = isCameraSupported,
                    onTakePhotoClick = ::takePhoto,
                    onSendPhotoClick = ::sendPhoto,
                    onStartWearableActivityClick = ::startWearableActivity,
                    onPauseMusicClick = ::pauseMusic
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dataClient.addListener(clientDataViewModel)
        messageClient.addListener(clientDataViewModel)
        capabilityClient.addListener(
            clientDataViewModel,
            Uri.parse("wear://"),
            CapabilityClient.FILTER_REACHABLE
        )

        if (isCameraSupported) {
            lifecycleScope.launch {
                try {
                    capabilityClient.addLocalCapability(CAMERA_CAPABILITY).await()
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (exception: Exception) {
                    Log.e(TAG, "Could not add capability: $exception")
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        dataClient.removeListener(clientDataViewModel)
        messageClient.removeListener(clientDataViewModel)
        capabilityClient.removeListener(clientDataViewModel)

        lifecycleScope.launch {
            // This is a judicious use of NonCancellable.
            // This is asynchronous clean-up, since the capability is no longer available.
            // If we allow this to be cancelled, we may leave the capability in-place for other
            // nodes to see.
            withContext(NonCancellable) {
                try {
                    capabilityClient.removeLocalCapability(CAMERA_CAPABILITY).await()
                } catch (exception: Exception) {
                    Log.e(TAG, "Could not remove capability: $exception")
                }
            }
        }
    }

    private fun startWearableActivity() {
        lifecycleScope.launch {
            try {
                val caps = capabilityClient
                    .getAllCapabilities(CapabilityClient.FILTER_REACHABLE)
//                    .getCapability(WEAR_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                    .await()

                Log.d(TAG, "Found caps: " + caps.keys.toString())
                val nodes = caps.get(WEAR_CAPABILITY)?.nodes ?: emptyList()

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

    private fun pauseMusic() {
        Log.d(TAG, "Pausing")
        val mAudioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if(mAudioManager.isMusicActive)
            Log.d(TAG, "Music active")
        
        if (false ) {
            Log.d(TAG, "Pausing by button")
            val i = Intent("com.android.music.musicservicecommand")
            i.putExtra("command", "pause")
            applicationContext.sendBroadcast(i)
        } else {
            Log.d(TAG, "Pausing by focus")
            val mPlaybackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val mFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(mPlaybackAttributes)
//                        .setAcceptsDelayedFocusGain(true)
//                        .setWillPauseWhenDucked(true)
//                        .setOnAudioFocusChangeListener(this, Handler(){d -> })
                .build()
            val res = mAudioManager.requestAudioFocus(mFocusRequest)
            if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                Log.d(TAG, "Failed")
            } else if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d(TAG, "OK")
            } else if (res == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
                Log.d(TAG, "Delayed")
            }
        }
    }

    private fun takePhoto() {
        if (!isCameraSupported) return
        takePhotoLauncher.launch(null)
    }

    private fun sendPhoto() {
        lifecycleScope.launch {
            try {
                val image = clientDataViewModel.image ?: return@launch
                val imageAsset = image.toAsset()
                val request = PutDataMapRequest.create(IMAGE_PATH).apply {
                    dataMap.putAsset(IMAGE_KEY, imageAsset)
                    dataMap.putLong(TIME_KEY, Instant.now().epochSecond)
                }
                    .asPutDataRequest()
                    .setUrgent()

                val result = dataClient.putDataItem(request).await()

                Log.d(TAG, "DataItem saved: $result")
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                Log.d(TAG, "Saving DataItem failed: $exception")
            }
        }
    }

    private suspend fun sendCount(count: Int) {
        try {
            val request = PutDataMapRequest.create(COUNT_PATH).apply {
                dataMap.putInt(COUNT_KEY, count)
            }
                .asPutDataRequest()
                .setUrgent()

            val result = dataClient.putDataItem(request).await()

            Log.d(TAG, "DataItem saved: $result")
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Log.d(TAG, "Saving DataItem failed: $exception")
        }
    }

    /**
     * Converts the [Bitmap] to an asset, compress it to a png image in a background thread.
     */
    private suspend fun Bitmap.toAsset(): Asset =
        withContext(Dispatchers.Default) {
            ByteArrayOutputStream().use { byteStream ->
                compress(Bitmap.CompressFormat.PNG, 100, byteStream)
                Asset.createFromBytes(byteStream.toByteArray())
            }
        }

    companion object {
        private const val TAG = "MainActivity"

        private const val START_ACTIVITY_PATH = "/start-activity"
        private const val COUNT_PATH = "/count"
        private const val IMAGE_PATH = "/image"
        private const val IMAGE_KEY = "photo"
        private const val TIME_KEY = "time"
        private const val COUNT_KEY = "count"
        private const val CAMERA_CAPABILITY = "camera"
        private const val WEAR_CAPABILITY = "wear"

        private val countInterval = Duration.ofSeconds(5)
    }
}
