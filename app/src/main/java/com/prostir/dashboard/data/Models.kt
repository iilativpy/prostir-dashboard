package com.prostir.dashboard.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Every Admin API response is wrapped in this envelope. */
@Serializable
data class Envelope<T>(
    val success: Boolean = true,
    val status: Int = 200,
    val message: String? = null,
    val data: T? = null,
)

@Serializable
data class LoginRequest(
    val login: String,
    val password: String,
    @SerialName("device_name") val deviceName: String? = null,
)

@Serializable
data class LoginData(
    val token: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class House(val id: Int, val name: String)

@Serializable
data class HousePercent(
    @SerialName("house_id") val houseId: Int,
    @SerialName("nights_booked") val nightsBooked: Int = 0,
    val percent: Double? = null,
)

@Serializable
data class MonthOccupancy(
    val month: Int,
    @SerialName("month_name") val monthName: String = "",
    /** "past", "current" or "future". For current and future months the % is what is booked so far. */
    val status: String = "past",
    @SerialName("nights_available") val nightsAvailable: Int = 0,
    @SerialName("nights_booked") val nightsBooked: Int = 0,
    val percent: Double? = null,
    val houses: List<HousePercent> = emptyList(),
) {
    val isFinished: Boolean get() = status == "past"
}

@Serializable
data class YearComparison(
    val year: Int,
    val months: List<Double?> = emptyList(),
    @SerialName("average_percent") val averagePercent: Double? = null,
)

@Serializable
data class Occupancy(
    val year: Int,
    val houses: List<House> = emptyList(),
    val months: List<MonthOccupancy> = emptyList(),
    @SerialName("average_percent") val averagePercent: Double? = null,
    val comparison: List<YearComparison> = emptyList(),
)

@Serializable
data class PaceYear(
    val year: Int,
    /** % booked on each label date; null = date is in the future. */
    val values: List<Double?> = emptyList(),
    @SerialName("final_percent") val finalPercent: Double? = null,
)

@Serializable
data class BookingPace(
    val month: Int,
    @SerialName("month_name") val monthName: String = "",
    @SerialName("houses_count") val housesCount: Int = 0,
    /** Days from the 1st of the month for each point. */
    val offsets: List<Int> = emptyList(),
    val labels: List<String> = emptyList(),
    val years: List<PaceYear> = emptyList(),
)

@Serializable
data class Guest(
    val name: String? = null,
    val surname: String? = null,
    val phone: String? = null,
    val email: String? = null,
)

@Serializable
data class ArrivalItem(
    /** "check_in" or "check_out". */
    val type: String,
    val date: String,
    @SerialName("reservation_id") val reservationId: Int,
    @SerialName("order_id") val orderId: Int? = null,
    val house: House,
    @SerialName("date_from") val dateFrom: String,
    @SerialName("date_to") val dateTo: String,
    val nights: Int = 0,
    val guests: Int? = null,
    val guest: Guest? = null,
    val comment: String? = null,
    @SerialName("admin_comment") val adminComment: String? = null,
    /** "site", "admin", "booking" or "airbnb". */
    val source: String = "",
    @SerialName("awaiting_offline_payment") val awaitingOfflinePayment: Boolean = false,
) {
    val isCheckIn: Boolean get() = type == "check_in"
}

@Serializable
data class Arrivals(
    val from: String,
    val to: String,
    val items: List<ArrivalItem> = emptyList(),
)
