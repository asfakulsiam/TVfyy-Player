package com.example.data.local.converter

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class MapTypeConverter {
    private val moshi = Moshi.Builder().build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    private val adapter = moshi.adapter<Map<String, String>>(mapType)

    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String {
        if (map == null || map.isEmpty()) return "{}"
        return try {
            adapter.toJson(map)
        } catch (_: Exception) {
            "{}"
        }
    }

    @TypeConverter
    fun toStringMap(json: String?): Map<String, String> {
        if (json.isNullOrBlank() || json == "{}") return emptyMap()
        return try {
            adapter.fromJson(json) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
