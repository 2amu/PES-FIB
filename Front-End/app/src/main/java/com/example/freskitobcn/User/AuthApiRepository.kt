package com.example.freskitobcn.User

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AuthApiRepository {
    private val client = OkHttpClient()
    private val TAG = "AuthApi"

    suspend fun registerUser(
        username: String,
        email: String,
        password1: String,
        password2: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val url = "http://nattech.fib.upc.edu:40430/auth/registration/"

        val jsonBody = """
            {
                "username": "$username",
                "email": "$email",
                "password1": "$password1",
                "password2": "$password2"
            }
        """.trimIndent()

        val body = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "Codi de resposta: ${response.code}")
            Log.d(TAG, "Cos de resposta: $responseBody")

            if (response.isSuccessful) {
                return@withContext Result.success("Registre completat correctament")
            } else {
                // Intenta analitzar l'error JSON retornat
                val errorMessage = try {
                    val json = JSONObject(responseBody ?: "")
                    val errors = json.keys().asSequence().joinToString("\n") { key ->
                        val msgArray = json.getJSONArray(key)
                        val msg = msgArray.join(", ")
                        "$key: $msg"
                    }
                    errors
                } catch (e: Exception) {
                    "Error inesperat del servidor."
                }

                return@withContext Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error de connexió", e)
            return@withContext Result.failure(e)
        }
    }

    suspend fun loginUser(username: String?, email: String?, password: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        val url = "http://nattech.fib.upc.edu:40430/auth/login/"

        val jsonBody = if (!username.isNullOrEmpty()) {
            """
        {
            "username": "$username",
            "password": "$password"
        }
        """.trimIndent()
        } else if (!email.isNullOrEmpty()) {
            """
        {
            "email": "$email",
            "password": "$password"
        }
    """.trimIndent()
        } else {
            return@withContext Result.failure(Exception("S'ha de proporcionar almenys un dels camps: username o email"))
        }

        Log.d(TAG, "Cos de la petició: $jsonBody")

        val body = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "Codi de resposta login: ${response.code}")
            Log.d(TAG, "Cos de resposta login: $responseBody")

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody ?: "")
                val token = jsonResponse.optString("access")
                val userId = jsonResponse.getJSONObject("user").optInt("pk").toString()
                Log.e("DEBUG", "userId: $userId, token: $token")
                return@withContext Result.success(AuthResult(token, userId))
            } else {
                val errorMessage = try {
                    val json = JSONObject(responseBody ?: "")
                    val errors = json.keys().asSequence().joinToString("\n") { key ->
                        val msgArray = json.getJSONArray(key)
                        val msg = msgArray.join(", ")
                        "$key: $msg"
                    }
                    errors
                } catch (e: Exception) {
                    "Error inesperat del servidor durant login."
                }

                return@withContext Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error de connexió durant login", e)
            return@withContext Result.failure(e)
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        val url = "http://nattech.fib.upc.edu:40430/auth/google/"

        val jsonBody = if (!idToken.isNullOrEmpty()) {
            """
        {
            "id_token": "$idToken"
        }
        """.trimIndent()
        } else {
            return@withContext Result.failure(Exception("S'ha de proporcionar un usuari de google"))
        }

        Log.d(TAG, "Cos de la petició: $jsonBody")

        val body = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "Codi de resposta login: ${response.code}")
            Log.d(TAG, "Cos de resposta login: $responseBody")

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody ?: "")

                val userId = jsonResponse.getInt("user_id").toString()
                val token = jsonResponse.optString("access")
                Log.e("DEBUG", "userId: $userId, token: $token")
                return@withContext Result.success(AuthResult(token, userId))
            } else {
                val errorMessage = try {
                    val json = JSONObject(responseBody ?: "")
                    val errors = json.keys().asSequence().joinToString("\n") { key ->
                        val msgArray = json.getJSONArray(key)
                        val msg = msgArray.join(", ")
                        "$key: $msg"
                    }
                    errors
                } catch (e: Exception) {
                    "Error inesperat del servidor durant login."
                }

                return@withContext Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error de connexió durant login", e)
            return@withContext Result.failure(e)
        }
    }

    suspend fun logoutUser(token: String): Result<String> = withContext(Dispatchers.IO) {
        val url = "http://nattech.fib.upc.edu:40430/auth/logout/"

        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody("application/json".toMediaTypeOrNull())) // Body buit
            .addHeader("Authorization", "Bearer $token")
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "Codi de resposta logout: ${response.code}")
            Log.d(TAG, "Cos de resposta logout: $responseBody")

            if (response.isSuccessful) {
                return@withContext Result.success("Logout fet correctament")
            } else {
                val errorMessage = try {
                    val json = JSONObject(responseBody ?: "")
                    val errors = json.keys().asSequence().joinToString("\n") { key ->
                        val msgArray = json.getJSONArray(key)
                        val msg = msgArray.join(", ")
                        "$key: $msg"
                    }
                    errors
                } catch (e: Exception) {
                    "Error inesperat del servidor durant logout."
                }

                return@withContext Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error de connexió durant logout", e)
            return@withContext Result.failure(e)
        }
    }

    suspend fun requestPasswordReset(email: String): Result<String> = withContext(Dispatchers.IO) {
        val url = "http://nattech.fib.upc.edu:40430/auth/password/reset/"

        val jsonBody = """
        {
            "email": "$email"
        }
    """.trimIndent()

        val body = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "Codi de resposta reset: ${response.code}")
            Log.d(TAG, "Cos de resposta reset: $responseBody")

            if (response.isSuccessful) {
                return@withContext Result.success("S'ha enviat el correu de restabliment.")
            } else {
                val errorMessage = try {
                    val json = JSONObject(responseBody ?: "")
                    val errors = json.keys().asSequence().joinToString("\n") { key ->
                        val msgArray = json.getJSONArray(key)
                        val msg = msgArray.join(", ")
                        "$key: $msg"
                    }
                    errors
                } catch (e: Exception) {
                    "Error inesperat del servidor durant el restabliment de la contrasenya."
                }

                return@withContext Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error de connexió durant reset password", e)
            return@withContext Result.failure(e)
        }
    }
}