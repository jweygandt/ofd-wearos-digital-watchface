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

import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.style.UserStyle
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.ofd.watch.R
import com.ofd.watchface.digital12.D12
import com.ofd.watchface.digital12.DigitalWatchCanvasRenderer
import com.ofd.watchface.vcomp.ICON_BACKGROUND
import com.ofd.watchface.vcomp.VirtualComplication
import com.ofd.watchface.vcomp.VirtualComplicationWatchRenderSupport
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.tasks.await

private val tapCount = AtomicInteger(0)
val playPauseEnabled = AtomicBoolean(false)

private const val TAG = "VirtualComplicationPlayPause"

/**
 * This is the BIGGIE! This is to be used in position 7, special SMALL_IMAGE slot to toggle
 * COMPLICATION_14 on and off, and set the global playPauseMode state.
 *
 * Currently the icons and state name are misleading, it is OFF, TAPONLY, TAPANDVISIBLE, however
 * names may change over time after some real world testing.
 */
class VirtualComplicationPlayPauseImpl(
    val watch: VirtualComplicationWatchRenderSupport
) : VirtualComplication {
    override val type: ComplicationType
        get() = ComplicationType.SMALL_IMAGE

    override val image: Icon
        get() {
            return Icon.createWithResource(
                watch.context, getIconFromPlayMode()
            )
        }

    private fun getIconFromPlayMode() =
        if (playPauseEnabled.get()) R.drawable.ic_play_pause_active
        else R.drawable.ic_play_pause_disabled

    override val text: String
        get() = D12.status.get() ?: "Not yet set"
    override val rangeValue: Float
        get() = 0f
    override val rangeMin: Float
        get() = 0f
    override val rangeMax: Float
        get() = 0f

    override fun customDrawable(
        canvas: Canvas, bleft: Float, btop: Float, bbottom: Float, sqsize: Float
    ): Boolean = false

    override val tapCallback: Runnable
        get() = Runnable {
            val cnt = tapCount.incrementAndGet()
            when (cnt % 2) {
                0 -> {
                    playPauseEnabled.set(false)
                    setEnable(false)
                }
                1 -> {
                    playPauseEnabled.set(true)
                    setEnable(true)
                }
            }
        }

    private fun setEnable(enable: Boolean) {
        Log.d(TAG, "Setting enable: $enable")
        val styleSetting = watch.currentUserStyleRepository!!.schema.userStyleSettings.get(0)
        val defaultOption = styleSetting.options.get(if (enable) 1 else 0)
        val newStyle = UserStyle(
            selectedOptions = mapOf(
                Pair(
                    styleSetting, defaultOption
                )
            )
        )
        watch.currentUserStyleRepository!!.updateUserStyle(newStyle)
    }

    override val color: Int
        get() = -1

    override val expiresms: Long
        get() = -1

    override val iconBackground: ICON_BACKGROUND
        get() = ICON_BACKGROUND.NONE

    companion object {
        suspend fun setWatchState(watch: DigitalWatchCanvasRenderer, visible: Boolean?) {
            if (visible != null && playPauseEnabled.get()) {
                Log.d(TAG, "WatchState.visible=$visible")
                val nodeMap = getCapabilitiesForReachableNodes(watch.capabilityClient)
                nodeMap.filter { e -> e.value.contains("mobile") }.map { e -> e.key }.forEach { n ->
                    notifyVisibilityChanged(watch.messageClient, n, visible)
                }
            }
        }

        suspend fun toggleMusic(watch: VirtualComplicationWatchRenderSupport) {
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
                    capabilityInfo.nodes.forEach { n ->
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
