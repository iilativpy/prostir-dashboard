package com.prostir.dashboard.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** Admin API, see "Admin API (mobile app)" in prostir-backend/README.md. */
interface AdminApi {
    @POST("api/admin/login")
    suspend fun login(@Body body: LoginRequest): Envelope<LoginData>

    @POST("api/admin/logout")
    suspend fun logout(): Response<ResponseBody>

    @GET("api/admin/stats/occupancy")
    suspend fun occupancy(@Query("year") year: Int?): Envelope<Occupancy>

    @GET("api/admin/stats/booking-pace")
    suspend fun bookingPace(@Query("month") month: Int?): Envelope<BookingPace>

    @GET("api/admin/stats/arrivals")
    suspend fun arrivals(@Query("from") from: String?, @Query("to") to: String?): Envelope<Arrivals>
}
