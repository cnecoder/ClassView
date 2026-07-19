package com.example.schedule.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holidays")
data class Holiday(
    @PrimaryKey val date: String,       // "yyyy-MM-dd"
    val name: String,                    // "元旦"
    val isOffDay: Boolean               // true=放假 false=调休上班
)
