package com.example.schedule.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.schedule.data.model.Alarm
import com.example.schedule.data.model.ClassInstance
import com.example.schedule.data.model.Course
import com.example.schedule.data.model.Holiday

@Database(
    entities = [Course::class, Holiday::class, Alarm::class, ClassInstance::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun holidayDao(): HolidayDao
    abstract fun alarmDao(): AlarmDao
    abstract fun classInstanceDao(): ClassInstanceDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val ctx = context.applicationContext
                    try {
                        Room.databaseBuilder(ctx, AppDatabase::class.java, "schedule.db")
                            .fallbackToDestructiveMigration()
                            .build()
                    } catch (e: Exception) {
                        // 迁移失败时删除旧 DB 文件重建
                        ctx.deleteDatabase("schedule.db")
                        Room.databaseBuilder(ctx, AppDatabase::class.java, "schedule.db")
                            .fallbackToDestructiveMigration()
                            .build()
                    }.also { instance = it }
                }
            }
        }
    }
}
