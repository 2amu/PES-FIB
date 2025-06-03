package com.example.freskitobcn

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import com.example.freskitobcn.Weather.WeatherRepository

@Composable
fun Header(apiKey: String,
           navController: NavHostController,
              currentScreen: Screen
) {
    var feelsLikeTemperature by remember { mutableStateOf<Int?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val weatherData = WeatherRepository.fetchWeatherDataBarcelona(apiKey)
            feelsLikeTemperature = weatherData.feelsLikeC.toInt()
        } catch (e: Exception) {
            errorMessage = "Error al carregar la temperatura"
            feelsLikeTemperature = null
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        color = CommonUtils.Pink
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Izquierda: Sensación térmica + icono
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (feelsLikeTemperature != null) {
                    Text(
                        text = " ${feelsLikeTemperature}°C",
                        fontSize = 26.sp,
                        textAlign = TextAlign.Center,
                        color = CommonUtils.DarkBlue,
                        fontWeight = FontWeight.Bold,
                    )
                    if (feelsLikeTemperature!! >= 27) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val icon = when {
                            feelsLikeTemperature!! in 27..33 -> Icons.Filled.Whatshot
                            else -> Icons.Filled.Warning
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFFCC7A00),
                        )
                    }
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        color = CommonUtils.DarkBlue
                    )
                }
            }

            // Centro: Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
            )

            // Derecha: Menú
            IconButton(
                onClick = { navController.navigate(Screen.Settings.route) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Perfil",
                    modifier = Modifier.size(36.dp),
                    tint = if (currentScreen == Screen.Settings) Color.White else CommonUtils.DarkBlue
                )
            }
        }
    }
}

@Composable
fun Footer(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        color = CommonUtils.Pink
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onScreenSelected(Screen.Home) }) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Home Icon",
                    tint = if (currentScreen == Screen.Home) Color.White else CommonUtils.DarkBlue
                )
            }
            IconButton(onClick = { onScreenSelected(Screen.Xat) }) {
                Icon(
                    imageVector = Icons.Filled.MailOutline,
                    contentDescription = "Chat Icon",
                    tint = if (currentScreen == Screen.Xat) Color.White else CommonUtils.DarkBlue
                )
            }
            IconButton(onClick = { onScreenSelected(Screen.Map) }) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "Map Icon",
                    tint = if (currentScreen == Screen.Map) Color.White else CommonUtils.DarkBlue
                )
            }
            IconButton(onClick = { onScreenSelected(Screen.Ranking) }) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents, // Puedes elegir otro icono si prefieres
                    contentDescription = "Ranking Icon",
                    tint = if (currentScreen == Screen.Ranking) Color.White else CommonUtils.DarkBlue
                )
            }
        }
    }
}
