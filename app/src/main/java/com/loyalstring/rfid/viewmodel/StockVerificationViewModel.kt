package com.loyalstring.rfid.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.report.BatchDetailsResponse
import com.loyalstring.rfid.data.model.report.Item
import com.loyalstring.rfid.data.model.report.ScanBatchRequest
import com.loyalstring.rfid.data.model.report.SessionItem
import com.loyalstring.rfid.data.model.report.SessionListResponse
import com.loyalstring.rfid.data.model.report.StockVerificationReqReport
import com.loyalstring.rfid.data.model.report.StockVerificationResponseReport
import com.loyalstring.rfid.repository.StockVerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockVerificationViewModel @Inject constructor(
    private val repository: StockVerificationRepository
) : ViewModel() {

    private val _reportState =
        MutableStateFlow<UiState<StockVerificationResponseReport>>(UiState.Idle)

    val reportState: StateFlow<UiState<StockVerificationResponseReport>> =
        _reportState
    private var fullReport: StockVerificationResponseReport? = null

    private val _detailState = MutableStateFlow<UiState<List<Item>>>(UiState.Idle)
    val detailState: StateFlow<UiState<List<Item>>> = _detailState

    private val _sessionState =
        MutableStateFlow<UiState<SessionListResponse>>(UiState.Idle)

    val sessionState: StateFlow<UiState<SessionListResponse>> =
        _sessionState

    private val _batchDetailsState =
        MutableStateFlow<UiState<BatchDetailsResponse>>(UiState.Idle)

    val batchDetailsState: StateFlow<UiState<BatchDetailsResponse>> =
        _batchDetailsState

    private var originalSessions: List<SessionItem> = emptyList()

    fun fetchStockVerificationReport(request: StockVerificationReqReport) {
        viewModelScope.launch {

            _reportState.value = UiState.Loading

            try {
                val response = repository.getStockVerificationReport(request)
                _reportState.value = UiState.Success(response)
                fullReport=response

            } catch (e: Exception) {
                _reportState.value =
                    UiState.Error(e.message ?: "Something went wrong")
            }
        }
    }



    fun fetchDetailItems(
        branchId: String?,
        type: String?,
        date: String?,
        categoryId: String?,
        productId: String?,
        designId: String?,
        clientCode: String
    ) {

        viewModelScope.launch {

            _detailState.value = UiState.Loading

            try {

                Log.d(
                    "DETAIL_DEBUG",
                    "branch=$branchId | type=$type | date=$date | category=$categoryId | product=$productId | design=$designId"
                )

                if (date.isNullOrEmpty()) {
                    _detailState.value = UiState.Error("Invalid date")
                    return@launch
                }

                // 🔹 Fetch fresh report data
                val response = repository.getStockVerificationReport(
                    StockVerificationReqReport(
                        ClientCode = clientCode,
                        ReportDate = date
                    )
                )

                if (response.Branches.isNullOrEmpty()) {
                    _detailState.value = UiState.Error("No data available")
                    return@launch
                }

                // 🔹 Convert IDs safely
                val branchIdInt = branchId?.toIntOrNull()
                val categoryIdInt = categoryId?.toIntOrNull()?.takeIf { it > 0 }
                val productIdInt = productId?.toIntOrNull()?.takeIf { it > 0 }
                val designIdInt = designId?.toIntOrNull()?.takeIf { it > 0 }

                // 🔹 Find Branch
                val branch = response.Branches
                    ?.firstOrNull { it.BranchId == branchIdInt }

                if (branch == null) {
                    _detailState.value = UiState.Error("Branch not found")
                    return@launch
                }

                // 🔹 Category Filter
                val categories = branch.Categories
                    ?.filter { category ->
                        categoryIdInt == null || category.CategoryId == categoryIdInt
                    }
                    .orEmpty()

                // 🔹 Product Filter
                val products = categories
                    .flatMap { it.Products.orEmpty() }
                    .filter { product ->
                        productIdInt == null || product.ProductId == productIdInt
                    }

                // 🔹 Design Filter
                val designs = products
                    .flatMap { it.Designs.orEmpty() }
                    .filter { design ->
                        designIdInt == null || design.DesignId == designIdInt
                    }

                // 🔹 Collect Items
                val items = designs
                    .flatMap { it.Items.orEmpty() }

                if (items.isEmpty()) {
                    _detailState.value = UiState.Success(emptyList())
                    return@launch
                }

                // 🔹 Status Filter
                val finalItems = when (type?.uppercase()) {

                    "TOTAL" -> items

                    "MATCHED" -> items.filter {
                        it.Status?.equals("Matched", true) == true
                    }

                    "UNMATCHED" -> items.filter {
                        it.Status?.equals("Unmatched", true) == true
                    }

                    else -> items
                }

                _detailState.value = UiState.Success(finalItems)

            } catch (e: Exception) {

                Log.e("DETAIL_ERROR", e.message ?: "Unknown error")

                _detailState.value =
                    UiState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun resetState() {
        _reportState.value = UiState.Idle
    }

    fun fetchBatchDetails(
        clientCode: String,
        scanBatchId: String
    ) {

        viewModelScope.launch {

            _batchDetailsState.value = UiState.Loading

            try {

                val result = repository.getBatchDetails(
                    ScanBatchRequest(
                        ClientCode = clientCode,
                        ScanBatchId = scanBatchId,
                        ReturnAllData= true
                    )
                )

                _batchDetailsState.value = UiState.Success(result)

            } catch (e: Exception) {

                _batchDetailsState.value =
                    UiState.Error(e.message ?: "Unknown error")

            }

        }
    }


/*
    fun fetchSessionsFiltered(
        clientCode: String,
        branchId: Int?,
        fromDate: String,
        toDate: String
    ) {

        viewModelScope.launch {

            _sessionState.value = UiState.Loading

            try {

                val response = repository.getFilteredSessions(
                    clientCode,
                    branchId,
                    fromDate,
                    toDate
                )

                _sessionState.value = UiState.Success(response)

            } catch (e: Exception) {

                _sessionState.value =
                    UiState.Error(e.message ?: "Error")
            }
        }
    }*/
fun filterSessions(
    branchId: Int?,
    fromDate: String,
    toDate: String
) {

    println("========== FILTER START ==========")
    println("BranchId Filter = $branchId")
    println("FromDate = $fromDate")
    println("ToDate = $toDate")

    println("Original Sessions Count = ${originalSessions.size}")

    originalSessions.forEach { session ->
        println("Session -> BranchId=${session.BranchId}, Date=${session.StartedOn}")
    }

    val filtered = originalSessions.filter { session ->

        val branchMatch =
            branchId == null || session.BranchId == branchId

        val date = session.StartedOn?.substring(0, 10)

        val dateMatch =
            date != null && date >= fromDate && date <= toDate

        println(
            "Checking Session -> BranchId=${session.BranchId}, Date=$date | " +
                    "BranchMatch=$branchMatch | DateMatch=$dateMatch"
        )

        branchMatch && dateMatch
    }

    println("Filtered Count = ${filtered.size}")

    filtered.forEach {
        println("Filtered Session -> BranchId=${it.BranchId}, Date=${it.StartedOn}")
    }

    val current = (_sessionState.value as? UiState.Success)?.data ?: return

    _sessionState.value =
        UiState.Success(
            current.copy(Sessions = filtered)
        )

    println("========== FILTER END ==========")
}

    fun fetchSessions(clientCode: String) {

        viewModelScope.launch {

            _sessionState.value = UiState.Loading

            try {

                val result = repository.getSessionList(
                    ClientCodeRequest(clientCode)
                )

                // ⭐ SAVE ORIGINAL DATA HERE
                originalSessions = result.Sessions ?: emptyList()

                _sessionState.value = UiState.Success(result)

            } catch (e: Exception) {

                _sessionState.value =
                    UiState.Error(e.message ?: "Unknown error")

            }
        }
    }
}