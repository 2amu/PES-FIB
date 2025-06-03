package com.example.freskitobcn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.freskitobcn.Location.rememberUserLocation
import com.example.freskitobcn.ui.theme.FreskitobcnTheme
import com.example.freskitobcn.User.UserToken
import kotlinx.coroutines.flow.first
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.google.android.gms.maps.model.LatLng
import androidx.navigation.NavHostController
import com.example.freskitobcn.Refugi.Refugi
import com.example.freskitobcn.Refugi.RefugiApiRepository
import kotlinx.coroutines.delay
import kotlin.collections.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefugiRankingViewVisual(navController: NavHostController) {
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showTagDialog by remember { mutableStateOf(false) }
    val allTags = listOf(
        "Aigua Freskita",
        "Bon Ombra",
        "Aire Condicionat",
        "Bon Rotllo",
        "Animal Friendly",
        "Gratuït",
        "Family Friendly",
        "Bany Refrescant"
    )

    val refugisState = remember { mutableStateOf<List<Refugi>>(emptyList()) }
    val context = LocalContext.current
    val userLocation = rememberUserLocation(context)
    val userPreferences = remember { UserToken(context) }
    val repository = remember { RefugiApiRepository() }
    val isVisible = remember { mutableStateOf(false) }
    LaunchedEffect(selectedTag) {
        isVisible.value = false
        delay(100) // Pequeño retardo para reiniciar la animación
        if (selectedTag != null) isVisible.value = true
    }

    // Cargar refugios al iniciar o cuando cambia la localización
    LaunchedEffect(userLocation.value) {
        val token = userPreferences.tokenFlow.first()
        val location = userLocation.value ?: LatLng(41.38895, 2.11319)
        if (token != null) {
            refugisState.value = repository.getRefugisWithFavorites(
                token,
                location.latitude,
                location.longitude
            )
            Log.d("Refugis State", refugisState.value.toString())
        }
    }

    FreskitobcnTheme {
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
                Text(
                    text = stringResource(R.string.shelter_ranking),
                    style = MaterialTheme.typography.headlineLarge,
                    color = CommonUtils.DarkBlue,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .align(Alignment.CenterHorizontally),
                )

                if (selectedTag != null) {
                    val sortedRefugis = refugisState.value
                        .sortedByDescending { it.tags[selectedTag] ?: 0 }
                        .filter { (it.tags[selectedTag] ?: 0) > 0 }
                        .take(3)

                    if (sortedRefugis.isEmpty()) {
                        Text("No hay refugios con votos para la etiqueta \"$selectedTag\"")
                    } else {
                        // Primer puesto animado
                        if (sortedRefugis.isNotEmpty()) {
                            val first = sortedRefugis[0]
                            val animatedHeight by animateFloatAsState(
                                targetValue = if (isVisible.value) 200f else 0f,
                                animationSpec = tween(durationMillis = 500),
                                label = ""
                            )
                            AnimatedVisibility(
                                visible = isVisible.value,
                                enter = fadeIn(animationSpec = tween(durationMillis = 300)), // Duración de 300ms
                                exit = fadeOut(animationSpec = tween(durationMillis = 300))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(animatedHeight.dp)
                                        .background(
                                            Color(0xFFFFD700),
                                            shape = MaterialTheme.shapes.large
                                        )
                                        .padding(16.dp)
                                        .clickable {
                                            navController.navigate(
                                                Screen.MapWithRefugi.createRoute(
                                                    first.name
                                                )
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🥇", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
                                        Text(first.name, style = MaterialTheme.typography.titleLarge, color = Color.Black)
                                        if (first.institution.isNotBlank()) {
                                            Text(
                                                first.institution,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                        Text("${first.tags[selectedTag] ?: 0} ${stringResource(R.string.votes)}", style = MaterialTheme.typography.bodyLarge, color = Color.Black)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Segundo y tercer puesto en columna, uno debajo del otro
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Segundo puesto
                            if (sortedRefugis.size > 1) {
                                val second = sortedRefugis[1]
                                val animatedHeightSecond by animateFloatAsState(
                                    targetValue = if (isVisible.value) 150f else 0f,
                                    animationSpec = tween(durationMillis = 500),
                                    label = ""
                                )
                                AnimatedVisibility(
                                    visible = isVisible.value,
                                    enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                    exit = fadeOut(animationSpec = tween(durationMillis = 300))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(animatedHeightSecond.dp)
                                            .background(
                                                Color(0xFFC0C0C0),
                                                shape = MaterialTheme.shapes.medium
                                            )
                                            .padding(8.dp)
                                            .clickable {
                                                navController.navigate(
                                                    Screen.MapWithRefugi.createRoute(
                                                        second.name
                                                    )
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("🥈", style = MaterialTheme.typography.headlineSmall, color = Color.Black)
                                            Text(second.name, style = MaterialTheme.typography.titleMedium, color = Color.Black)
                                            if (second.institution.isNotBlank()) {
                                                Text(
                                                    second.institution,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray
                                                )
                                            }
                                            Text("${second.tags[selectedTag] ?: 0} ${stringResource(R.string.votes)}", style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Tercer puesto
                            if (sortedRefugis.size > 2) {
                                val third = sortedRefugis[2]
                                val animatedHeightThird by animateFloatAsState(
                                    targetValue = if (isVisible.value) 100f else 0f,
                                    animationSpec = tween(durationMillis = 500),
                                    label = ""
                                )
                                AnimatedVisibility(
                                    visible = isVisible.value,
                                    enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                    exit = fadeOut(animationSpec = tween(durationMillis = 300))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(animatedHeightThird.dp)
                                            .background(
                                                Color(0xFFCD7F32),
                                                shape = MaterialTheme.shapes.medium
                                            )
                                            .padding(8.dp)
                                            .clickable {
                                                navController.navigate(
                                                    Screen.MapWithRefugi.createRoute(
                                                        third.name
                                                    )
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("🥉", style = MaterialTheme.typography.titleLarge, color = Color.Black)
                                            Text(third.name, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                                            if (third.institution.isNotBlank()) {
                                                Text(
                                                    third.institution,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray
                                                )
                                            }
                                            Text("${third.tags[selectedTag] ?: 0} ${stringResource(R.string.votes)}", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { showTagDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.BottomCenter),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CommonUtils.DarkBlue
                )
            ) {
                Text(
                    selectedTag ?: stringResource(R.string.select_tag),
                    color = Color.White
                )
            }
        }


        if (showTagDialog) {
            AlertDialog(
                onDismissRequest = { showTagDialog = false },
                title = { Text(stringResource(R.string.select_tag), color = CommonUtils.DarkBlue) },
                text = {
                    Column(
                        modifier = Modifier.background(Color.White)
                    ) {
                        allTags.forEach { tag ->
                            ListItem(
                                headlineContent = { Text(tag, color = Color.Black) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .clickable {
                                        selectedTag = tag
                                        showTagDialog = false
                                    },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.White,
                                    headlineColor = Color.Black
                                )
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTagDialog = false }) {
                        Text(stringResource(R.string.close), color = CommonUtils.DarkBlue)
                    }
                },
                containerColor = Color.White // <- Fuerza el fondo blanco
            )
        }
    }
}