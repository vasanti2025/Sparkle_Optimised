package com.loyalstring.rfid.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.loyalstring.rfid.data.model.addSingleItem.Diamond
import com.loyalstring.rfid.data.model.addSingleItem.SKUStoneMain
import com.loyalstring.rfid.data.model.addSingleItem.SKUVendor

class SKUTypeConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromSKUVendorList(value: List<SKUVendor>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toSKUVendorList(value: String): List<SKUVendor> {
        val listType = object : TypeToken<List<SKUVendor>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromDiamondList(value: List<Diamond>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toDiamondList(value: String): List<Diamond> {
        val listType = object : TypeToken<List<Diamond>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromSKUStoneMainList(value: List<SKUStoneMain>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toSKUStoneMainList(value: String): List<SKUStoneMain> {
        val listType = object : TypeToken<List<SKUStoneMain>>() {}.type
        return gson.fromJson(value, listType)
    }
}