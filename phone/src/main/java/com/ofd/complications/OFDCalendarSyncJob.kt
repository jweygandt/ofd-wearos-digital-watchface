package com.ofd.complications

import android.app.job.JobInfo
import android.app.job.JobInfo.TriggerContentUri
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlin.jvm.internal.Intrinsics

class OFDCalendarSyncJob : JobService() {

    override fun onStopJob(params: JobParameters): Boolean {
        Intrinsics.checkNotNullParameter(params, "params")
        return false
    }

    companion object {
        private const val JOB_ID = 42
        private const val TAG = "OFDCalendarSyncJob"
        private var sRegistered = false

        fun register(context: Context) {
            Intrinsics.checkNotNullParameter(context, "context")
            try {
                val build = JobInfo.Builder(
                    JOB_ID, ComponentName(
                        context, OFDCalendarSyncJob::class.java
                    )
                ).addTriggerContentUri(
                    TriggerContentUri(
                        CalendarContract.CONTENT_URI, TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS
                    )
                ).setTriggerContentUpdateDelay(1000L).build()
                val jobScheduler = context.getSystemService(
                    JobScheduler::class.java
                ) as JobScheduler
                val valueOf =
                    if (jobScheduler != null) Integer.valueOf(jobScheduler.schedule(build)) else null
                if (valueOf != null && valueOf.toInt() == 1) {
                    sRegistered = true
                    Log.i(
                        TAG, "Registered OFDCalendarSyncJob on Plugin."
                    )
                    return
                }
                Log.i(
                    TAG, "Failed to register OFDCalendarSyncJob on Plugin."
                )
                sRegistered = false
            } catch (e: Exception) {
                Log.e(
                    TAG, "job is not scheduled due to unexpected Exception : " + e.message
                )
                sRegistered = false
            }
        }

        fun isRegistered(context: Context, z: Boolean): Boolean {
            Intrinsics.checkNotNullParameter(context, "context")
            if (!sRegistered || z) {
                val jobScheduler = context.getSystemService(
                    JobScheduler::class.java
                ) as JobScheduler
                return if (jobScheduler == null || jobScheduler.getPendingJob(JOB_ID) == null) false else true
            }
            return true
        }
    }

    override fun onStartJob(params: JobParameters): Boolean {
        Intrinsics.checkNotNullParameter(params, "params")
        Log.i(TAG, "Calendar sync job is started: " + params.triggeredContentUris?.map{ u -> u.toString() })
        Thread {
            try {
                Log.i(TAG, "Start SyncData.")
                OFDCalendar.setEventData(contentResolver,Wearable.getDataClient(applicationContext))
                Log.i(TAG, "Finish SyncData.")
            } catch (e: Exception) {
                Log.e(TAG, "Exception on onStartJob : " + e.message)
            }
        }.start()
        register(this)
        return false
    }
}
