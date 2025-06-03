package com.example.freskitobcn

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.res.stringResource
import com.example.freskitobcn.User.AuthApiRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@Composable
fun SignInScreen(
    onBackToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var locationConsent by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CommonUtils.Pink)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(stringResource(R.string.sign_up), fontWeight = FontWeight.Bold, color = CommonUtils.DarkBlue)

        Spacer(modifier = Modifier.height(50.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.username), color = CommonUtils.DarkBlue) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = CommonUtils.DarkBlue),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CommonUtils.DarkBlue, // Borde actiu
                unfocusedBorderColor = Color.White, // Borde inactiu
                cursorColor = CommonUtils.DarkBlue
            ),
            shape = RoundedCornerShape(150.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email), color = CommonUtils.DarkBlue) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            textStyle = TextStyle(color = CommonUtils.DarkBlue),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CommonUtils.DarkBlue, // Borde actiu
                unfocusedBorderColor = Color.White, // Borde inactiu
                cursorColor = CommonUtils.DarkBlue
            ),
            shape = RoundedCornerShape(150.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password), color = CommonUtils.DarkBlue) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            textStyle = TextStyle(color = CommonUtils.DarkBlue),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password),
                        tint = if (passwordVisible) Color.White else CommonUtils.DarkBlue
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CommonUtils.DarkBlue, // Borde actiu
                unfocusedBorderColor = Color.White, // Borde inactiu
                cursorColor = CommonUtils.DarkBlue
            ),
            shape = RoundedCornerShape(150.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(stringResource(R.string.confirm_password), color = CommonUtils.DarkBlue) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            textStyle = TextStyle(color = CommonUtils.DarkBlue),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = if (confirmPasswordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password),
                        tint = if (confirmPasswordVisible) Color.White else CommonUtils.DarkBlue
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CommonUtils.DarkBlue, // Borde actiu
                unfocusedBorderColor = Color.White, // Borde inactiu
                cursorColor = CommonUtils.DarkBlue
            ),
            shape = RoundedCornerShape(150.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = locationConsent,
                onCheckedChange = { locationConsent = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = CommonUtils.DarkBlue,
                    uncheckedColor = CommonUtils.DarkBlue
                ),
            )
            Text(stringResource(R.string.location_consent), color = CommonUtils.DarkBlue)
        }

        Spacer(modifier = Modifier.height(16.dp))

        val authApiRepository = remember { AuthApiRepository() }

        Button(
            onClick = {
                errorMessage = when {
                    username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() ->
                        context.getString(R.string.fill_all_fields)

                    password != confirmPassword ->
                        context.getString(R.string.passwords_mismatch)

                    !locationConsent ->
                        context.getString(R.string.location_required)

                    else -> {
                        coroutineScope.launch {
                            val result = authApiRepository.registerUser(
                                username = username,
                                email = email,
                                password1 = password,
                                password2 = confirmPassword
                            )

                            result.onSuccess {
                                errorMessage = null
                                onBackToLogin()
                            }.onFailure {
                                errorMessage = it.message ?: context.getString(R.string.unknown_error)
                            }
                        }
                        null
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CommonUtils.DarkBlue,
                contentColor = Color.White
            )
        ) {
            Text(stringResource(R.string.sign_up))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.height(40.dp)) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = CommonUtils.DarkBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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

@Preview(showBackground = true)
@Composable
fun SignInScreenPreview() {
    SignInScreen(
        onBackToLogin = {}
    )
}