package com.example.freskitobcn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.freskitobcn.Location.rememberUserLocation
import com.example.freskitobcn.Location.RequestLocationPermission
import com.example.freskitobcn.ui.theme.FreskitobcnTheme
import com.example.freskitobcn.User.UserToken
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import android.util.Log
import com.example.freskitobcn.Refugi.PreviewRefugi
import com.example.freskitobcn.Refugi.Refugi
import com.example.freskitobcn.Refugi.RefugiApiRepository
import com.google.android.gms.maps.model.LatLng

@Composable
fun MainPage(
    onRefugiClick: (String) -> Unit = {}
) {
    RequestLocationPermission()

    val context = LocalContext.current
    val refugisState = remember { mutableStateOf<List<Refugi>>(emptyList()) }
    val showFavoritesOnly = remember { mutableStateOf(false) }
    val searchQuery = remember { mutableStateOf(TextFieldValue("")) }
    val isLoading = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { RefugiApiRepository() }
    val userPreferences = remember { UserToken(context) }
    val expanded = remember { mutableStateOf(false) }
    val userLocation = rememberUserLocation(context)

    LaunchedEffect(userLocation.value)
    {
        isLoading.value = true
        val token = userPreferences.tokenFlow.first()
        Log.d("User Token", token ?: "Token is null")
        val location = userLocation.value ?: LatLng(41.38895, 2.11319) // Ubicación predeterminada: Barcelona
        Log.d("User Location", location.toString())
        if (token != null) {
            refugisState.value = repository.getRefugisWithFavorites(
                token,
                location.latitude,
                location.longitude
            )
            Log.d("Refugis State", refugisState.value.toString())
        }
        isLoading.value = false
    }

    FreskitobcnTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CommonUtils.LightPink)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery.value,
                        onValueChange = { newValue ->
                            searchQuery.value = newValue
                        },
                        label = { Text(stringResource(R.string.search_refugis), color = Color.DarkGray) },
                        textStyle = TextStyle(color = Color.Black),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = CommonUtils.LightPink,
                            unfocusedContainerColor = CommonUtils.LightPink,
                            focusedIndicatorColor = Color.Gray,
                            unfocusedIndicatorColor = Color.DarkGray,
                            cursorColor = Color.DarkGray,
                            selectionColors = TextSelectionColors(
                                handleColor = Color.Gray,
                                backgroundColor = Color.LightGray
                            )
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        isLoading.value = true
                                        val token = userPreferences.tokenFlow.first()
                                        userLocation.value?.let { location ->
                                            if (token != null) {
                                                val allRefugis = repository.getRefugisWithFavorites(
                                                    token,
                                                    location.latitude,
                                                    location.longitude
                                                )
                                                refugisState.value = if (searchQuery.value.text.isEmpty()) {
                                                    allRefugis
                                                } else if (showFavoritesOnly.value) {
                                                    allRefugis.filter { refugi ->
                                                        refugi.isFavorite && refugi.name.contains(searchQuery.value.text, ignoreCase = true)
                                                    }
                                                } else {
                                                    allRefugis.filter { refugi ->
                                                        refugi.name.contains(searchQuery.value.text, ignoreCase = true)
                                                    }
                                                }
                                            }
                                        }
                                        isLoading.value = false
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search),
                                    tint = Color.Gray
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        IconButton(onClick = { expanded.value = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.filter),
                                tint = Color.Gray
                            )
                        }

                        DropdownMenu(
                            expanded = expanded.value,
                            onDismissRequest = { expanded.value = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.all)) },
                                onClick = {
                                    coroutineScope.launch {
                                        showFavoritesOnly.value = false
                                        isLoading.value = true
                                        val token = userPreferences.tokenFlow.first()
                                        userLocation.value?.let { location ->
                                            if (token != null) {
                                                val allRefugis = repository.getRefugisWithFavorites(
                                                    token,
                                                    location.latitude,
                                                    location.longitude
                                                )
                                                refugisState.value = if (searchQuery.value.text.isNotEmpty()) {
                                                    allRefugis.filter { refugi ->
                                                        refugi.name.contains(searchQuery.value.text, ignoreCase = true)
                                                    }
                                                } else {
                                                    allRefugis
                                                }
                                            }
                                        }
                                        isLoading.value = false
                                    }
                                    expanded.value = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.favorites)) },
                                onClick = {
                                    coroutineScope.launch {
                                        showFavoritesOnly.value = true
                                        isLoading.value = true
                                        val token = userPreferences.tokenFlow.first()
                                        userLocation.value?.let { location ->
                                            if (token != null) {
                                                val allRefugis = repository.getRefugisWithFavorites(
                                                    token,
                                                    location.latitude,
                                                    location.longitude
                                                )
                                                refugisState.value = if (searchQuery.value.text.isEmpty()) {
                                                    allRefugis.filter { refugi ->
                                                        refugi.isFavorite
                                                    }
                                                } else {
                                                    allRefugis.filter { refugi ->
                                                        refugi.isFavorite && refugi.name.contains(searchQuery.value.text, ignoreCase = true)
                                                    }
                                                }
                                            }
                                        }
                                        isLoading.value = false
                                    }
                                    expanded.value = false
                                }
                            )
                        }
                    }
                }

                if (isLoading.value) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.Gray)
                    }
                } else {
                    val refugisToShow = refugisState.value
                    if (refugisToShow.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.no_refugis_found), color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            items(refugisToShow) { refugi ->
                                refugi.imageUrl?.let {
                                    PreviewRefugi(
                                        name = refugi.name,
                                        institution = refugi.institution,
                                        distance = refugi.distance,
                                        rating = refugi.rating,
                                        imageUrl = it,
                                        isFavorite = refugi.isFavorite,
                                        onClick = { onRefugiClick(refugi.name) },
                                        onFavoriteClick = { willBeFavorite ->
                                            coroutineScope.launch {
                                                val token = userPreferences.tokenFlow.first()
                                                if (token != null) {
                                                    val success = if (willBeFavorite) {
                                                        repository.addRefugiToFavorites(token, refugi.id)
                                                    } else {
                                                        repository.removeRefugiFromFavorites(token, refugi.id)
                                                    }
                                                    if (success) {
                                                        // Actualiza el estado local para feedback inmediato
                                                        refugisState.value = refugisState.value.map {
                                                            if (it.id == refugi.id) it.copy(isFavorite = willBeFavorite) else it
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}