package com.schengen.tracker.data

import androidx.room.TypeConverter
import org.json.JSONArray

class AppTypeConverters {
    @TypeConverter
    fun fromCountryCodes(value: String?): List<String> = decodeCountryCodes(value)

    @TypeConverter
    fun toCountryCodes(value: List<String>?): String = encodeCountryCodes(value.orEmpty())

    companion object {
        fun encodeCountryCodes(values: List<String>): String =
            JSONArray(values).toString()

        fun decodeCountryCodes(value: String?): List<String> {
            if (value.isNullOrBlank()) return emptyList()

            return runCatching {
                val json = JSONArray(value)
                List(json.length()) { index -> json.optString(index) }
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
            }.getOrElse {
                value.split(',', ';', '\n', '|')
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
            }
        }
    }
}
