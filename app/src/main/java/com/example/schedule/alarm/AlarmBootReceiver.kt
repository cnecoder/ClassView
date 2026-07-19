package com.example.schedule.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.schedule.ScheduleApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as? ScheduleApp ?: return
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            scope.launch {
                val scheduler = AlarmScheduler(context, app.courseRepository)
                scheduler.rescheduleAll(app.holidayRepository)
            }
        }
    }
}
