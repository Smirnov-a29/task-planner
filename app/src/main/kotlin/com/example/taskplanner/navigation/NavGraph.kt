package com.example.taskplanner.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taskplanner.AppContainer
import com.example.taskplanner.AppViewModelFactory
import com.example.taskplanner.data.model.Roles
import com.example.taskplanner.ui.admin.AdminPanelScreen
import com.example.taskplanner.ui.admin.AdminViewModel
import com.example.taskplanner.ui.auth.LoginScreen
import com.example.taskplanner.ui.auth.LoginViewModel
import com.example.taskplanner.ui.auth.RegisterScreen
import com.example.taskplanner.ui.auth.RegisterViewModel
import com.example.taskplanner.ui.edit.TaskEditScreen
import com.example.taskplanner.ui.edit.TaskEditViewModel
import com.example.taskplanner.ui.profile.ProfileScreen
import com.example.taskplanner.ui.splash.SplashScreen
import com.example.taskplanner.ui.tasks.StatsScreen
import com.example.taskplanner.ui.tasks.StatsViewModel
import com.example.taskplanner.ui.tasks.TaskListScreen
import com.example.taskplanner.ui.tasks.TaskListViewModel

@Composable
fun NavGraph(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        // --- Экран заставки ---
        composable(Screen.Splash.route) {
            SplashScreen(
                dataStore = container.dataStore,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToUser = {
                    navController.navigate(Screen.TaskList.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.Admin.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // --- Вход ---
        composable(Screen.Login.route) {
            val vm: LoginViewModel = viewModel(
                factory = AppViewModelFactory(container) { c, _ -> LoginViewModel(c.authRepository) }
            )
            LoginScreen(
                viewModel = vm,
                onLoginSuccess = { role ->
                    val dest = if (role == Roles.ADMIN) Screen.Admin.route else Screen.TaskList.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        // --- Регистрация ---
        composable(Screen.Register.route) {
            val vm: RegisterViewModel = viewModel(
                factory = AppViewModelFactory(container) { c, _ -> RegisterViewModel(c.authRepository) }
            )
            RegisterScreen(
                viewModel = vm,
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // --- Список задач пользователя ---
        composable(Screen.TaskList.route) {
            val vm: TaskListViewModel = viewModel(
                factory = AppViewModelFactory(container) { c, handle ->
                    TaskListViewModel(c.taskRepository, c.dataStore, handle, allTasks = false)
                }
            )
            TaskListScreen(
                viewModel = vm,
                dataStore = container.dataStore,
                onOpenProfile = { navController.navigate(Screen.Profile.route) },
                onOpenStats = { navController.navigate(Screen.Stats.route) },
                onCreateTask = { navController.navigate(Screen.TaskEdit.create(null)) },
                onEditTask = { taskId -> navController.navigate(Screen.TaskEdit.create(taskId)) }
            )
        }

        // --- Панель администратора ---
        composable(Screen.Admin.route) {
            val listVm: TaskListViewModel = viewModel(
                key = "admin_tasks",
                factory = AppViewModelFactory(container) { c, handle ->
                    TaskListViewModel(c.taskRepository, c.dataStore, handle, allTasks = true)
                }
            )
            val adminVm: AdminViewModel = viewModel(
                key = "admin_panel",
                factory = AppViewModelFactory(container) { c, _ -> AdminViewModel(c.taskRepository) }
            )
            AdminPanelScreen(
                listViewModel = listVm,
                adminViewModel = adminVm,
                dataStore = container.dataStore,
                onOpenProfile = { navController.navigate(Screen.Profile.route) },
                onCreateTask = { navController.navigate(Screen.TaskEdit.create(null)) },
                onEditTask = { taskId -> navController.navigate(Screen.TaskEdit.create(taskId)) }
            )
        }

        // --- Создание и редактирование задачи ---
        composable(
            route = Screen.TaskEdit.route,
            arguments = listOf(navArgument("taskId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            val vm: TaskEditViewModel = viewModel(
                factory = AppViewModelFactory(container) { c, handle ->
                    TaskEditViewModel(c.taskRepository, handle)
                }
            )
            TaskEditScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // --- Профиль ---
        composable(Screen.Profile.route) {
            ProfileScreen(
                dataStore = container.dataStore,
                onBack = { navController.popBackStack() },
                onStats = { navController.navigate(Screen.Stats.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // --- Статистика по задачам пользователя ---
        composable(Screen.Stats.route) {
            val vm: StatsViewModel = viewModel(
                factory = AppViewModelFactory(container) { c, _ ->
                    StatsViewModel(c.taskRepository, admin = false)
                }
            )
            StatsScreen(viewModel = vm, onBack = { navController.popBackStack() })
        }
    }
}
