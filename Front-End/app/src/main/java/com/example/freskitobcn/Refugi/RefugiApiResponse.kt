package com.example.freskitobcn.Refugi

import kotlinx.serialization.Serializable

@Serializable
data class RefugiApiResponse(
    val id: Int,
    val nombre: String,
    val latitud: String,
    val longitud: String,
    val direccion: String,
    val numero_calle: String?,
    val distrito: String,
    val vecindario: String,
    val codigo_postal: String,
    val institucion: String,
    val ultima_modificacion: String,
    val valoracion: Double,
    val imagen_local_url: String?,
    val distancia: Int,
    val horario: String,
    val isFavorite: Boolean,
    val tags: Map<String, Int>
)