/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ofd.complications

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.ofd.digital.alpha.DigitalWatchCanvasRenderer
import com.ofd.digital.alpha.OFD
import com.ofd.digital.alpha.R
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.tasks.await

public enum class PlayPauseMode { STOP, PLAY, PAUSE }

private val tapCount = AtomicInteger(0)
val playPauseMode = AtomicReference(PlayPauseMode.STOP)

private const val TAG = "VirtualComplicationPlayPause"


class VirtualComplicationPlayPauseImpl(
    val watch: DigitalWatchCanvasRenderer?,
    val context: Context?, val currentUserStyleRepository: CurrentUserStyleRepository?
) : VirtualComplication {
    override val type: ComplicationType
        get() = ComplicationType.SMALL_IMAGE

    override val image: Icon?
        get() {
            return Icon.createWithResource(
                context, getIconFromPlayMode()
            )
        }

    private fun getIconFromPlayMode() = when (playPauseMode.get()) {
        PlayPauseMode.STOP -> R.drawable.ic_baseline_stop_circle_24
        PlayPauseMode.PLAY -> R.drawable.ic_baseline_play_circle_filled_24
        PlayPauseMode.PAUSE -> R.drawable.ic_baseline_pause_circle_filled_24
    }

    override val text: String
        get() = OFD.status.get() ?: "Not yet set"
    override val rangeValue: Float
        get() = 0f
    override val rangeMin: Float
        get() = 0f
    override val rangeMax: Float
        get() = 0f

    override fun customDrawable(
        canvas: Canvas, bleft: Float, btop: Float, bbottom: Float, sqsize: Float
    ): Boolean = false

    override val tapCallback: Runnable?
        get() = Runnable() {
            val cnt = tapCount.incrementAndGet()
            when (cnt % 3) {
                0 -> {
                    playPauseMode.set(PlayPauseMode.STOP)
                    setEnable(false)
                }
                1 -> {
                    playPauseMode.set(PlayPauseMode.PLAY)
                    setEnable(true)
                }
                2 -> {
                    playPauseMode.set(PlayPauseMode.PAUSE)
                    setEnable(true)
                }
            }
        }

    private fun setEnable(enable: Boolean) {
        Log.d(TAG, "Setting enable: $enable")
        var styleSetting = currentUserStyleRepository!!.schema.userStyleSettings.get(0)
        var defaultOption = styleSetting.options.get(if (enable) 1 else 0)
        val newStyle: UserStyle = UserStyle(
            selectedOptions = mapOf(
                Pair(
                    styleSetting, defaultOption
                )
            )
        )
        currentUserStyleRepository.updateUserStyle(newStyle)
    }

    companion object {
        suspend fun setWatchState(watch: DigitalWatchCanvasRenderer, visible: Boolean?) {
            if (visible != null && playPauseMode.get()==PlayPauseMode.PAUSE) {
                Log.d(TAG, "WatchState.visible=$visible")
                val nodeMap = getCapabilitiesForReachableNodes(watch.capabilityClient)
                nodeMap.filter { e -> e.value.contains("mobile") }.map { e -> e.key }.forEach { n ->
                    notifyVisibilityChanged(watch.messageClient, n, visible)
                }
            }
        }

        suspend fun toggleMusic(watch: DigitalWatchCanvasRenderer) {
            Log.d(TAG, "ToggleMusic")
            val nodeMap = getCapabilitiesForReachableNodes(watch.capabilityClient)
            nodeMap.filter { e -> e.value.contains("mobile") }.map { e -> e.key }.forEach { n ->
                notifyVisibilityChanged(watch.messageClient, n, null)
            }
        }

        private suspend fun notifyVisibilityChanged(
            messageClient: MessageClient, n: Node, visible: Boolean?
        ) {
            try {
                val nodeId = n.id
                messageClient.sendMessage(
                    nodeId,
                    if (visible == null) "/toggle-playback" else if (visible) "/stop-playback" else "/start-playback",
                    byteArrayOf()
                ).await()
                Log.d(TAG, "Message sent successfully")
            } catch (cancellationException: CancellationException) {
                Log.e(TAG, "Message cancelled", cancellationException)
            } catch (exception: Exception) {
                Log.e(TAG, "Message failed", exception)
            }

        }

        private suspend fun getCapabilitiesForReachableNodes(capabilityClient: CapabilityClient): Map<Node, Set<String>> =
            capabilityClient.getAllCapabilities(CapabilityClient.FILTER_REACHABLE).await()
                // Pair the list of all reachable nodes with their capabilities
                .flatMap { (capability, capabilityInfo) ->
                    Log.d(TAG, "Capability: " + capability + ":")
                    capabilityInfo.nodes.forEach() { n ->
                        Log.d(
                            TAG, "  " + n.displayName + ":" + capability
                        )
                    }
                    capabilityInfo.nodes.map { it to capability }
                }
                // Group the pairs by the nodes
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                // Transform the capability list for each node into a set
                .mapValues { it.value.toSet() }
    }
}

class VirtualComplicationPlayPause : ComplicationDataSourceService() {


    companion object {
        private const val TAG = "VirtualComplicationPlayPause"

        var uctr = AtomicInteger(0)


    }

    override fun onComplicationActivated(
        complicationInstanceId: Int, type: ComplicationType
    ) {
        Log.d(TAG, "onComplicationActivated(): $complicationInstanceId")


    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        val image = SmallImage.Builder(
            Icon.createWithResource(
                applicationContext, R.drawable.ic_baseline_play_circle_filled_24
            ), SmallImageType.ICON
        ).build()
        return SmallImageComplicationData.Builder(
            smallImage = image, PlainComplicationText.Builder("").build()
        ).build()
    }

    override fun onComplicationRequest(
        request: ComplicationRequest, listener: ComplicationRequestListener
    ) {
        val data = when (request.complicationType) {

            ComplicationType.SMALL_IMAGE -> SmallImageComplicationData.Builder(
                SmallImage.Builder(
                    Icon.createWithResource(
                        applicationContext, R.drawable.ic_baseline_play_circle_filled_24
                    ), SmallImageType.ICON
                ).build(), PlainComplicationText.Builder("").build()
            ).build()

            else -> {
                Log.w(TAG, "Unexpected complication type ${request.complicationType}")
                null
            }
        }
        listener.onComplicationData(data)
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        Log.d(TAG, "Deactivated")
    }


}
