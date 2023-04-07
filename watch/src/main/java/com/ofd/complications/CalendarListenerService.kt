/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ofd.complications

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.tasks.await

class CalendarListenerService : WearableListenerService() {

    companion object {
        private val TAG = "CalendarListenerService"

        public val events = AtomicReference(listOf<String>())

        suspend fun readInitialValues(context: Context) {
            Wearable.getDataClient(context).dataItems.await().forEach { di ->
                when (di.uri.path) {
                    "/calendar/events" -> setEvents(di)
                }
            }
        }

        private fun setEvents(di: DataItem) {
            try {
                val sdftoday = SimpleDateFormat("h:mmaa", Locale.US)
                val sdflater = SimpleDateFormat("EEE haa", Locale.US)
                val sdfday = SimpleDateFormat("EEE", Locale.US)
                val cal = Calendar.getInstance()
                val now = System.currentTimeMillis()
                val today = cal.get(Calendar.DAY_OF_YEAR)
                val ois = ObjectInputStream(ByteArrayInputStream(di.data))
                val r = mutableListOf<String>()
                val date = ois.readLong()
                while (true) {
                    val dtstart = ois.readLong()
                    if (dtstart < 0) break;
                    val dtend = ois.readLong()
                    val title = ois.readObject() as String
                    val allday = ois.readInt() == 1
                    cal.timeInMillis = dtstart
                    val today = cal.get(Calendar.DAY_OF_YEAR) == today
                    val timestr = if (allday) {
                        if (dtstart <= now){
                            if(dtend <= now) null else "Today"
                        } else sdfday.format(dtstart)
                    } else {
                        if (dtstart <= now) {
                            if (dtend <= now) null else "Now"
                        } else {
                            if (today) sdftoday.format(dtstart) else sdflater.format(dtstart)
                        }
                    }
                    if (timestr != null) r.add(
                        timestr + " " + title
                    )
                }
                Log.d(TAG, "Events1: " + r.joinToString("\n", "    "))
                events.set(r)
            } catch (e: Exception) {
                events.set(listOf(e.message ?: "ERROR"))
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        Log.d(TAG, "!!!!!! Data Changed")

        dataEvents.forEach { dataEvent ->
            val uri = dataEvent.dataItem.uri
//            Log.d(TAG, "Data: " + uri.toString())
            when (uri.path) {
                "/calendar/events" -> {
                    setEvents(dataEvent.dataItem)
                    Log.d(
                        TAG, "updating: " + ComponentName(
                            applicationContext, CalendarComplication::class.java
                        )
                    )
                    ComplicationDataSourceUpdateRequester.create(
                        applicationContext,
                        ComponentName(applicationContext, CalendarComplication::class.java)
                    ).requestUpdateAll()
                }
            }
        }
    }
}
