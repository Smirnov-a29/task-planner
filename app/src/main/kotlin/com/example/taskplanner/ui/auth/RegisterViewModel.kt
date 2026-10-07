package com.example.taskplanner.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskplanner.data.model.ApiResult
import com.example.taskplanner.data.model.Roles
import com.example.taskplanner.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val name: String = "",
    val login: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmVisible: Boolean = false,
    val role: String = Roles.USER,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false,
    // ошибки валидации под полями
    val nameError: String? = null,
    val loginError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null
)

class RegisterViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onNameChange(v: String) = _state.update { it.copy(name = v, nameError = null) }
    fun onLoginChange(v: String) = _state.update { it.copy(login = v, loginError = null) }
    fun onEmailChange(v: String) = _state.update { it.copy(email = v, emailError = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, passwordError = null) }
    fun onConfirmChange(v: String) = _state.update { it.copy(confirmPassword = v, confirmError = null) }
    fun onRoleChange(v: String) = _state.update { it.copy(role = v) }
    fun togglePassword() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun toggleConfirm() = _state.update { it.copy(confirmVisible = !it.confirmVisible) }
    fun consumeError() = _state.update { it.copy(errorMessage = null) }

    private fun validate(): Boolean {
        val s = _state.value
        val nameErr = if (s.name.isBlank()) "Введите имя" else null
        val loginErr = if (s.login.trim().length < 3) "Минимум 3 символа" else null
        val emailErr = if (!s.email.contains("@") || !s.email.contains(".")) "Некорректный email" else null
        val passErr = if (s.password.length < 6) "Минимум 6 символов" else null
        val confirmErr = if (s.confirmPassword != s.password) "Пароли не совпадают" else null

        _state.update {
            it.copy(
                nameError = nameErr,
                loginError = loginErr,
                emailError = emailErr,
                passwordError = passErr,
                confirmError = confirmErr
            )
        }
        return listOf(nameErr, loginErr, emailErr, passErr, confirmErr).all { it == null }
    }

    fun register() {
        if (!validate()) return
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val res = repo.register(
                name = s.name.trim(),
                login = s.login.trim(),
                email = s.email.trim(),
                password = s.password,
                role = s.role
            )
            when (res) {
                is ApiResult.Success ->
                    _state.update { it.copy(isLoading = false, success = true) }
                is ApiResult.Error ->
                    _state.update { it.copy(isLoading = false, errorMessage = res.message) }
                ApiResult.Loading -> {}
            }
        }
    }
}
