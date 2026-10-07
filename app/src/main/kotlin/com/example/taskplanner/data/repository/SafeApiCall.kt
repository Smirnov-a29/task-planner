package com.example.taskplanner.data.repository

import com.example.taskplanner.data.model.ApiResult
import com.example.taskplanner.data.model.ErrorResponse
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Универсальная обёртка над сетевым вызовом.
 * IOException → «Нет подключения к серверу»; !isSuccessful → тело разбирается как ErrorResponse.
 */
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResult.Success(body)
            } else {
                ApiResult.Error("Пустой ответ сервера")
            }
        } else {
            val raw = response.errorBody()?.string()
            val message = raw?.let {
                runCatching { errorJson.decodeFromString<ErrorResponse>(it).error }.getOrNull()
            } ?: "Ошибка ${response.code()}"
            ApiResult.Error(message)
        }
    } catch (e: IOException) {
        ApiResult.Error("Нет подключения к серверу")
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Неизвестная ошибка")
    }
}
