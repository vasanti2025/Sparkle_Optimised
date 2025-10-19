package com.loyalstring.rfid.viewmodel

import androidx.lifecycle.ViewModel
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.repository.BulkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ProductListViewModel @Inject constructor(
    repository: BulkRepository
) : ViewModel() {

    val productList: Flow<List<BulkItem>> = repository.getAllBulkItems()

}