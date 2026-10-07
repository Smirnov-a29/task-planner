package com.example.taskplanner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

/** Фабрика, передающая в конструктор ViewModel контейнер зависимостей и SavedStateHandle. */
class AppViewModelFactory(
    private val container: AppContainer,
    private val creator: (AppContainer, SavedStateHandle) -> ViewModel
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val handle = extras.createSavedStateHandle()
        return creator(container, handle) as T
    }
}
