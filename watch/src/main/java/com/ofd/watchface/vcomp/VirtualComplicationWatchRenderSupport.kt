package com.ofd.watchface.vcomp

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
    abstract val messageClient: MessageClient
    abstract val capabilityClient: CapabilityClient
    abstract val scope: CoroutineScope
    abstract val context: Context
    abstract val currentUserStyleRepository : CurrentUserStyleRepository?
}

