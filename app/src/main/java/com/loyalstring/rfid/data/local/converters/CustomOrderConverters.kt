package com.loyalstring.rfid.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.loyalstring.rfid.data.model.order.*

class CustomOrderConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromCustomOrderItemList(value: List<CustomOrderItem>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCustomOrderItemList(value: String): List<CustomOrderItem> {
        val listType = object : TypeToken<List<CustomOrderItem>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromPaymentList(value: List<Payment>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toPaymentList(value: String): List<Payment> {
        val listType = object : TypeToken<List<Payment>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromURDPurchaseList(value: List<URDPurchase>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toURDPurchaseList(value: String): List<URDPurchase> {
        val listType = object : TypeToken<List<URDPurchase>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromCustomer(value: Customer): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toCustomer(value: String): Customer {
        return gson.fromJson(value, Customer::class.java)
    }
}