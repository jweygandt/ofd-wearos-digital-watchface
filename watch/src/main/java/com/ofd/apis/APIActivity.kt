package com.ofd.apis

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.ofd.watchface.location.LocationViewModel
import com.ofd.watchface.location.ResolvedLocation
import kotlinx.coroutines.launch

abstract class APIActivity<Result>(private val service: APIService<Result>) :
    ComponentActivity() {

    @Composable
    abstract fun doContent(data: MutableState<Result?>): Unit

    val data = mutableStateOf<Result?>(null)

    val TAG = this.javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { doContent(data) }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val rlocation = LocationViewModel(
                TAG, applicationContext
            ).readLocationResult()
            val r = if (rlocation is ResolvedLocation) {
                service.get(applicationContext, rlocation)
            } else {
                service.makeErrorResult(rlocation.toString())
            }
            Log.d(TAG, "New value: " + r.toString())
            data.value = r
        }
    }

    override fun onPause() {
        super.onPause()
        // well this is a big hack
        // could not get PurpleAQIActivity to reliability launch
        // It would work, but then once OpenWeather*Activity(s) were launched, it would not
        // Clicking on AQI would bring up the last OpenWeather activity
        // Did notice in "less-filtered" logcat that the OS was getting the right intent
        // although with PurpleAQI there was a difference in log message than with OpenWeather
        // something about "locks", but don't really understand the differences

        // So force finish of the activity causes a new one to be created each time
        finish()
    }
}

