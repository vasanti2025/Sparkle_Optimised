package com.loyalstring.rfid.data.model.addSingleItem

data class DesignModel(
    val Id: Int,
    val CategoryId: Int,
    val ProductId: Int,
    val DesignName: String,
    val Description: String?,
    val Slug: String?,
    val LabelCode: String?,
    val Status: String?,
    val ClientCode: String,
    val MinQuantity: String?,     // nullable string as per the JSON
    val MinWeight: String?,       // nullable string as per the JSON
    val EmployeeCode: String?,
    val CategoryName: String,
    val ProductName: String,
    val CreatedOn: String,
    val LastUpdated: String
)
