package com.example.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    val ALGIERS_ZONE: ZoneId = ZoneId.of("Africa/Algiers")
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val HIJRI_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("ar"))

    fun format(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "-"
        val localDate = Instant.ofEpochMilli(timestamp).atZone(ALGIERS_ZONE).toLocalDate()
        return localDate.format(DATE_FORMATTER)
    }

    fun formatDateTime(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "-"
        val localDateTime = Instant.ofEpochMilli(timestamp).atZone(ALGIERS_ZONE).toLocalDateTime()
        return localDateTime.format(DATE_TIME_FORMATTER)
    }

    fun toHijri(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "-"
        return try {
            val localDate = Instant.ofEpochMilli(timestamp).atZone(ALGIERS_ZONE).toLocalDate()
            val hijrahDate: HijrahDate = HijrahChronology.INSTANCE.date(localDate)
            hijrahDate.format(HIJRI_FORMATTER) + " هـ"
        } catch (e: Exception) {
            "-"
        }
    }

    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val date = Instant.ofEpochMilli(timestamp).atZone(ALGIERS_ZONE).toLocalDate()
        return date.atStartOfDay(ALGIERS_ZONE).toInstant().toEpochMilli()
    }

    fun getEndOfOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val date = Instant.ofEpochMilli(timestamp).atZone(ALGIERS_ZONE).toLocalDate()
        return date.atTime(23, 59, 59, 999_000_000).atZone(ALGIERS_ZONE).toInstant().toEpochMilli()
    }

    fun getCurrentYear(): Int {
        return LocalDate.now(ALGIERS_ZONE).year
    }

    fun getRemainingDaysText(deadline: Long?): Pair<String, DeadlineStatus> {
        if (deadline == null) return "غير محدد" to DeadlineStatus.NONE
        val todayStart = getStartOfDay(System.currentTimeMillis())
        val deadlineStart = getStartOfDay(deadline)

        val diffMillis = deadlineStart - todayStart
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        return when {
            diffDays < 0 -> "متأخر بـ ${-diffDays} يوم" to DeadlineStatus.OVERDUE
            diffDays == 0 -> "اليوم هو آخر أجل" to DeadlineStatus.DUE_TODAY
            diffDays == 1 -> "متبقي يوم واحد" to DeadlineStatus.UPCOMING
            diffDays == 2 -> "متبقي يومان" to DeadlineStatus.UPCOMING
            diffDays in 3..10 -> "متبقي $diffDays أيام" to DeadlineStatus.UPCOMING
            else -> "متبقي $diffDays يوم" to DeadlineStatus.UPCOMING
        }
    }

    enum class DeadlineStatus {
        NONE, OVERDUE, DUE_TODAY, UPCOMING
    }
}
