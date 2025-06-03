package com.example.freskitobcn.User

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import android.util.Base64

class UserApiRepository(private val context: Context) {
    private val client = OkHttpClient()
    private val TAG = "UserApi"

    suspend fun getUserProfile(token: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        Log.d("PERFIL", "Token enviado al backend: $token")
        val url = "http://nattech.fib.upc.edu:40430/auth/user/"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $token")
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "Resposta USER: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                Log.d(TAG, "IDIOMA REBUT: ${json.optString("idioma")}")

                val idiomaFinal = json.optString("idioma").ifBlank { "English" }

                val profile = UserProfile(
                    username = json.optString("username"),
                    first_name = json.optString("first_name"),
                    last_name = json.optString("last_name"),
                    idioma = idiomaFinal
                )

                return@withContext Result.success(profile)
            } else {
                return@withContext Result.failure(Exception("No s'ha pogut obtenir l'usuari"))
            }
        } catch (e: Exception) {
            Log.e("Settings", "ERRORR exception")
            return@withContext Result.failure(e)
        }
    }

    suspend fun updateOwnProfile(
        token: String,
        firstName: String?,
        lastName: String?,
        username: String?,
        idioma: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val url = "http://nattech.fib.upc.edu:40430/auth/user/"

        val jsonBody = JSONObject().apply {
            if (!firstName.isNullOrBlank()) put("first_name", firstName)
            if (!lastName.isNullOrBlank()) put("last_name", lastName)
            if (!username.isNullOrBlank()) put("username", username)
            if (!idioma.isNullOrBlank()) put("idioma", idioma)
        }

        val body = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .patch(body)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d("UserApi", "Resposta PATCH /auth/user: $responseBody")

            return@withContext if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error actualitzant perfil"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun getPhoto(token: String, userId: String): Result<String> = withContext(Dispatchers.IO) {
        val url = "http://nattech.fib.upc.edu:40430/api/usuarios/$userId/photo"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $token")
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "Resposta USER: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)

                val photo = json.optString("photo")
                Log.e("Settings", "fotooooo $photo")

                return@withContext Result.success(photo)
            } else {
                return@withContext Result.failure(Exception("No s'ha pogut obtenir la foto de l'usuari"))
            }
        } catch (e: Exception) {
            Log.e("Settings", "ERROR exception")
            return@withContext Result.failure(e)
        }
    }

    suspend fun setPhoto(token: String, photo: String): Result<String> = withContext(Dispatchers.IO) {
        val url = "http://nattech.fib.upc.edu:40430/api/usuarios/photo/update/"

        val photoBytes = Base64.decode(photo, Base64.DEFAULT)
        val MEDIA_TYPE_JPG = "image/jpeg".toMediaTypeOrNull()

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("photo", "profile.jpg", photoBytes.toRequestBody(MEDIA_TYPE_JPG))
            .build()
        Log.d("UserApi", "Enviant foto base64: ${photo.take(100)}...")


        val request = Request.Builder()
            .url(url)
            .put(body)
            .addHeader("Authorization", "Bearer $token")
            .build()

        Log.d("UserApi", "Payload JSON: $")
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d("UserApi", "Resposta PUT: $responseBody")

            return@withContext if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                val updatedPhoto = json.optString("photo", "")
                Result.success(updatedPhoto)
            } else {
                Result.failure(Exception("Error actualitzant perfil"))
            }
        } catch (e: Exception) {
            Log.d("UserApi", "Resposta Puuuuuuuuuuuuuut $e")
            return@withContext Result.failure(e)
        }
    }

    suspend fun deletePhoto(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        val url = "http://nattech.fib.upc.edu:40430/api/usuarios/photo/delete/"

        val request = Request.Builder()
            .url(url)
            .delete()
            .addHeader("Authorization", "Bearer $token")
            .build()

        Log.d("UserApi", "Payload JSON: $")
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d("UserApi", "Resposta DELETE: $responseBody")
            Log.d("UserApi", "Content-Length: ${response.body?.contentLength()}")

            return@withContext if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error eliminant foto"))
            }
        } catch (e: Exception) {
            Log.e("UserApi", "error DELETE $e")
            return@withContext Result.failure(e)
        }
    }
}