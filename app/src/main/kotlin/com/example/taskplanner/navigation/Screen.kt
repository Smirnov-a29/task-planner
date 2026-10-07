package com.example.taskplanner.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object TaskList : Screen("task_list")
    object Admin : Screen("admin")
    object Profile : Screen("profile")
    object Stats : Screen("stats")

    // Создание и редактирование задачи: task_edit?taskId={taskId}
    object TaskEdit : Screen("task_edit?taskId={taskId}") {
        fun create(taskId: String? = null) =
            if (taskId == null) "task_edit" else "task_edit?taskId=$taskId"
    }
}
