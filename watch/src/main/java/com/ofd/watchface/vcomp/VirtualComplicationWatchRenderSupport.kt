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

/**
 * TODO let's figure out a better method, but for now when used in VComp construction they will
 * not be needed for the functionality, it avoids allowing null values
 */
class NullVCompSupport : VirtualComplicationWatchRenderSupport {
    override val messageClient: MessageClient
        get() = TODO("Not yet implemented")
    override val capabilityClient: CapabilityClient
        get() = TODO("Not yet implemented")
    override val scope: CoroutineScope
        get() = TODO("Not yet implemented")
    override val context: Context
        get() = TODO("Not yet implemented")
    override val currentUserStyleRepository: CurrentUserStyleRepository?
        get() = TODO("Not yet implemented")
}
