package com.ofd.apis.openweather

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.util.Log
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val BITMAP_CACHE = ConcurrentHashMap<String, Bitmap>()
private val ICON_CACHE = ConcurrentHashMap<String, Icon>()
private const val TAG = "OpenWeatherIcons"

suspend fun getBitmap(iname: String): Bitmap? {
    var bm = BITMAP_CACHE[iname]
    if (bm == null) {
        loadImage(iname)
        bm = BITMAP_CACHE[iname]
    }
    return bm
}

suspend fun getIcon(iname: String): Icon? {
    var icon = ICON_CACHE[iname]
    if (icon == null) {
        loadImage(iname)
        icon = ICON_CACHE[iname]
    }
    return icon
}

suspend private fun loadImage(iname: String) {
    withContext(Dispatchers.IO) {
        Log.d(TAG, "Reading image: " + iname)
        try {
            val url = URL("https://openweathermap.org/img/wn/$iname@2x.png")
            val bm = BitmapFactory.decodeStream(
                url.openConnection().getInputStream()
            )
            val icon = Icon.createWithBitmap(bm)
            BITMAP_CACHE[iname] = bm
            ICON_CACHE[iname] = icon
        } catch (e: Exception) {
            Log.e(TAG, "URL: $iname", e)
        }
    }
}
