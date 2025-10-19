package com.loyalstring.rfid.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "orderItem", indices = [Index(value = ["rfidCode"], unique = true)])
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val branchId: String,
    val branchName: String,
    val exhibition: String,
    val remark: String,
    val purity: String,
    val size: String,
    val length: String,
    val typeOfColor: String,
    val screwType: String,
    val polishType: String,
    val finePer: String,
    val wastage: String,
    val orderDate: String,
    val deliverDate: String,
    val productName: String,
    val itemCode: String,
    val rfidCode: String= "" ,
    val grWt: String?,
    val nWt: String?,
    val stoneAmt: String?,
    val finePlusWt: String?,
    val itemAmt: String?,
    val packingWt: String,
    val totalWt: String,
    val stoneWt: String,
    val dimondWt: String,
    val sku: String,
    val qty: String,
    val hallmarkAmt: String,
    val mrp: String,
    val image: String,
    val netAmt: String,
    val diamondAmt: String,

    val categoryId: Int?,
    val categoryName: String,
    val productId: Int,
    val productCode: String,
    val skuId: Int,
    val designid: Int,
    val designName: String,
    val purityid: Int,
    val counterId: Int,
    val counterName: String,
    val companyId: Int,
    val epc: String,
    val tid: String,

    val todaysRate: String,
    val makingPercentage: String,
    val makingFixedAmt: String,
    val makingFixedWastage: String,
    val makingPerGram: String
)
