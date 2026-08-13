package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for formatting timestamps with strict Asia/Dhaka TimeZone
 * and English UI localization (e.g., "Today, 02:30 PM" or "Aug 06, 02:30 PM").
 */
object DateUtils {
    private val dhakaZone: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")

    /**
     * Formats timestamp for Chat History & Chat Session Cards cleanly in English.
     * Examples: "Today, 02:30 PM" or "Aug 06, 02:30 PM"
     */
    fun formatSessionTimestamp(timestamp: Long): String {
        if (timestamp <= 0L) return "Just now"

        val dhakaCal = Calendar.getInstance(dhakaZone, Locale.US)
        val nowCal = Calendar.getInstance(dhakaZone, Locale.US)
        dhakaCal.timeInMillis = timestamp

        val timeSdf = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = dhakaZone }
        val timeFormatted = timeSdf.format(dhakaCal.time)

        val isToday = nowCal.get(Calendar.YEAR) == dhakaCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == dhakaCal.get(Calendar.DAY_OF_YEAR)

        return if (isToday) {
            "Today, $timeFormatted"
        } else {
            val dateSdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.US).apply { timeZone = dhakaZone }
            dateSdf.format(dhakaCal.time)
        }
    }

    /**
     * Formats time string strictly in Asia/Dhaka TimeZone.
     * Example: "02:30 PM" or "14:30:00"
     */
    fun formatTimeString(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = dhakaZone }
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats clock time for logs or terminal output strictly in Asia/Dhaka TimeZone.
     * Example: "14:30:05"
     */
    fun format24HourTime(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US).apply { timeZone = dhakaZone }
        return sdf.format(Date(timestamp))
    }
}
