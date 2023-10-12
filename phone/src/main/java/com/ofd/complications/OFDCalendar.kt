package com.ofd.complications

import android.content.ContentResolver
import android.provider.CalendarContract
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataRequest
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.recur.RecurrenceRule

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
    private val xsdf = SimpleDateFormat("MM/dd/yyy hh:mm aa ZZZ", Locale.US)
    private val localtz = TimeZone.getDefault()
    private val offset = localtz.rawOffset.toLong()

    data class EVENT(
        var dtstart: Long,
        var dtend: Long,
        var durationStr: String,
        val title: String,
        val allday: Int,
        val eventtz: String,
        val eventendtz: String,
        val rrule: String?,
    ) : Comparable<EVENT> {

        var error = false
        var valid = true

        init {
            try {
                // All day events are in GMT, translate to local TZ
                val durationMs = if (durationStr != null && durationStr.length > 0) {
                    if (durationStr.endsWith("S")) durationStr = durationStr.replace("P", "PT")
                    Duration.parse(durationStr).toMillis()
                } else 0

//                Log.d(TAG, "duration: " + durationStr + ", " + durationMs)

                if (allday == 1) {
                    if ("UTC".equals(eventtz)) {
                        dtstart -= offset
                        dtend -= offset
                    }
                }

                if (rrule != null && rrule.length > 0) {
                    var start = DateTime(dtstart)
                    val rule = RecurrenceRule(rrule)
                    val r = rule.iterator(start)
                    r.fastForward(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30))

                    if (r.hasNext()) {
                        dtstart = r.nextDateTime().timestamp
                        if (dtend <= 0)
                            dtend += dtstart
                        dtend += durationMs
//                        Log.d(
//                            OFDCalendar.TAG,
//                            "Next: " + xsdf.format(nextTime.timestamp) + ", duration: " + durationMs / 1000
//                        )
                    } else {
                        valid = false
                    }
                } else if (dtend <= 0) {
                    dtend += dtstart + durationMs
                }

                // Limit duration to a single day
                dtend = Math.min(dtend, dtstart + 24L * 3600000L)
            } catch (e: Exception) {
                Log.e(OFDCalendar.TAG, "bad rule: $rrule, ${e.message}", e)
                error = true
                valid = false
            }
        }

        override fun compareTo(other: EVENT): Int {
            val st = dtstart - other.dtstart
            if (st > 0) return 1 else if (st < 0) return -1
            if (equals(other)) return 0
            val hc = hashCode() - other.hashCode()
            if (hc > 0) return 1 else return -1
        }
    }

    fun getEvents(contentResolver: ContentResolver) {
        // Run query
//        val whereClause =
//            "((${CalendarContract.Events.DTEND} > ?) AND (" + "${CalendarContract.Events.DTEND} < ?))"
//        val whereArgs: Array<String> = arrayOf(
//            System.currentTimeMillis().toString(),
//            (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000).toString()
//        )

//        val whereClause =
//            "${CalendarContract.Events.DTEND} = 0 AND ${CalendarContract.Events.RRULE} IS NOT NULL"
//        val whereArgs: Array<String> = arrayOf(
//        )

        val whereClause =
            "${CalendarContract.Events.DTEND} = 0 OR ${CalendarContract.Events.RRULE} IS NOT NULL" +
                " OR ${CalendarContract.Events.RDATE} IS NOT NULL OR ${CalendarContract.Events.EXRULE} IS NOT NULL" +
                " OR ${CalendarContract.Events.EXDATE} IS NOT NULL"
        val whereArgs: Array<String> = arrayOf(
        )

//        val whereClause =
//            "${CalendarContract.Events.RRULE} IS NOT NULL"
//        val whereArgs: Array<String> = arrayOf(
//        )

//        val whereClause = ""
//        val whereArgs = emptyArray<String>()

        val cur = contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.DURATION,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.EVENT_TIMEZONE,
                CalendarContract.Events.EVENT_END_TIMEZONE,
                CalendarContract.Events.RRULE,
                CalendarContract.Events.RDATE,
                CalendarContract.Events.EXRULE,
                CalendarContract.Events.EXDATE,
            ),
            whereClause, whereArgs,
            CalendarContract.Events.DTSTART,
        )
        Log.d(TAG, "Events: $cur")
        Log.d(TAG, "CURs: ${cur?.count}")
        while (cur?.moveToNext() == true) {
            var inx = 0;
            val dtstart = cur.getLong(inx++)
            val dtend = cur.getLong(inx++) //xsdf.format(cur.getLong(1))
            val duration = cur.getString(inx++) ?: ""
            val title = cur.getString(inx++) ?: ""
            val allday = cur.getInt(inx++)
            val eventtz = cur.getString(inx++) ?: ""
            val eventendtz = cur.getString(inx++) ?: eventtz
            val rrule = cur.getString(inx++) ?: ""
            val rdate = cur.getString(inx++) ?: ""
            val exrule = cur.getString(inx++) ?: ""
            val exdate = cur.getString(inx++) ?: ""
            Log.d(
                TAG,
                "event1: ${xsdf.format(dtstart)}\t$dtend\t$allday\t($duration)\t($title)\t($rrule)\t($rdate)\t($exrule)\t($exdate)"
            )
            EVENT(dtstart, dtend, duration, title, allday, eventtz, eventendtz, rrule)
        }
        cur?.close()
    }

    fun setEventData(contentResolver: ContentResolver, dataClient: DataClient) {
        val selection =
            "((${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} < ?)) OR " +
                "((${CalendarContract.Events.DTSTART} < ?) AND (" + "${CalendarContract.Events.DTEND} >= ?))" +
                "OR (${CalendarContract.Events.DTEND} = 0 OR ${CalendarContract.Events.RRULE} IS NOT NULL " +
                "OR ${CalendarContract.Events.RDATE} IS NOT NULL OR ${CalendarContract.Events.EXRULE} IS NOT NULL " +
                "OR ${CalendarContract.Events.EXDATE} IS NOT NULL)"
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
                CalendarContract.Events.DURATION,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.EVENT_TIMEZONE,
                CalendarContract.Events.EVENT_END_TIMEZONE,
                CalendarContract.Events.RRULE,
                CalendarContract.Events.RDATE,
                CalendarContract.Events.EXRULE,
                CalendarContract.Events.EXDATE,
            ),
            selection, selectionArgs,
            CalendarContract.Events.DTSTART,
        ).use { cur ->
            Log.d(TAG, "CURs: ${cur?.count}")
            val sdf = SimpleDateFormat("MM/dd/yy EEE hh:mm aa ZZZ", Locale.US)
            val baos = ByteArrayOutputStream()
            val sorted = sortedSetOf<EVENT>()
            ObjectOutputStream(baos).use { oos ->
                oos.writeLong(System.currentTimeMillis())
                var error = false
                while (cur?.moveToNext() == true) {
                    var inx = 0;
                    val dtstart = cur.getLong(inx++)
                    val dtend = cur.getLong(inx++) //xsdf.format(cur.getLong(1))
                    val duration = cur.getString(inx++) ?: ""
                    val title = cur.getString(inx++) ?: ""
                    val allday = cur.getInt(inx++)
                    val eventtz = cur.getString(inx++) ?: ""
                    val eventendtz = cur.getString(inx++) ?: eventtz
                    val rrule = cur.getString(inx++) ?: ""
                    val rdate = cur.getString(inx++) ?: ""
                    val exrule = cur.getString(inx++) ?: ""
                    val exdate = cur.getString(inx++) ?: ""

//                    Log.d(
//                        TAG,
//                        "event: ${xsdf.format(dtstart)}\t$dtend\t$allday\t($duration)\t($title)\t($rrule)\t($rdate)\t($exrule)\t($exdate)"
//                    )

                    if (rdate.length > 0 || exrule.length > 0 || exdate.length > 0) {
                        error = true;
                        Log.e(
                            TAG,
                            "error event: ${xsdf.format(dtstart)}\t$dtend\t$allday\t($duration)\t($title)\t($rrule)\t($rdate)\t($exrule)\t($exdate)"
                        )
                    } else {
                        val event = EVENT(
                            dtstart, dtend, duration, title, allday, eventtz, eventendtz, rrule
                        )
                        if (event.error) error = true
                        else if (event.valid) sorted.add(event)
                    }
                }

                var cnt = 0
                sorted.forEach {
                    cnt++
                    Log.d(
                        TAG,
                        "event2: ${sdf.format(it.dtstart)}\t${sdf.format(it.dtend)}\t${it.allday}\t${it.eventtz}\t${it.eventendtz}\t(${it.title})"
                    )
                    if (error && cnt == 4) {
                        oos.writeLong(it.dtstart)
                        oos.writeLong(it.dtend)
                        oos.writeObject("Errors see phone logs")
                        oos.writeInt(1)
                    }
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

