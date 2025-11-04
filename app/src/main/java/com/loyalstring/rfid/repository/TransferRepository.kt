package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.local.entity.TransferTypeEntity
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.stockTransfer.CancelStockTransfer
import com.loyalstring.rfid.data.model.stockTransfer.CancelStockTransferResponse
import com.loyalstring.rfid.data.model.stockTransfer.STApproveRejectRequest
import com.loyalstring.rfid.data.model.stockTransfer.STApproveRejectResponse
import com.loyalstring.rfid.data.model.stockTransfer.StockInOutRequest
import com.loyalstring.rfid.data.model.stockTransfer.StockTransferInOutResponse
import com.loyalstring.rfid.data.remote.data.StockTransferRequest
import kotlinx.coroutines.flow.StateFlow

interface TransferRepository {
    val transferTypes: StateFlow<List<TransferTypeEntity>>

    suspend fun refreshTransferTypes(request: ClientCodeRequest)
    suspend fun submitStockTransfer(request: StockTransferRequest): Result<Unit>
    suspend fun getAllStockTransfers(request: StockInOutRequest): Result<List<StockTransferInOutResponse>>
    suspend fun stApproveReject(request: STApproveRejectRequest): Result<STApproveRejectResponse>
    suspend fun cancelStockTransfer(request: CancelStockTransfer): Result<CancelStockTransferResponse>

}
