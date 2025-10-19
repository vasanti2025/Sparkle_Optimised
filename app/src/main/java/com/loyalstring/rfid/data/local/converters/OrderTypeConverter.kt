package com.loyalstring.rfid.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.loyalstring.rfid.data.model.order.Diamond
import com.loyalstring.rfid.data.model.order.Stone

class OrderTypeConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromStoneList(value: List<Stone>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStoneList(value: String?): List<Stone> {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<Stone>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromDiamondList(value: List<Diamond>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toDiamondList(value: String?): List<Diamond> {
        if (value.isNullOrEmpty()) return emptyList()
        val listType = object : TypeToken<List<Diamond>>() {}.type
        return gson.fromJson(value, listType)
    }
}
