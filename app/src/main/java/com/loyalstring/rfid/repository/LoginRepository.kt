package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.login.LoginRequest
import com.loyalstring.rfid.data.model.login.LoginResponse
import com.loyalstring.rfid.data.remote.api.RetrofitInterface

import retrofit2.Response
import javax.inject.Inject

class LoginRepository @Inject constructor(
    private val apiService: RetrofitInterface
) {
    suspend fun login(request: LoginRequest): Response<LoginResponse> {
        return apiService.login(request)
    }
}
