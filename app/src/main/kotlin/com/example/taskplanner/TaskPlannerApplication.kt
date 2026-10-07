package com.example.taskplanner

import android.app.Application
import com.example.taskplanner.data.api.ApiService
import com.example.taskplanner.data.api.RetrofitClient
import com.example.taskplanner.data.local.DataStoreManager
import com.example.taskplanner.data.repository.AuthRepository
import com.example.taskplanner.data.repository.TaskRepository

/** Простой ручной контейнер зависимостей — без Hilt и Koin, что проще для учебного проекта. */
class AppContainer(application: Application) {
    val dataStore: DataStoreManager = DataStoreManager(application)
    private val api: ApiService = RetrofitClient.create(dataStore)
    val authRepository: AuthRepository = AuthRepository(api, dataStore)
    val taskRepository: TaskRepository = TaskRepository(api)
}

class TaskPlannerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
