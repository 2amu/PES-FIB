package com.example.freskitobcn.Chat

import android.util.Log
import com.example.freskitobcn.Xat.ChatMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ChatWebSocketClient(
    private val refugiId: Int,
    private val token: String
) {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket sin timeout
        .build()

    private var webSocket: WebSocket? = null

    private val _incomingMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<ChatMessage> = _incomingMessages.asSharedFlow()

    private val _connectionStatus = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _connectionStatus.asStateFlow()

    fun connect() {
        val url = "ws://nattech.fib.upc.edu:40430/ws/chat/$refugiId/?token=$token"
        val request = Request.Builder().url(url).build()

        Log.d("ChatSocket", "Intentando conectar a: $url")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d("ChatSocket", "WebSocket conectado")
                _connectionStatus.value = true
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.d("ChatSocket", "Mensaje recibido: $text")
                try {
                    val json = JSONObject(text)
                    Log.d("weeb", "$json")
                    // Manejar errores del backend
                    if (json.has("error")) {
                        val errorMsg = json.getString("error")
                        Log.e("ChatSocket", "Error del servidor: $errorMsg")
                        return
                    }

                    // Procesar mensajes válidos
                    if (json.optString("type") == "chat_message") {
                        val message = ChatMessage(
                            mensaje = json.getString("mensaje"),
                            userid = json.getString("user_id"),
                            username = json.getString("username"),
                            timestamp = json.getString("timestamp")
                        )
                        _incomingMessages.tryEmit(message)
                    }

                } catch (e: Exception) {
                    Log.e("ChatSocket", "Error parseando mensaje", e)
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e("ChatSocket", "Error en WebSocket", t)
                _connectionStatus.value = false
                reconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d("ChatSocket", "WebSocket cerrado: $reason")
                _connectionStatus.value = false
            }
        })
    }

    fun sendMessage(text: String) {
        val json = JSONObject()
        json.put("mensaje", text)
        webSocket?.send(json.toString())
    }

    private fun reconnect() {
        connect()
    }

    fun disconnect() {
        webSocket?.close(1000, "Usuario cerró el chat")
    }
}
