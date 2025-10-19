package com.loyalstring.rfid.data.local.converters
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.loyalstring.rfid.data.model.order.CustomOrderItem
import com.loyalstring.rfid.data.model.order.Customer
import com.loyalstring.rfid.data.model.order.Payment

class CustomOrderTypeConverters {

    // Convert List<CustomOrderItem> to JSON string
    @TypeConverter
    fun fromCustomOrderItemList(value: List<CustomOrderItem>?): String? {
        val gson = Gson()
        val type = object : TypeToken<List<CustomOrderItem>>() {}.type
        return gson.toJson(value, type)
    }

    // Convert JSON string to List<CustomOrderItem>
    @TypeConverter
    fun toCustomOrderItemList(value: String?): List<CustomOrderItem>? {
        val gson = Gson()
        val type = object : TypeToken<List<CustomOrderItem>>() {}.type
        return gson.fromJson(value, type)
    }

    // Convert List<Payment> to JSON string
    @TypeConverter
    fun fromPaymentList(value: List<Payment>?): String? {
        val gson = Gson()
        val type = object : TypeToken<List<Payment>>() {}.type
        return gson.toJson(value, type)
    }

    // Convert JSON string to List<Payment>
    @TypeConverter
    fun toPaymentList(value: String?): List<Payment>? {
        val gson = Gson()
        val type = object : TypeToken<List<Payment>>() {}.type
        return gson.fromJson(value, type)
    }

    // Convert Customer object to JSON string
    @TypeConverter
    fun fromCustomer(value: Customer?): String? {
        val gson = Gson()
        return gson.toJson(value)
    }

    // Convert JSON string to Customer object
    @TypeConverter
    fun toCustomer(value: String?): Customer? {
        val gson = Gson()
        return gson.fromJson(value, Customer::class.java)
    }
}
