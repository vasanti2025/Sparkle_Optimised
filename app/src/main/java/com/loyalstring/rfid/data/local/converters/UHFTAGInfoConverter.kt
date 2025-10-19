package com.loyalstring.rfid.data.local.converters

import androidx.room.TypeConverter
import com.rscja.deviceapi.entity.UHFTAGInfo

class UHFTAGInfoConverter {
    @TypeConverter
    fun fromTagInfo(tagInfo: UHFTAGInfo?): ByteArray? {
        return tagInfo?.epc?.toByteArray()
    }

    @TypeConverter
    fun toTagInfo(data: ByteArray?): UHFTAGInfo? {
        return data?.let {
            val tagInfo = UHFTAGInfo()
            tagInfo.epc = String(it)
            tagInfo
        }
    }
}
