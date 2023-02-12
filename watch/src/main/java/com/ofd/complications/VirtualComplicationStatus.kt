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
import android.provider.CalendarContract
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.ofd.watchface.digital12.D12
import com.ofd.watchface.vcomp.ICON_BACKGROUND
import com.ofd.watchface.vcomp.VirtualComplication
import com.ofd.watchface.vcomp.VirtualComplicationWatchRenderSupport
import java.util.concurrent.atomic.AtomicInteger

/**
 * This is a better, more dynamic version of ComplicationStats, to be used for debugging in
 * position 5.
 */
class VirtualComplicationStatusImpl(val watch: VirtualComplicationWatchRenderSupport) : VirtualComplication {
    private val TAG = "VirtualComplicationStatusImpl"

    override val type: ComplicationType
        get() = ComplicationType.LONG_TEXT
    override val image: Icon? get() = null
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

    override val onTap: Runnable?
        get() = Runnable {
            Log.d(TAG, "onClick")
            getCalendars()
        }

    override val color: Int
        get() = -1

    override val expiresms: Long
        get() = -1

    override val iconBackground: ICON_BACKGROUND
        get() = ICON_BACKGROUND.NONE

    private val EVENT_PROJECTION = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.NAME,
        CalendarContract.Calendars.CALENDAR_COLOR,
        CalendarContract.Calendars.VISIBLE,
        CalendarContract.Calendars.SYNC_EVENTS,
        CalendarContract.Calendars.ACCOUNT_NAME,
        CalendarContract.Calendars.ACCOUNT_TYPE,
    )
    private val PROJECTION_ID_INDEX = 0
    private val PROJECTION_DISPLAY_NAME_INDEX = 1
    private val PROJECTION_NAME_INDEX = 2
    private val PROJECTION_CALENDAR_COLOR_INDEX = 3
    private val PROJECTION_VISIBLE_INDEX = 4
    private val PROJECTION_SYNC_EVENTS_INDEX = 5
    private val PROJECTION_ACCOUNT_NAME_INDEX = 6
    private val PROJECTION_ACCOUNT_TYPE_INDEX = 7

    fun getCalendars() {
        // Run query
//        calendarItemAdapter.clearData()
        val uri = CalendarContract.Calendars.CONTENT_URI
        val selection = ""
        val selectionArgs = emptyArray<String>()
        val cur = watch.contentResolver.query(
            uri,
            EVENT_PROJECTION,
            selection, selectionArgs,
            null,
        )
        Log.d(TAG, "CUR: $cur")
        Log.d(TAG, "CURs: ${cur?.count}")
        while (cur?.moveToNext() == true) {
            val calId = cur.getLong(PROJECTION_ID_INDEX)
            val displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX)
            val name = cur.getString(PROJECTION_NAME_INDEX)
            val color = cur.getInt(PROJECTION_CALENDAR_COLOR_INDEX)
            val visible = cur.getInt(PROJECTION_VISIBLE_INDEX)
            val syncEvents = cur.getInt(PROJECTION_SYNC_EVENTS_INDEX)
            val accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX)
            val accountType = cur.getString(PROJECTION_ACCOUNT_TYPE_INDEX)
            Log.d(TAG, "data: $calId\t$displayName\t$name\t$color\t$visible\t$syncEvents\t$accountName\t$accountType")
//            calendarItemAdapter.pushData(
//                CalendarItem(
//                    id = calId,
//                    name = name,
//                    displayName = displayName,
//                    color = color,
//                    visible = visible == 1,
//                    syncEvents = syncEvents == 1,
//                    accountName = accountName,
//                    accountType = accountType,
//                )
//            )
        }
        cur?.close()
    }

}

class VirtualComplicationStatus : ComplicationDataSourceService() {


    companion object {
        private const val TAG = "ComplicationStatus"

        var uctr = AtomicInteger(0)


    }

    override fun onComplicationActivated(
        complicationInstanceId: Int, type: ComplicationType
    ) {
        Log.d(TAG, "onComplicationActivated(): $complicationInstanceId")


    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "Here is line one\nline 2\nline 3 and one that is long\nline 4")
                .build(),
            contentDescription = PlainComplicationText.Builder(text = "Calendar.").build()
        ).setTapAction(null).build()
    }

    override fun onComplicationRequest(
        request: ComplicationRequest, listener: ComplicationRequestListener
    ) {
        val msg = D12.status.get() ?: "Not yet set"
        val data = when (request.complicationType) {

            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(
                    text = "Short"
                ).build(),
                contentDescription = PlainComplicationText.Builder(text = "Calender").build()
            ).build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder(
                    text = msg
                ).build(),
                contentDescription = PlainComplicationText.Builder(text = "Calender").build()
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
