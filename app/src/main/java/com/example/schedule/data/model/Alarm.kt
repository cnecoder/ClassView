package com.example.schedule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val triggerTime: Long,              // 触发时间戳 (ms)
    val courseName: String,
    val repeatIntervalMinutes: Int = 0,
    val repeatCount: Int = 0,
    val parentAlarmId: Long = 0         // 首次闹钟 0, 重复闹钟关联首次 id
)
