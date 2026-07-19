package com.example.schedule.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface HolidayApi {

    /** 获取某年全部节假日 */
    @GET("api/holiday/year/{year}")
    suspend fun getYearHolidays(@Path("year") year: Int): HolidayResponse

    /** 获取某年某月节假日 */
    @GET("api/holiday/year/{year}/{month}")
    suspend fun getMonthHolidays(
        @Path("year") year: Int,
        @Path("month") month: Int
    ): HolidayResponse
}
