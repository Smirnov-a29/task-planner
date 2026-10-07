package com.example.taskplanner.data.repository

import com.example.taskplanner.data.api.ApiService
import com.example.taskplanner.data.local.DataStoreManager
import com.example.taskplanner.data.model.ApiResult
import com.example.taskplanner.data.model.AuthResponse
import com.example.taskplanner.data.model.LoginRequest
import com.example.taskplanner.data.model.RegisterRequest
import com.example.taskplanner.data.model.SuccessResponse

/** Работа с регистрацией, входом и хранением сессии пользователя. */
class AuthRepository(
    private val api: ApiService,
    private val dataStore: DataStoreManager
) {

    suspend fun register(
        name: String,
        login: String,
        email: String,
        password: String,
        role: String
    ): ApiResult<SuccessResponse> =
        safeApiCall { api.register(RegisterRequest(name, login, email, password, role)) }

    suspend fun login(login: String, password: String): ApiResult<AuthResponse> {
        val result = safeApiCall { api.login(LoginRequest(login, password)) }
        if (result is ApiResult.Success) {
            val data = result.data
            dataStore.saveAuth(data.token, data.role, data.name, data.email)
        }
        return result
    }

    suspend fun logout() = dataStore.clearAuthData()
}
