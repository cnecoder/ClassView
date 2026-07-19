package com.example.schedule.data.remote

data class HolidayResponse(
    val code: Int,                      // 0=成功
    val holiday: Map<String, HolidayInfo>?  // key="01-01"
)

data class HolidayInfo(
    val holiday: Boolean,               // true=放假
    val name: String,                   // "元旦"
    val wage: Int,                      // 薪资倍数
    val date: String                    // "yyyy-MM-dd"
)
