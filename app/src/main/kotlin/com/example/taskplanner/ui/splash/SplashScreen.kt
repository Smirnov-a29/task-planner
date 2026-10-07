package com.example.taskplanner.ui.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChecklistRtl
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskplanner.data.local.DataStoreManager
import com.example.taskplanner.data.model.Roles
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/** Экран заставки: проверяет сохранённую сессию и выбирает стартовый экран. */
@Composable
fun SplashScreen(
    dataStore: DataStoreManager,
    onNavigateToLogin: () -> Unit,
    onNavigateToUser: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1500)                                   // заставка показывается 1,5 секунды
        val token = dataStore.tokenFlow.first()
        val role = dataStore.roleFlow.first()
        when {
            token.isNullOrBlank() -> onNavigateToLogin()
            role == Roles.ADMIN -> onNavigateToAdmin()
            else -> onNavigateToUser()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ChecklistRtl,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Планировщик задач",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator()
        }
    }
}
