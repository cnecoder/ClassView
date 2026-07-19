package com.example.schedule

import android.app.Application
import com.example.schedule.alarm.AlarmRescheduleWorker
import com.example.schedule.data.db.AppDatabase
import com.example.schedule.data.remote.HolidayApi
import com.example.schedule.data.repository.CourseRepository
import com.example.schedule.data.repository.HolidayRepository
import com.example.schedule.util.DebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ScheduleApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var courseRepository: CourseRepository
        private set
    lateinit var holidayRepository: HolidayRepository
        private set

    override fun onCreate() {
        super.onCreate()
        DebugLog.init(this)
        DebugLog.w("App", "=== APP START ===")

        // 全局异常捕获
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            DebugLog.e("App", "CRASH in ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        val db = AppDatabase.getInstance(this)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://timor.tech/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val holidayApi = retrofit.create(HolidayApi::class.java)

        courseRepository = CourseRepository(db.courseDao(), db.alarmDao(), db.classInstanceDao())
        holidayRepository = HolidayRepository(db.holidayDao(), holidayApi)

        applicationScope.launch {
            if (!holidayRepository.hasCache()) {
                val currentYear = java.time.LocalDate.now().year
                holidayRepository.fetchAndCacheHolidays(listOf(currentYear, currentYear + 1))
            }
        }

        AlarmRescheduleWorker.schedule(this)
    }
}
