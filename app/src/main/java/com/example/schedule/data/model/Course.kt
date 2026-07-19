package com.example.schedule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val daysOfWeek: String = "",          // "1,3,5" 多选上课日
    val startTime: String,              // "HH:mm"
    val endTime: String,                // "HH:mm"
    val startDate: String,              // "yyyy-MM-dd"
    val endDate: String,                // "yyyy-MM-dd"
    val repeatWeeks: String,            // "1,2,3,5-18"
    val skipHolidays: Boolean = true,
    val restDays: String = "6,7",       // 默认周六日休息
    val enableAlarm: Boolean = false,
    val alarmMinutesBefore: Int = 15,
    val alarmRepeatInterval: Int = 5,
    val alarmRepeatCount: Int = 3,
    val color: Int = 0xFF4CAF50.toInt(),
    val note: String = ""
)
