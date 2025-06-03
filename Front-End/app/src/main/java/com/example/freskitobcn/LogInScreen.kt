package com.example.freskitobcn

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import com.example.freskitobcn.User.AuthApiRepository
import com.example.freskitobcn.User.UserApiRepository
import com.example.freskitobcn.User.UserToken
import com.example.freskitobcn.CommonUtils
import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult




@Composable
fun LogInScreen(
    onNavigateToSignIn: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNavigateToPasswordReset: () -> Unit
) {
    var userInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authApiRepository = remember { AuthApiRepository() }
    val context = LocalContext.current
    val activity = context as Activity
    val coroutineScope = rememberCoroutineScope()
    val UserToken = remember { UserToken(context) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                Log.d("LOGIN_GOOGLE", "Token recibido de Google: $idToken")
                coroutineScope.launch {
                    UserToken.clearToken() // Limpia cualquier token viejo
                    val result = authApiRepository.loginWithGoogle(idToken)
                    result.onSuccess { authResult ->
                        UserToken.saveAuthToken(authResult.token)
                        UserToken.saveUserId(authResult.userId)
                        Toast.makeText(context, "Login Google OK", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    }.onFailure {
                        Toast.makeText(context, "Error en login con Google", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.e("LOGIN_GOOGLE", "idToken es null")
            }
        } catch (e: ApiException) {
            Log.e("LOGIN_GOOGLE", "Error en login con Google", e)
        }
    }

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("344007678371-6bfbcu57ls87urkrnppvgordvdh0sati.apps.googleusercontent.com")
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }


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
            modifier = Modifier
                .size(300.dp)
                .weight(1f, fill = true)
        )

        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            label = { Text(stringResource(R.string.mail_username), color = CommonUtils.DarkBlue) },
            modifier = Modifier
                .fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            textStyle = TextStyle(color = CommonUtils.DarkBlue),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White, // Borde actiu
                unfocusedBorderColor = CommonUtils.DarkBlue, // Borde inactiu
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White, // Borde actiu
                unfocusedBorderColor = CommonUtils.DarkBlue, // Borde inactiu
                cursorColor = CommonUtils.DarkBlue
            ),
            shape = RoundedCornerShape(150.dp),
                    trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(
                            R.string.show_password
                        ),
                        tint = if (passwordVisible) Color.White else CommonUtils.DarkBlue
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    val result = if (android.util.Patterns.EMAIL_ADDRESS.matcher(userInput).matches()) {
                        // És un email
                        authApiRepository.loginUser(username = null, email = userInput, password = password)
                    } else {
                        // És un username
                        authApiRepository.loginUser(username = userInput, email = null, password = password)
                    }

                    result.onSuccess { authResult ->
                        Log.d("LOGIN", "Token recibido del backend: ${authResult.token}token")
                        UserToken.saveAuthToken(authResult.token)
                        UserToken.saveUserId(authResult.userId)

                        val userApiRepository = UserApiRepository(context)
                        val userProfileResult = userApiRepository.getUserProfile(authResult.token)

                        userProfileResult.onSuccess { profile ->
                            Log.d("LOGIN", "Idioma de l'usuari: ${profile.idioma}")

                            val langCode = CommonUtils.idiomaToLangCode(profile.idioma)
                            val currentLang = context.resources.configuration.locales.get(0).language

                            if (langCode.isNotBlank() && langCode != currentLang) {
                                CommonUtils.setAppLocale(context, langCode)
                                CommonUtils.restartActivity(context)
                                onLoginSuccess()
                            } else {
                                onLoginSuccess()
                            }
                        }.onFailure {
                            Log.e("LOGIN", "Error obtenint perfil: ${it.message}")
                            onLoginSuccess()
                        }
                    }.onFailure { error ->
                        Toast.makeText(context, error.message ?: context.getString(R.string.authentication_error), Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CommonUtils.DarkBlue,
                contentColor = Color.White
            )
        ) {
            Text(text = stringResource(R.string.log_in))
        }

        Spacer(modifier = Modifier.height(8.dp))


        Button(
            onClick = {
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = CommonUtils.DarkBlue
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.google),
                    contentDescription = "Google Logo",
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                )
                Text(stringResource(R.string.login_google))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToSignIn) {
            Text(
                text = stringResource(R.string.sign_up),
                color = CommonUtils.DarkBlue,
                fontWeight = FontWeight.Bold
            )
        }

        TextButton(onClick = { onNavigateToPasswordReset() }) {
            Text(
                text = stringResource(R.string.forgot_password),
                color = Color.White,
                textDecoration = TextDecoration.Underline
            )
        }

    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LogInScreen(
        onNavigateToSignIn = {},
        onLoginSuccess = {},
        onNavigateToPasswordReset = {}
    )
}
