package com.example.schedule.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "class_instances",
    indices = [Index(value = ["courseId"]), Index(value = ["date"])]
)
data class ClassInstance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val date: String,                   // "yyyy-MM-dd"
    val startTime: String,              // "HH:mm"
    val endTime: String,                // "HH:mm"
    val enableAlarm: Boolean = false,
    val alarmMinutesBefore: Int = 15,
    val alarmRepeatInterval: Int = 5,
    val alarmRepeatCount: Int = 3,
    val manuallyEdited: Boolean = false,
    val note: String = ""               // 后续扩展：反馈/备注
)
