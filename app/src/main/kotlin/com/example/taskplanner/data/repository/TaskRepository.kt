package com.example.taskplanner.data.repository

import com.example.taskplanner.data.api.ApiService
import com.example.taskplanner.data.model.ApiResult
import com.example.taskplanner.data.model.Category
import com.example.taskplanner.data.model.CategoryRequest
import com.example.taskplanner.data.model.StatsResponse
import com.example.taskplanner.data.model.StatusRequest
import com.example.taskplanner.data.model.SuccessResponse
import com.example.taskplanner.data.model.Task
import com.example.taskplanner.data.model.TaskHistoryEntry
import com.example.taskplanner.data.model.TaskRequest

/** Доступ к задачам, категориям и статистике через REST API сервера. */
class TaskRepository(private val api: ApiService) {

    suspend fun getTasks(
        query: String? = null,
        category: String? = null,
        status: String? = null,
        priority: String? = null
    ): ApiResult<List<Task>> = safeApiCall {
        api.getTasks(query?.takeIf { it.isNotBlank() }, category, status, priority)
    }

    suspend fun getAllTasks(
        query: String? = null,
        category: String? = null,
        status: String? = null,
        priority: String? = null
    ): ApiResult<List<Task>> = safeApiCall {
        api.getAllTasks(query?.takeIf { it.isNotBlank() }, category, status, priority)
    }

    suspend fun getTask(id: String): ApiResult<Task> = safeApiCall { api.getTask(id) }

    suspend fun createTask(request: TaskRequest): ApiResult<Task> =
        safeApiCall { api.createTask(request) }

    suspend fun updateTask(id: String, request: TaskRequest): ApiResult<Task> =
        safeApiCall { api.updateTask(id, request) }

    suspend fun updateStatus(id: String, status: String): ApiResult<Task> =
        safeApiCall { api.updateStatus(id, StatusRequest(status)) }

    suspend fun deleteTask(id: String): ApiResult<SuccessResponse> =
        safeApiCall { api.deleteTask(id) }

    suspend fun getHistory(id: String): ApiResult<List<TaskHistoryEntry>> =
        safeApiCall { api.getHistory(id) }

    suspend fun getStats(): ApiResult<StatsResponse> = safeApiCall { api.getStats() }

    suspend fun getAdminStats(): ApiResult<StatsResponse> = safeApiCall { api.getAdminStats() }

    suspend fun getCategories(): ApiResult<List<Category>> = safeApiCall { api.getCategories() }

    suspend fun createCategory(name: String): ApiResult<Category> =
        safeApiCall { api.createCategory(CategoryRequest(name)) }

    suspend fun updateCategory(id: String, name: String): ApiResult<Category> =
        safeApiCall { api.updateCategory(id, CategoryRequest(name)) }

    suspend fun deleteCategory(id: String): ApiResult<SuccessResponse> =
        safeApiCall { api.deleteCategory(id) }
}
