package com.ofd.complications

import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

class CalendarComplication : SuspendingComplicationDataSourceService() {
    private val TAG = this.javaClass.simpleName

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
//        Log.d(
//            TAG, "Updating: ${request.complicationType}: ${
//                CalendarListenerService.events.get().joinToString("\n")
//            }"
//        )
        return when (request.complicationType) {
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                PlainComplicationText
                    .Builder(CalendarListenerService.events.get().joinToString("\n")).build(),
                PlainComplicationText.Builder("").build()
            ).build()

            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData =
        LongTextComplicationData.Builder(
            PlainComplicationText.Builder("line1\nline2\nline3\nline4\nline5").build(),
            PlainComplicationText.Builder("").build()
        ).build()
}
