package com.loyalstring.rfid.data.local.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rscja.deviceapi.entity.UHFTAGInfo
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "bulk_items",
    indices = [Index(value = ["epc"], unique = true)]
)
data class BulkItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productName: String?,
    val itemCode: String?,
    val rfid: String?,
    val grossWeight: String?,
    val stoneWeight: String?,
    val diamondWeight: String?,
    val netWeight: String?,
    val category: String?,
    val design: String?,
    val purity: String?,
    val makingPerGram: String?,
    val makingPercent: String?,
    val fixMaking: String?,
    val fixWastage: String?,
    val stoneAmount: String?,
    val diamondAmount: String?,
    val sku: String?,
    var epc: String?,
    val vendor: String?,
    val tid: String?,
    val box: String?,
    val designCode: String?,
    val productCode: String?,
    val imageUrl: String?,
    val totalQty: Int,
    val pcs: Int?,
    val matchedPcs: Int?,
    val totalGwt: Double?,
    val matchGwt: Double?,
    val totalStoneWt: Double?,
    val matchStoneWt: Double?,
    val totalNetWt: Double,
    val matchNetWt: Double?,
    val unmatchedQty: Int?,
    val matchedQty: Int?,
    val unmatchedGrossWt: Double?,
    val mrp: Double,
    val counterName: String?,
    val counterId: Int?,
    val boxId: Int?,
    val boxName: String?,
    val branchId: Int?,
    val branchName: String?,
    val packetId: Int?,
    val packetName: String?,
    val scannedStatus: String?,
    val categoryId: Int,
    val productId: Int?,
    val branchType: String?,
    val designId: Int?,
    var isScanned: Boolean = false
) : Parcelable {
    // ❗ Declare outside constructor, so it's excluded from Parcelable
    @IgnoredOnParcel
    var uhfTagInfo: UHFTAGInfo? = null
}
