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
package com.ofd.calendar

import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import java.util.concurrent.atomic.AtomicInteger


class CalendarService : SuspendingComplicationDataSourceService() {


    companion object {
        private const val TAG = "CalendarService"

        var uctr = AtomicInteger(0)
    }


    override fun onComplicationActivated(
        complicationInstanceId: Int,
        type: ComplicationType
    ) {
        Log.d(TAG, "onComplicationActivated(): $complicationInstanceId")


    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "Here is line one\nline 2\nline 3 and one that is long\nline 4")
                .build(),
            contentDescription = PlainComplicationText.Builder(text = "Calendar.")
                .build()
        )
            .setTapAction(null)
            .build()
    }

    // Projection array. Creating indices for this array instead of doing
// dynamic lookups improves performance.
    private val EVENT_PROJECTION: Array<String> = arrayOf(
        CalendarContract.Calendars._ID,                     // 0
        CalendarContract.Calendars.ACCOUNT_NAME,            // 1
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,   // 2
        CalendarContract.Calendars.OWNER_ACCOUNT            // 3
    )

    // The indices for the projection array above.
    private val PROJECTION_ID_INDEX: Int = 0
    private val PROJECTION_ACCOUNT_NAME_INDEX: Int = 1
    private val PROJECTION_DISPLAY_NAME_INDEX: Int = 2
    private val PROJECTION_OWNER_ACCOUNT_INDEX: Int = 3

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        Log.d(TAG, "onComplicationRequest() id: ${request.complicationInstanceId}")

        Thread {
            // Run query
            val uri: Uri = CalendarContract.Calendars.CONTENT_URI
            val selection: String = "((${CalendarContract.Calendars.ACCOUNT_NAME} = ?) AND (" +
                "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?) AND (" +
                "${CalendarContract.Calendars.OWNER_ACCOUNT} = ?))"
            val selectionArgs: Array<String> =
                arrayOf("hera@example.com", "com.example", "hera@example.com")
            Log.d(TAG, "uri=" + uri)
            val cur: Cursor? =
                contentResolver.query(uri, EVENT_PROJECTION, null, null, null)
            Log.d(TAG, "cur=" + cur)

            if (cur != null) {
                // Use the cursor to step through the returned records
                while (cur.moveToNext()) {
                    // Get the field values
                    val calID: Long = cur.getLong(PROJECTION_ID_INDEX)
                    val displayName: String = cur.getString(PROJECTION_DISPLAY_NAME_INDEX)
                    val accountName: String = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX)
                    val ownerName: String = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX)
                    // Do something with the values...
                    Log.d(TAG, "Query: $calID, $displayName, $accountName, $ownerName")
                }
                Log.d(TAG, "Done with cursor")
            } else {
                Log.d(TAG, "Null cursor")
            }
        }.start()

        return when (request.complicationType) {

            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(
                    text = "Short"
                ).build(),
                contentDescription = PlainComplicationText.Builder(text = "Calender")
                    .build()
            )
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder(
                    text = "Line 0\nHere is line 1 and one that is long\nline 2\nline 3\nline 4\nLine5"
                ).build(),
                contentDescription = PlainComplicationText.Builder(text = "Calender")
                    .build()
            )
                .build()

            else -> {
                Log.w(TAG, "Unexpected complication type ${request.complicationType}")
                null
            }
        }
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        Log.d(TAG, "Deactivated")
    }

}
