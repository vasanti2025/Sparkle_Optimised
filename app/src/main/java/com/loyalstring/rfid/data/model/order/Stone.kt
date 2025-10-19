package com.loyalstring.rfid.data.model.order
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Stone(  val Id: Int,
                   val StoneName: String?,
                   val StoneWeight: String?,
                   val StonePieces: String?,
                   val StoneRate: String?, // or Double? if numeric
                   val StoneAmount: String?,
                   val Description: String?,
                   val ClientCode: String?,
                   val LabelledStockId: Int?,
                   val CompanyId: Int?,
                   val CounterId: Int?,
                   val BranchId: Int?,
                   val EmployeeId: Int?,
                   val CreatedOn: String?,
                   val LastUpdated: String?,
                   val StoneLessPercent: String?,
                   val StoneCertificate: String?,
                   val StoneSettingType: String?,
                   val StoneRatePerPiece: String?,
                   val StoneRateKarate: String?,
                   val StoneStatusType: String?) : Parcelable
