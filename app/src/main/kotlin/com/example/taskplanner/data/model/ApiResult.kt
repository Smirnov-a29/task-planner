package com.example.taskplanner.data.model

/** Результат обращения к серверу: успех, ошибка или состояние загрузки. */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}
