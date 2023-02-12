package com.ofd.watchface.vcomp

import android.content.ContentResolver
import android.content.Context
import androidx.wear.watchface.style.CurrentUserStyleRepository
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import kotlinx.coroutines.CoroutineScope

/**
 * Many items are needed during handing of VComps, such as context. So to that end the Renderer
 * .CanvasRenderer2 class should also implement this class
 */
interface VirtualComplicationWatchRenderSupport {
    val messageClient: MessageClient
    val capabilityClient: CapabilityClient
    val scope: CoroutineScope
    val context: Context
    val currentUserStyleRepository : CurrentUserStyleRepository?
    val contentResolver : ContentResolver
}

