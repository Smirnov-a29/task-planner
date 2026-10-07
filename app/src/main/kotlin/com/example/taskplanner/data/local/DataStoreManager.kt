package com.example.taskplanner.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "task_planner_prefs")

/** Локальное хранилище приложения: сессия пользователя, тема и история поиска. */
class DataStoreManager(private val context: Context) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_ROLE = stringPreferencesKey("user_role")
        private val KEY_NAME = stringPreferencesKey("user_name")
        private val KEY_EMAIL = stringPreferencesKey("user_email")
        private val KEY_DARK = booleanPreferencesKey("dark_theme")
        private val KEY_HISTORY = stringPreferencesKey("search_history")
        private const val HISTORY_LIMIT = 10
    }

    // ---- Данные сессии ----
    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val roleFlow: Flow<String?> = context.dataStore.data.map { it[KEY_ROLE] }
    val nameFlow: Flow<String?> = context.dataStore.data.map { it[KEY_NAME] }
    val emailFlow: Flow<String?> = context.dataStore.data.map { it[KEY_EMAIL] }

    // Синхронное чтение токена — используется в OkHttp Interceptor через runBlocking
    suspend fun getToken(): String? = context.dataStore.data.first()[KEY_TOKEN]

    suspend fun saveAuth(token: String, role: String, name: String, email: String) {
        context.dataStore.edit {
            it[KEY_TOKEN] = token
            it[KEY_ROLE] = role
            it[KEY_NAME] = name
            it[KEY_EMAIL] = email
        }
    }

    suspend fun clearAuthData() {
        context.dataStore.edit {
            it.remove(KEY_TOKEN)
            it.remove(KEY_ROLE)
            it.remove(KEY_NAME)
            it.remove(KEY_EMAIL)
        }
    }

    // ---- Тёмная тема ----
    val darkThemeFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK] ?: false }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK] = enabled }
    }

    // ---- История поиска (не более 10 элементов) ----
    val searchHistoryFlow: Flow<List<String>> = context.dataStore.data.map { prefs ->
        decode(prefs[KEY_HISTORY])
    }

    suspend fun addSearchQuery(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = decode(prefs[KEY_HISTORY]).toMutableList()
            current.remove(q)            // дубликат перемещаем, а не добавляем повторно
            current.add(0, q)            // новый запрос — в начало списка
            while (current.size > HISTORY_LIMIT) current.removeAt(current.size - 1)
            prefs[KEY_HISTORY] = Json.encodeToString(current)
        }
    }

    suspend fun clearSearchHistory() {
        context.dataStore.edit { it.remove(KEY_HISTORY) }
    }

    private fun decode(raw: String?): List<String> =
        if (raw.isNullOrBlank()) emptyList()
        else runCatching { Json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
}
