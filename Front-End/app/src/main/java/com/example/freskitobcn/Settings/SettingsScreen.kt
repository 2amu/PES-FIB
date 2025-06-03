package com.example.freskitobcn

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.filled.Person

import androidx.compose.ui.graphics.Color

import androidx.compose.material.TopAppBar
import androidx.compose.ui.platform.LocalContext
import com.example.freskitobcn.User.UserToken
import androidx.compose.runtime.LaunchedEffect
import com.example.freskitobcn.User.UserApiRepository
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.foundation.clickable
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.InputStream
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialogDefaults.containerColor


import androidx.compose.ui.res.stringResource
import com.example.freskitobcn.User.AuthApiRepository

@Composable
fun SettingsScreen(onLogout: () -> Unit) {
    val context = LocalContext.current

    //Agafa token i id de l'usuari
    val userToken = remember { UserToken(context) }
    val token by userToken.tokenFlow.collectAsState(initial = null)
    val userId by userToken.userIdFlow.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    val originalName = remember {mutableStateOf("")}
    val originalLastN = remember {mutableStateOf("")}
    val originalUserN = remember {mutableStateOf("")}
    val originalIdioma = remember {mutableStateOf("")}

    //variables perfil
    val username = remember {mutableStateOf("not found")}
    val nombre = remember { mutableStateOf("not found") }
    val apellido = remember { mutableStateOf("not found") }
    val notificaciones = remember { mutableStateOf(true) }
    val idiomaSeleccionado = remember { mutableStateOf("") }
    var userphoto = remember { mutableStateOf("")}

    //crida api per obtenir valors de variables
    LaunchedEffect(userId, token) {
        if (userId != null && token != null) {
            Log.e("Settings", "obtenint perfil")
            val repository = UserApiRepository(context)
            val result = repository.getUserProfile(token!!)

            result.onSuccess { profile ->
                username.value = "@${profile.username}"
                nombre.value = profile.first_name
                apellido.value = profile.last_name
                idiomaSeleccionado.value = profile.idioma

                originalUserN.value = username.value
                originalName.value = nombre.value
                originalLastN.value = apellido.value
                originalIdioma.value = idiomaSeleccionado.value
            }.onFailure {
                Log.e("Settings", "Error obtenint perfil: ${it.message}")
            }

            val langCode = CommonUtils.idiomaToLangCode(idiomaSeleccionado.value)

            val photo = repository.getPhoto(token!!, userId!!)

            photo.onSuccess { photo ->
                userphoto.value = photo
            }.onFailure {
                Log.e("Settings", "Error obtenint foto: ${it.message}")
            }
        }
    }




    val contentResolver = context.contentResolver

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            inputStream?.let { stream ->
                // Decodifica y redimensiona la imagen a 512x512 máx
                val originalBitmap = BitmapFactory.decodeStream(stream)
                val resized = Bitmap.createScaledBitmap(originalBitmap, 512, 512, true)

                // Comprime la imagen a JPEG 80%
                val outputStream = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val byteArray = outputStream.toByteArray()

                // Codifica en Base64 sin saltos de línea
                val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                coroutineScope.launch {
                    val result = UserApiRepository(context).setPhoto(token!!, base64)
                    if (result.isSuccess) {
                        userphoto.value = result.getOrNull().toString()
                        Toast.makeText(context,
                            context.getString(R.string.photo_updated), Toast.LENGTH_SHORT).show()

                    }
                }
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                backgroundColor = CommonUtils.LightPink,
                contentColor = Color.Black,
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CommonUtils.LightPink)
            ) {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                        .fillMaxSize()
                        .background(CommonUtils.LightPink)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .align(Alignment.CenterHorizontally)
                            .background(CommonUtils.DarkBlue, CircleShape)
                            .clickable {
                                imagePickerLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userphoto.value.isNotBlank()) {
                            AsyncImage(
                                model = userphoto.value,
                                contentDescription = stringResource(R.string.profile_picture),
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(Color.LightGray, CircleShape)
                                    .clip(CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if (userphoto.value.isNotBlank()) {
                        Button(
                            onClick = {
                                // Acción para eliminar la imagen
                                coroutineScope.launch {
                                    val result = UserApiRepository(context).deletePhoto(token!!)
                                    if (result.isSuccess) {
                                        userphoto.value = ""
                                        Toast.makeText(context, "Foto eliminada", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error eliminant la foto", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(150.dp),
                            border = BorderStroke(2.dp, CommonUtils.DarkBlue),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.White,   // 🔁 En Material 2 se llama "backgroundColor"
                                contentColor = CommonUtils.DarkBlue
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(stringResource(R.string.delete_image))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Nombre
                    EditableSettingItem(label = stringResource(R.string.name), value = nombre.value) {
                        nombre.value = it
                    }

                    // Apellido
                    EditableSettingItem(label = stringResource(R.string.last_name), value = apellido.value) {
                        apellido.value = it
                    }

                    // Username
                    EditableSettingItem(label = stringResource(R.string.username), value = username.value) {
                        username.value = it
                    }

                    // Switch de notificaciones
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.notifications))
                        Switch(
                            checked = notificaciones.value,
                            onCheckedChange = { notificaciones.value = it }
                        )
                    }

                    // Selector de idioma
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.language))
                        DropdownMenuIdioma(
                            idiomaSeleccionado = idiomaSeleccionado.value,
                            onIdiomaSeleccionado = { idiomaSeleccionado.value = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val nombreCambiado = nombre.value != originalName.value
                            val apellidoCambiado = apellido.value != originalLastN.value
                            val usernameCambiado = username.value != originalUserN.value
                            val idiomaCambiado = idiomaSeleccionado.value != originalIdioma.value

                            if (nombreCambiado || apellidoCambiado || usernameCambiado || idiomaCambiado) {
                                coroutineScope.launch {
                                    val result = UserApiRepository(context).updateOwnProfile(
                                        token = token!!,
                                        firstName = if (nombreCambiado) nombre.value else null,
                                        lastName = if (apellidoCambiado) apellido.value else null,
                                        username = if (usernameCambiado) username.value.removePrefix("@") else null,
                                        idioma = if (idiomaCambiado) idiomaSeleccionado.value else null,
                                    )

                                    result.onSuccess {
                                        if (idiomaCambiado) {
                                            val langCode = CommonUtils.idiomaToLangCode(idiomaSeleccionado.value)
                                            originalIdioma.value = idiomaSeleccionado.value
                                            CommonUtils.setAppLocale(context, langCode)
                                            CommonUtils.restartActivity(context)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.profile_updated),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }.onFailure {
                                        Toast.makeText(
                                            context,
                                            "Error: ${it.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.save_changes))
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val result = AuthApiRepository().logoutUser(token = token!!)
                                Log.e("Settings", "$token")
                                result.onSuccess {
                                    onLogout()
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        "Error: ${it.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = CommonUtils.DarkBlue)
                    ) {
                        Text(
                            text = stringResource(R.string.log_out),
                            color = Color.White
                        )
                    }

                }
            }
        }
    )
}

@Composable
fun EditableSettingItem(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label)
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DropdownMenuIdioma(idiomaSeleccionado: String?, onIdiomaSeleccionado: (String) -> Unit) {
    val expanded = remember { mutableStateOf(false) }
    val idiomas = listOf("Castellano", "Català", "English")

    val idiomaMostrat = when {
        idiomaSeleccionado == null || idiomaSeleccionado == "" || idiomaSeleccionado == "null" -> stringResource(
            R.string.not_defined
        )
        else -> idiomaSeleccionado
    }
    Log.e("Settings", "$idiomaMostrat")

    Box {
        OutlinedButton(onClick = { expanded.value = true }) {
            Text(idiomaMostrat)
        }

        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            idiomas.forEach { idioma ->
                DropdownMenuItem(onClick = {
                    onIdiomaSeleccionado(idioma)
                    expanded.value = false
                }) {
                    Text(idioma)
                }
            }
        }
    }
}