package com.example.freskitobcn.Xat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.freskitobcn.Refugi.Refugi
import com.example.freskitobcn.Refugi.RefugiApiRepository
import com.example.freskitobcn.User.UserToken
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.setValue
import com.example.freskitobcn.CommonUtils
import com.example.freskitobcn.ui.theme.FreskitobcnTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.freskitobcn.Location.rememberUserLocation
import com.google.android.gms.maps.model.LatLng
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.freskitobcn.Chat.ChatWebSocketClient
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import android.util.Log
import java.util.Locale
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.res.stringResource
import com.example.freskitobcn.R
import androidx.compose.material.icons.filled.Send

@Composable
fun MainXatScreen(
    onRefugiClick: (Int, String) -> Unit
) {
    val context = LocalContext.current
    val userToken = remember { UserToken(context) }
    val isLoading = remember { mutableStateOf(false) }
    var refugisXat by remember { mutableStateOf<List<Refugi>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { RefugiApiRepository() }
    val userLocation = rememberUserLocation(context) // Obtén la ubicación del usuari

    LaunchedEffect(userLocation.value) {
        isLoading.value = true
        val token = userToken.tokenFlow.first()
        val location = userLocation.value ?: LatLng(41.38895, 2.11319) // Ubicación predeterminada: Barcelona
        if (token != null) {
            refugisXat = repository.getRefugisWithFavorites(
                token,
                location.latitude,
                location.longitude // Pasa lat y long
            )
        }
        isLoading.value = false
    }

    FreskitobcnTheme {
        if (isLoading.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CommonUtils.LightPink),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.Gray)
            }
        }

        else if (refugisXat.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CommonUtils.LightPink),
                contentAlignment = Alignment.Center
            ) {
                Text("No se encontraron refugios", color = Color.Gray)
            }
        }

        else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CommonUtils.LightPink)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    itemsIndexed(refugisXat) { index, refugi ->
                        refugi.imageUrl?.let {
                            ChatRefugiPreview(
                                name = refugi.name,
                                institution = refugi.institution,
                                imageUrl = it,
                                isFavorite = refugi.isFavorite,
                                isEvenIndex = index % 2 == 0, // Zebra striping
                                onFavoriteClick = { willBeFavorite ->
                                    coroutineScope.launch {
                                        val token = userToken.tokenFlow.first()
                                        if (token != null) {
                                            val success = if (willBeFavorite) {
                                                repository.addRefugiToFavorites(token, refugi.id)
                                            } else {
                                                repository.removeRefugiFromFavorites(token, refugi.id)
                                            }
                                            if (success) {
                                                refugisXat = refugisXat.map {
                                                    if (it.id == refugi.id) it.copy(isFavorite = willBeFavorite) else it
                                                }
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    onRefugiClick(refugi.id, refugi.name)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun XatScreen(refugiId: Int, refugi: String, onBackClick: () -> Unit) {
    val context = LocalContext.current

    val scrollState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val userToken = remember { UserToken(context) }

    var token by remember { mutableStateOf<String?>(null) }
    var userId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        token = userToken.tokenFlow.first()
        userId = userToken.userIdFlow.first()
    }

    val chatApi = remember { ChatApiRepository(context) }
    val messages = remember { mutableStateListOf<ChatMessage>() }

    LaunchedEffect(token) {
        if (token != null) {
            val oldMessages = chatApi.getLastMessages(token!!, refugiId)
            messages.addAll(oldMessages.reversed())
        }
    }

    val chatClient = remember(token) {
        token?.let { ChatWebSocketClient(refugiId = refugiId, token = it) }
    }

    LaunchedEffect(chatClient) {
        chatClient?.connect()
        chatClient?.incomingMessages?.collect {
            messages.add(0, it) // prepend para reverseLayout
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            chatClient?.disconnect()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CommonUtils.LightPink)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = CommonUtils.DarkBlue,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = refugi,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                reverseLayout = true,
                state = scrollState
            ) {
                items(messages) { msg ->
                    val isCurrentUser = msg.userid == userId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    color = if (isCurrentUser) CommonUtils.Pink else Color.White,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            if (!isCurrentUser) {
                                Text(
                                    text = msg.username,
                                    fontWeight = FontWeight.Bold,
                                    color = CommonUtils.DarkBlue,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = msg.mensaje,
                                color = if (isCurrentUser) Color.White else Color.Black,
                                fontSize = 14.sp
                            )
                            Text(
                                text = formatTimestamp(msg.timestamp),
                                color = if (isCurrentUser) Color.LightGray else Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }


            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(3f),
                    placeholder = { Text(stringResource(R.string.write_message)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF3F3F3),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = CommonUtils.Pink,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    onClick = {
                        if (inputText.isNotBlank()) {
                            chatClient?.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CommonUtils.DarkBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = stringResource(R.string.send),
                        tint = Color.White
                    )
                }
            }
        }
    }
}


fun formatTimestamp(timestamp: String): String {
    return try {
        val cleanedTimestamp = timestamp.replace(" ", "T")
        val utcDateTime = OffsetDateTime.parse(cleanedTimestamp)
        val localDateTime = utcDateTime.atZoneSameInstant(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale("es"))
        localDateTime.format(formatter)
    } catch (e: Exception) {
        Log.e("Fecha", "Error parseando timestamp: $timestamp", e)
        "Fecha inválida"
    }
}
