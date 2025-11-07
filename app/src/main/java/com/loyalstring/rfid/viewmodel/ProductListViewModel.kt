package com.loyalstring.rfid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.repository.BulkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductListViewModel @Inject constructor(
    repository: BulkRepository
) : ViewModel() {

  //  val productList: Flow<List<BulkItem>> = repository.getAllBulkItems()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _productList = MutableStateFlow<List<BulkItem>>(emptyList())
    val productList: StateFlow<List<BulkItem>> = _productList

    init {
        viewModelScope.launch {
            repository.getAllBulkItems().collect { items ->
                _productList.value = items
                _isLoading.value = false
            }
        }
    }

}