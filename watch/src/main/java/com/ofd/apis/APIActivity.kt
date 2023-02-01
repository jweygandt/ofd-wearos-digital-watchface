package com.ofd.apis

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.ofd.watchface.location.LocationViewModel
import com.ofd.watchface.location.ResolvedLocation
import kotlinx.coroutines.launch

abstract class APIActivity<Result> : ComponentActivity() {

    abstract fun makeErrorResult(s: String): Result

    abstract suspend fun getData(context: Context, location: ResolvedLocation): Result

    @Composable
    abstract fun doContent(): Unit

    val data = mutableStateOf<Result?>(null)

    val TAG = this.javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { doContent() }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val rlocation = LocationViewModel(
                TAG, applicationContext
            ).readLocationResult()
            val r = if (rlocation is ResolvedLocation) {
                getData(applicationContext, rlocation)
            } else {
                makeErrorResult(rlocation.toString())
            }
            Log.d(TAG, "New value: " + r.toString())
            data.value = r
        }
    }

    override fun onPause() {
        super.onPause()
    }
}

