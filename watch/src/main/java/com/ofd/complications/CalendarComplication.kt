package com.ofd.complications

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.ofd.apis.purpleair.PurpleAirActivity2

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
            ).setTapAction(tapAction()).build()

            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData =
        LongTextComplicationData.Builder(
            PlainComplicationText.Builder("line1\nline2\nline3\nline4\nline5").build(),
            PlainComplicationText.Builder("").build()
        ).build()

    fun Context.tapAction(): PendingIntent? {
        val builder: Uri.Builder = CalendarContract.CONTENT_URI.buildUpon()
        builder.appendPath("time")
        ContentUris.appendId(builder, System.currentTimeMillis());
        val intent = applicationContext.packageManager.getLaunchIntentForPackage("com.samsung.android.calendar")
//            Intent(Intent.ACTION_VIEW)
//            .setData(Uri.Builder().scheme("content").path("com.samsung.android.app.calendar.view.daily.DailyActivity").build()) //builder.build())
//            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

}
