package com.ofd.complications

import android.content.ContentResolver
import android.provider.CalendarContract
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataRequest
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.*

object OFDCalendar {
    val TAG = this.javaClass.simpleName

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

    fun getCalendars(contentResolver: ContentResolver) {
        // Run query
//        calendarItemAdapter.clearData()
        val selection = ""
        val selectionArgs = emptyArray<String>()
        val cur = contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
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
            Log.d(
                TAG,
                "data: $calId\t$displayName\t$name\t$color\t$visible\t$syncEvents\t$accountName\t$accountType"
            )
        }
        cur?.close()
    }

    private val sdf = SimpleDateFormat("EEE hh:mm aa ZZZ", Locale.US)

    fun getEvents(contentResolver: ContentResolver) {
        // Run query
//        calendarItemAdapter.clearData()
        val selection =
            "((${CalendarContract.Events.DTEND} > ?) AND (" + "${CalendarContract.Events.DTEND} < ?))"
        val selectionArgs: Array<String> = arrayOf(
            System.currentTimeMillis().toString(),
            (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000).toString()
        )
//        val selection = ""
//        val selectionArgs = emptyArray<String>()
        val cur = contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION
            ),
            selection, selectionArgs,
            CalendarContract.Events.DTSTART,
        )
        Log.d(TAG, "Events: $cur")
        Log.d(TAG, "CURs: ${cur?.count}")
        while (cur?.moveToNext() == true) {
            val dtstart = sdf.format(cur.getLong(0))
            val dtend = sdf.format(cur.getLong(1))
            val title = cur.getString(2)
            val description = cur.getString(3)
            Log.d(
                TAG, "event: $dtstart\t$dtend\t($title)\t($description)"
            )
        }
        cur?.close()
    }

    data class EVENT(
        var dtstart: Long,
        var dtend: Long,
        val title: String,
        val allday: Int,
        val eventtz: String,
        val eventendtz: String,
    ) : Comparable<EVENT> {
        override fun compareTo(other: EVENT): Int {
            val st = dtstart - other.dtstart
            if (st > 0) return 1 else if (st < 0) return -1
            if (equals(other)) return 0
            val hc = hashCode() - other.hashCode()
            if (hc > 0) return 1 else return -1
        }
    }

    fun setEventData(contentResolver: ContentResolver, dataClient: DataClient) {
        val selection =
            "((${CalendarContract.Events.DTSTART} >= ?) AND (" + "${CalendarContract.Events.DTSTART} < ?)) OR " + "((${CalendarContract.Events.DTSTART} < ?) AND (" + "${CalendarContract.Events.DTEND} >= ?))"
        val now = System.currentTimeMillis()
        val nowstr = now.toString()
        val weekstr = (now + 7 * 24 * 60 * 60 * 1000).toString()
        val selectionArgs: Array<String> = arrayOf(
            nowstr, weekstr, nowstr, nowstr
        )
        contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.EVENT_TIMEZONE,
                CalendarContract.Events.EVENT_END_TIMEZONE
            ),
            selection, selectionArgs,
            CalendarContract.Events.DTSTART,
        ).use { cur ->
            Log.d(TAG, "CURs: ${cur?.count}")
            val sdf = SimpleDateFormat("EEE hh:mm aa ZZZ", Locale.US)
            val localtz = TimeZone.getDefault()
            val offset = localtz.rawOffset.toLong()
            val baos = ByteArrayOutputStream()
            val sorted = sortedSetOf<EVENT>()
            ObjectOutputStream(baos).use { oos ->
                oos.writeLong(System.currentTimeMillis())
                while (cur?.moveToNext() == true) {
                    var dtstart = cur.getLong(0)
                    var dtend = cur.getLong(1)
                    val title = cur.getString(2)
                    val allday = cur.getInt(3)
                    val eventtz = cur.getString(4)
                    val eventendtz = cur.getString(5) ?: eventtz

                    if (allday == 1) {
                        if ("UTC".equals(eventtz)) {
                            dtstart -= offset
                            dtend -= offset
                        }
                    }

                    sorted.add(EVENT(dtstart, dtend, title, allday, eventtz, eventendtz))
                }

                sorted.forEach {
                    Log.d(
                        TAG,
                        "event: ${sdf.format(it.dtstart)}\t${sdf.format(it.dtend)}\t${it.allday}\t${it.eventtz}\t${it.eventendtz}\t(${it.title})"
                    )
                    oos.writeLong(it.dtstart)
                    oos.writeLong(it.dtend)
                    oos.writeObject(it.title)
                    oos.writeInt(it.allday)
                }

                oos.writeLong(-1)
            }
            dataClient.putDataItem(PutDataRequest.create("/calendar/events").apply {
                data = baos.toByteArray()
            })
        }
    }
}
