package com.example.taskplanner.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskplanner.data.model.ApiResult
import com.example.taskplanner.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val login: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val role: String? = null,        // != null => успешный вход
    val loggedIn: Boolean = false
)

class LoginViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onLoginChange(v: String) = _state.update { it.copy(login = v, errorMessage = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, errorMessage = null) }
    fun togglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun consumeError() = _state.update { it.copy(errorMessage = null) }

    fun login() {
        val s = _state.value
        if (s.login.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(errorMessage = "Введите логин и пароль") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = repo.login(s.login.trim(), s.password)) {
                is ApiResult.Success ->
                    _state.update { it.copy(isLoading = false, role = res.data.role, loggedIn = true) }
                is ApiResult.Error ->
                    _state.update { it.copy(isLoading = false, errorMessage = res.message) }
                ApiResult.Loading -> {}
            }
        }
    }
}
