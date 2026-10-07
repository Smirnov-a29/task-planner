package com.example.taskplanner.data.api

import com.example.taskplanner.data.model.AuthResponse
import com.example.taskplanner.data.model.Category
import com.example.taskplanner.data.model.CategoryRequest
import com.example.taskplanner.data.model.LoginRequest
import com.example.taskplanner.data.model.RegisterRequest
import com.example.taskplanner.data.model.StatsResponse
import com.example.taskplanner.data.model.StatusRequest
import com.example.taskplanner.data.model.SuccessResponse
import com.example.taskplanner.data.model.Task
import com.example.taskplanner.data.model.TaskHistoryEntry
import com.example.taskplanner.data.model.TaskRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Описание REST API сервера «Планировщик задач». */
interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<SuccessResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("tasks")
    suspend fun getTasks(
        @Query("query") query: String? = null,
        @Query("category") category: String? = null,
        @Query("status") status: String? = null,
        @Query("priority") priority: String? = null
    ): Response<List<Task>>

    @GET("tasks/{id}")
    suspend fun getTask(@Path("id") id: String): Response<Task>

    @POST("tasks")
    suspend fun createTask(@Body request: TaskRequest): Response<Task>

    @PUT("tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body request: TaskRequest): Response<Task>

    @PATCH("tasks/{id}/status")
    suspend fun updateStatus(@Path("id") id: String, @Body request: StatusRequest): Response<Task>

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<SuccessResponse>

    @GET("tasks/{id}/history")
    suspend fun getHistory(@Path("id") id: String): Response<List<TaskHistoryEntry>>

    @GET("all-tasks")
    suspend fun getAllTasks(
        @Query("query") query: String? = null,
        @Query("category") category: String? = null,
        @Query("status") status: String? = null,
        @Query("priority") priority: String? = null
    ): Response<List<Task>>

    @GET("stats")
    suspend fun getStats(): Response<StatsResponse>

    @GET("admin/stats")
    suspend fun getAdminStats(): Response<StatsResponse>

    @GET("categories")
    suspend fun getCategories(): Response<List<Category>>

    @POST("categories")
    suspend fun createCategory(@Body request: CategoryRequest): Response<Category>

    @PUT("categories/{id}")
    suspend fun updateCategory(@Path("id") id: String, @Body request: CategoryRequest): Response<Category>

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<SuccessResponse>
}
