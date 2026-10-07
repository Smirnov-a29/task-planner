package com.example.taskplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.taskplanner.navigation.NavGraph
import com.example.taskplanner.ui.theme.TaskPlannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as TaskPlannerApplication).container

        setContent {
            // Признак тёмной темы читается из DataStore при запуске приложения
            val darkTheme by container.dataStore.darkThemeFlow.collectAsState(initial = false)
            TaskPlannerTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavGraph(container = container)
                }
            }
        }
    }
}
