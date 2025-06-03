package com.example.freskitobcn.Mapa

import com.example.freskitobcn.Location.RequestLocationPermission
import com.example.freskitobcn.Location.rememberUserLocation
import com.example.freskitobcn.Location.hasLocationPermission

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.freskitobcn.Refugi.PreviewRefugi
import com.example.freskitobcn.Refugi.Refugi
import com.example.freskitobcn.Refugi.RefugiApiRepository
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.MapProperties
import androidx.compose.material3.FloatingActionButton
import com.example.freskitobcn.R
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.ui.Alignment
import com.google.android.gms.maps.CameraUpdateFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import com.example.freskitobcn.CommonUtils
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.ui.res.stringResource
import com.example.freskitobcn.User.UserToken


val TAG = "Mapa:"
@Composable
fun MapaScreen(
    selectedRefugiName: String = "",
    onRefugiClick: (String) -> Unit = {}
) {
    RequestLocationPermission()

    val context = LocalContext.current
    val markers = remember { mutableStateOf<List<MarkerData>>(emptyList()) }
    val refugisState = remember { mutableStateOf<List<Refugi>>(emptyList()) }

    var refugioSeleccionado by remember { mutableStateOf<Refugi?>(null) }
    val isRefugiSelected = refugioSeleccionado != null

    val coroutineScope = rememberCoroutineScope()
    val userPreferences = remember { UserToken(context) }
    val repository = remember { RefugiApiRepository() }
    val userLocation = rememberUserLocation(context)

    // Default camera position (Barcelona)
    val Barna = LatLng(41.38879, 2.15899)
    val defaultCamPos = CameraPosition.fromLatLngZoom(Barna, 12f)
    val cameraPositionState = rememberCameraPositionState {
        position = defaultCamPos
    }

    // Load refugis and set up markers
    LaunchedEffect(Unit) {
        val token = userPreferences.tokenFlow.first()

        val location = userLocation.value ?: LatLng(41.38895, 2.11319) // Ubicación predeterminada: FIB
        Log.d(TAG, "User Location: $location")

        if (token != null) {
            refugisState.value = repository.getRefugisWithFavorites(
                token,
                location.latitude,
                location.longitude
            )
        }

        Log.d(TAG, "S'afegeixen els markers")
        markers.value = refugisState.value.map { refugi ->
            MarkerData(refugi.name, LatLng(refugi.lat, refugi.long))
        }

        if (selectedRefugiName.isNotEmpty()) {
            val selectedRefugi = refugisState.value.find { it.name == selectedRefugiName }
            if (selectedRefugi != null) {
                refugioSeleccionado = selectedRefugi

                // Animate camera to the selected refugi
                coroutineScope.launch {
                    val position = LatLng(selectedRefugi.lat, selectedRefugi.long)
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(position, 15f),
                        durationMs = 1000
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMapView(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            markers = markers,
            userLocation = userLocation.value ?: LatLng(41.38895, 2.11319), // Ubicación predeterminada: FIB
            selectedMarkerTitle = refugioSeleccionado?.name,
            onMarkerClick = { markerTitle ->
                // Find refugi in original list using title
                refugioSeleccionado = refugisState.value.find { it.name == markerTitle }

                // If we find the refugi, animate camera to its position
                refugioSeleccionado?.let { refugi ->
                    coroutineScope.launch {
                        val position = LatLng(refugi.lat, refugi.long)
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(position, 15f),
                            durationMs = 500
                        )
                    }
                }
            },
            onMapClick = {
                refugioSeleccionado = null
            }
        )

        refugioSeleccionado?.let { refugi ->
            refugi.imageUrl?.let {
                PreviewRefugi(
                    name = refugi.name,
                    institution = refugi.institution,
                    distance = refugi.distance,
                    rating = refugi.rating,
                    imageUrl = it,
                    isFavorite = refugi.isFavorite,
                    onClick = {
                        onRefugiClick(refugi.name)
                    },
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
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }

        FloatingActionButton(
            onClick = {
                userLocation.value?.let { location ->
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(location, 15f),
                            durationMs = 1000
                        )
                    }
                }
            },
            containerColor = CommonUtils.DarkBlue,
            modifier = Modifier
                .align(if (isRefugiSelected) Alignment.TopStart else Alignment.BottomStart)
                .padding(
                    start = 16.dp,
                    top = if (isRefugiSelected) 16.dp else 0.dp,
                    bottom = if (isRefugiSelected) 0.dp else 16.dp
                )
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = stringResource(R.string.center_location),
                tint = Color.White
            )
        }
    }
}

@Composable
fun GoogleMapView(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    markers: MutableState<List<MarkerData>>,
    userLocation: LatLng?,
    selectedMarkerTitle: String? = null,
    onMarkerClick: (String) -> Unit,
    onMapClick: () -> Unit
) {
    val locationPermissionGranted = hasLocationPermission(LocalContext.current)
    LocalContext.current

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(locationPermissionGranted),
        uiSettings = MapUiSettings(locationPermissionGranted),
        onMapClick = {
            onMapClick()
        }
    ) {
        markers.value.forEach { marker ->
            val isSelected = marker.title == selectedMarkerTitle

            val markerIcon = if (isSelected) {
                // Use a custom marker for selected refugi
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
            } else {
                BitmapDescriptorFactory.defaultMarker()
            }

            Marker(
                state = MarkerState(position = marker.position),
                title = marker.title,
                icon = markerIcon,
                onClick = {
                    onMarkerClick(marker.title)
                    false
                }
            )
        }

        userLocation?.let {
            val customIcon = remember {
                BitmapDescriptorFactory.fromResource(R.drawable.marcador_ubi)
            }
            Marker(
                state = MarkerState(position = it),
                title = stringResource(R.string.your_location),
                icon = customIcon
            )
        }
    }
}

data class MarkerData(val title: String, val position: LatLng)