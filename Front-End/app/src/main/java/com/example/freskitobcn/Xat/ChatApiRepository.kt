package com.example.freskitobcn.Xat

import android.content.Context
import com.example.freskitobcn.User.UserToken
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import com.example.freskitobcn.Xat.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class ChatApiRepository(private val context: Context) {

    private val client = OkHttpClient()

    suspend fun getLastMessages(token: String, refugiId: Int): List<ChatMessage> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("http://nattech.fib.upc.edu:40430/api/chat/$refugiId/")
            .addHeader("Authorization", "Bearer $token")
            .build()

        val response = client.newCall(request).execute()
        Log.d("CHATAPI:", "$response")
        if (!response.isSuccessful) return@withContext emptyList()

        val body = response.body?.string() ?: return@withContext emptyList()
        val jsonArray = JSONArray(body)
        Log.d("CHATAPI:", "$jsonArray")
        val result = mutableListOf<ChatMessage>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            result.add(
                ChatMessage(
                    mensaje = obj.getString("contenido"),
                    username = obj.getString("username"),
                    userid = obj.getString("user_id"),
                    timestamp = obj.getString("timestamp")
                )
            )
        }

        result
    }

}