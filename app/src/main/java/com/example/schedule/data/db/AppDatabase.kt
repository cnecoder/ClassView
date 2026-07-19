package com.example.schedule.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.schedule.data.model.Alarm
import com.example.schedule.data.model.Course
import com.example.schedule.data.model.Holiday

@Database(
    entities = [Course::class, Holiday::class, Alarm::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun holidayDao(): HolidayDao
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "schedule.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
