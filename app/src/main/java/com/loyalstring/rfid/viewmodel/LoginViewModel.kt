package com.loyalstring.rfid.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.model.login.LoginRequest
import com.loyalstring.rfid.data.model.login.LoginResponse
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.repository.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LoginRepository // or whatever your dependency is
) : ViewModel() {

    private val _loginResponse = MutableLiveData<Resource<LoginResponse>>()
    val loginResponse: LiveData<Resource<LoginResponse>> = _loginResponse

    fun login(request: LoginRequest, rememberMe: Boolean) {
        viewModelScope.launch {
            _loginResponse.value = Resource.Loading()
            try {
                val response = repository.login(request)

                val body = response.body()
                if (response.isSuccessful && body != null && body.employee != null) {
                    _loginResponse.value = Resource.Success(body)
                    setRememberMe(rememberMe)
                } else {
                    _loginResponse.value = Resource.Error("Invalid login credentials.")
                }
            } catch (e: Exception) {
                _loginResponse.value = Resource.Error("Exception: ${e.message}")
            }
        }
    }

   /* fun isUserRemembered(): Boolean {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("remember_me", false)
    }*/
   suspend fun isUserRemembered(): Boolean = withContext(Dispatchers.IO) {
       val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.getBoolean("remember_me", false)
   }

    private fun setRememberMe(remember: Boolean) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit() { putBoolean("remember_me", remember) }
    }

    fun clearRememberMe() {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit() { clear() }
    }
}