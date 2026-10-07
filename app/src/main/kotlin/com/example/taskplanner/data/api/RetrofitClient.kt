package com.example.taskplanner.data.api

import com.example.taskplanner.data.local.DataStoreManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.net.Proxy
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 10.0.2.2 — адрес хост-машины со стороны эмулятора Android
    const val BASE_URL = "http://10.0.2.2:8080/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun create(dataStore: DataStoreManager): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // JWT добавляется в каждый запрос. Токен читается синхронно через runBlocking.
        val authInterceptor = okhttp3.Interceptor { chain ->
            val token = runBlocking { dataStore.getToken() }
            val request = if (!token.isNullOrBlank()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            // Соединение с локальным сервером выполняется напрямую, минуя системный прокси,
            // который может быть настроен в эмуляторе или на устройстве
            .proxy(Proxy.NO_PROXY)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiService::class.java)
    }
}
