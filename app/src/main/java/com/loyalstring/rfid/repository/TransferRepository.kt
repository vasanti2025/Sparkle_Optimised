package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.local.dao.TransferTypeDao
import com.loyalstring.rfid.data.local.entity.TransferTypeEntity
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import kotlinx.coroutines.flow.Flow

class TransferRepository(
    private val retrofitInterface: RetrofitInterface,
    private val dao: TransferTypeDao
) {
    val transferTypes: Flow<List<TransferTypeEntity>> = dao.getActiveTransferTypes()


    suspend fun refreshTransferTypes(request: ClientCodeRequest): List<TransferTypeEntity> {
        val response = retrofitInterface.getStockTransferTypes(request)
        if (response.isSuccessful) {
            val list = response.body() ?: emptyList()
            dao.clearAll()
            dao.insertAll(list) // ✅ Correct usage
            return list
        } else {
            throw Exception("Failed to load transfer types: ${response.code()}")
        }
    }


}
