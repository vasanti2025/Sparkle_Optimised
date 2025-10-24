package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.local.dao.TransferTypeDao
import com.loyalstring.rfid.data.local.entity.TransferTypeEntity
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.stockTransfer.STApproveRejectRequest
import com.loyalstring.rfid.data.model.stockTransfer.STApproveRejectResponse
import com.loyalstring.rfid.data.model.stockTransfer.StockInOutRequest
import com.loyalstring.rfid.data.model.stockTransfer.StockTransferInOutResponse
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.data.remote.data.StockTransferRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class TransferRepositoryImpl @Inject constructor(
    private val apiService: RetrofitInterface,
    dao: TransferTypeDao
) : TransferRepository {

    private val _transferTypes = MutableStateFlow<List<TransferTypeEntity>>(emptyList())
    override val transferTypes: StateFlow<List<TransferTypeEntity>> = _transferTypes

    override suspend fun refreshTransferTypes(request: ClientCodeRequest) {
        try {
            val response = apiService.getStockTransferTypes(request)
            _transferTypes.value = if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
        } catch (e: Exception) {
            _transferTypes.value = emptyList()
            throw e
        }
    }

    override suspend fun submitStockTransfer(request: StockTransferRequest): Result<Unit> {
        return try {
            val response = apiService.postStockTransfer(request)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Transfer failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllStockTransfers(request: StockInOutRequest): Result<List<StockTransferInOutResponse>> {
        return try {
            val response = apiService.getAllStockTransfer(request)
            if (response.isSuccessful) Result.success(response.body() ?: emptyList())
            else Result.failure(Exception("Failed to fetch transfers: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stApproveReject(request: STApproveRejectRequest): Result<STApproveRejectResponse> {
        return try {
            val response = apiService.approveStockTransfer(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
