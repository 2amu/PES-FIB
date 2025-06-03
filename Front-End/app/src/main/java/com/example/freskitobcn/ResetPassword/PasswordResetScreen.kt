package com.example.freskitobcn.ResetPassword

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.freskitobcn.CommonUtils
import com.example.freskitobcn.R
import com.example.freskitobcn.User.AuthApiRepository
import kotlinx.coroutines.launch

@Composable
fun PasswordResetScreen(
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val authApi = remember { AuthApiRepository() }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CommonUtils.Pink)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.reset_password), style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            coroutineScope.launch {
                val result = authApi.requestPasswordReset(email)
                message = result.getOrNull() ?: result.exceptionOrNull()?.message
            }
            onBackToLogin()
        }) {
            Text(stringResource(R.string.send))
        }

        Spacer(modifier = Modifier.height(16.dp))

        message?.let {
            Text(text = it, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = onBackToLogin,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(
                text = stringResource(R.string.back),
                color = CommonUtils.DarkBlue,
                fontWeight = FontWeight.Bold
            )
        }
    }
}