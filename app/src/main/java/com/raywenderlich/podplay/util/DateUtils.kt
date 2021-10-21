package com.raywenderlich.podplay.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

object DateUtils {

    fun jsonDateToShortDate(jsonDate: String?): String {
        if (jsonDate == null) {
            return "-"
        }

        val inFormat = SimpleDateFormat("dd-MM-yyyy'T'HH:mm:ss", Locale.getDefault())
        val date = inFormat.parse(jsonDate) ?: return "_"
        val outputFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())

        return outputFormat.format(date)
    }
}