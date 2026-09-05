package com.example.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    fun getTodayIso(): String {
        return isoDateFormat.format(Date())
    }

    fun getYesterdayIso(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return isoDateFormat.format(cal.time)
    }

    fun getCurrentMonthYear(): String {
        return monthYearFormat.format(Date())
    }

    fun getCurrentMonthKey(): String {
        return monthKeyFormat.format(Date())
    }

    fun formatToDisplay(isoDate: String): String {
        if (isoDate.isBlank()) return ""
        return try {
            val date = isoDateFormat.parse(isoDate) ?: return isoDate
            displayDateFormat.format(date)
        } catch (_: Exception) {
            isoDate
        }
    }

    fun getMonthKey(isoDate: String): String {
        return if (isoDate.length >= 7) isoDate.substring(0, 7) else ""
    }

    fun getMonthDisplayName(monthKey: String): String {
        return try {
            val date = monthKeyFormat.parse(monthKey) ?: return monthKey
            monthYearFormat.format(date)
        } catch (_: Exception) {
            monthKey
        }
    }

    fun getPastNDays(n: Int): List<String> {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        for (i in 0 until n) {
            list.add(isoDateFormat.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return list
    }
}

object CurrencyUtils {
    fun formatInr(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val formatted = formatter.format(amount)
        // Clean standard ₹ formatting if needed
        return if (formatted.startsWith("INR") || formatted.startsWith("₹")) {
            formatted.replace("INR", "₹").trim()
        } else {
            "₹%,.0f".format(Locale("en", "IN"), amount)
        }
    }

    fun formatInrClean(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            "₹%,d".format(Locale("en", "IN"), amount.toLong())
        } else {
            "₹%,.1f".format(Locale("en", "IN"), amount)
        }
    }
}
