package com.loyalstring.rfid.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.loyalstring.rfid.data.remote.data.UserPermissionResponse
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.repository.UserPermissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserPermissionViewModel @Inject constructor(
    private val repository: UserPermissionRepository
) : ViewModel() {

    private val _permissionResponse = MutableLiveData<Resource<Unit>>()
    val permissionResponse: LiveData<Resource<Unit>> = _permissionResponse

    fun loadPermissions(clientCode: String, userId: Int) {
        viewModelScope.launch {
            _permissionResponse.value = Resource.Loading()
            _permissionResponse.value = repository.fetchAndSavePermissions(clientCode, userId)
        }
    }

    suspend fun getAccessibleBranches(): List<String> {
        val userPerm = repository.getUserPermission()
        val json = userPerm?.branchSelectionJson ?: return emptyList()

        return try {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val list: List<Map<String, Any>> = Gson().fromJson(json, type)
            list.mapNotNull { it["Name"]?.toString()?.trim() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }


}

