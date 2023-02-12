package com.ofd.complications

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import java.util.concurrent.atomic.AtomicReference

/*
 * This manages the playing and pausing. An instance of this class will be created for generally
 * each request due to the destruction of Service.
 *
 * An AtomicReference is assed in for holding data that shoud survice the destruction. Likely a
 * ViewModel might be better, but for now this is simple.
 *
 * Generally you will use the service, but if you alternate between the PhoneApp and Service there
 * may be issues due to the focusRequest not being the same.
 */
class PlayPause(val context: Context, val focusRequest: AtomicReference<AudioFocusRequest?>) {
    val TAG = this.javaClass.simpleName

    var mAudioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun togglePlayback() {
        if (focusRequest.get() == null) pauseMusic()
        else resumeMusic()
    }

    fun resumeMusic() {
        if (focusRequest.get() == null) {
            Log.d(TAG, "No pause to resume from")
        } else {
            Log.d(TAG, "Resuming")
            mAudioManager!!.abandonAudioFocusRequest(focusRequest.get()!!)
        }
        focusRequest.set(null)
    }

    fun pauseMusic() {
        Log.d(TAG, "Pausing")
        if (mAudioManager!!.isMusicActive) Log.d(TAG, "Music active")

        Log.d(TAG, "Pausing by focus")
        val mPlaybackAttributes =
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        focusRequest.set(AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(mPlaybackAttributes)
//                        .setAcceptsDelayedFocusGain(true)
//                        .setWillPauseWhenDucked(true)
//                        .setOnAudioFocusChangeListener(this, Handler(){d -> })
            .build())
        when (mAudioManager!!.requestAudioFocus(focusRequest.get()!!)) {
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

