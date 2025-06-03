package com.example.freskitobcn.Refugi

data class Refugi(
    val id: Int,
    val name: String,
    val institution: String,
    val imageUrl: String?,
    val lat: Double,
    val long: Double,
    val distance: Int,
    val hours: String,
    val rating: Double,
    val isFavorite: Boolean = false, // Campo para marcar favoritos
    val tags: Map<String, Int> // Campo para almacenar etiquetas
)