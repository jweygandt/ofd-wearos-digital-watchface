// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.ofd.digital.alpha

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.fondesa.kpermissions.allGranted
import com.fondesa.kpermissions.coroutines.sendSuspend
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.ofd.digital.alpha.location.LocationViewModel
import com.ofd.digital.alpha.location.ResolvedLocation
import java.util.*
import kotlinx.coroutines.launch

class WhereAmIActivity : FragmentActivity() {
    private lateinit var locationViewModel: LocationViewModel

    private lateinit var textView: TextView

    class TT(val w: WhereAmIActivity) : TimerTask() {
        override fun run() {
//            Log.d(TAG, "Updating complication")
//            w.applicationContext.forceComplicationUpdate()
            Log.d(TAG, "Timer location")
            w.lifecycleScope.launch {
                Log.d(TAG, "in launch")
                Log.d(TAG, "TLocation=" + w.locationViewModel.readLocationResult())
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.where_am_i_activity)
        textView = findViewById(R.id.text)

        locationViewModel = LocationViewModel("Activity:onCreate", applicationContext)

//        Timer().scheduleAtFixedRate(TT(this), 0, 60000)

        lifecycleScope.launch {
            Log.d(TAG, "onCreate:launch()")
            checkPermissions()

            val location = locationViewModel.readLocationResult()

            if (location is ResolvedLocation) {
                textView.text = "You are at: " + location.getAddressDescription() + ":" +
                    location.getTimeAgo()
            } else {
                textView.setText(R.string.location_error)
            }
        }

//        forceComplicationUpdate()
    }

    override fun onRestart() {
        Log.d(TAG, "onRestart()")
        super.onRestart()
    }

    override fun onStop() {
        Log.d(TAG, "onStop()")
//        forceComplicationUpdate()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()
    }

    suspend fun checkPermissions() {
        val result = permissionsBuilder(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ).build().sendSuspend()

        if (!result.allGranted()) throw SecurityException("No location permission")
    }

    companion object {
        var TAG = "WhereAmIActivity"
        fun Context.tapAction(): PendingIntent? {
            val intent = Intent(this, WhereAmIActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
