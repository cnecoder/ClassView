package com.example.schedule.alarm

import android.content.Context
import androidx.work.*
import com.example.schedule.ScheduleApp
import java.util.concurrent.TimeUnit

/**
 * 每日定时重设闹钟（凌晨 3:00），确保节假日数据和闹钟保持最新
 */
class AlarmRescheduleWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ScheduleApp ?: return Result.failure()
        val scheduler = AlarmScheduler(applicationContext, app.courseRepository)
        scheduler.rescheduleAll(app.holidayRepository)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "alarm_reschedule"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresDeviceIdle(false)
                .build()

            val request = PeriodicWorkRequestBuilder<AlarmRescheduleWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInitialDelay(calculateDelayTo3AM(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        private fun calculateDelayTo3AM(): Long {
            val now = System.currentTimeMillis()
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = now
                set(java.util.Calendar.HOUR_OF_DAY, 3)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) {
                    add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
            }
            return cal.timeInMillis - now
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
