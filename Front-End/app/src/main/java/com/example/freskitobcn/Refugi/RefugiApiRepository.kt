package com.example.freskitobcn.Refugi

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@kotlinx.serialization.Serializable
data class Tag(
    val id: Int,
    val name: String
)

@kotlinx.serialization.Serializable
data class TagVoteRequest(
    val tags: List<String>
)

@kotlinx.serialization.Serializable
data class RatingRequest(
    val comentario: String = ""
)

@kotlinx.serialization.Serializable
data class CommentRating(
    val id: Int,
    val user: String,
    val photo: String?,
    val puntuacion: Int,
    val fecha: String,
    val comentario: String
)

class RefugiApiRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
    private val client = OkHttpClient()
    private val tag = "RefugiApi"
    private val urlfall = "https://upload.wikimedia.org/wikipedia/commons/a/a3/Image-not-found.png"

    suspend fun getRefugisWithFavorites(token: String?, lat: Double, long: Double): List<Refugi> = withContext(Dispatchers.IO) {
        try {
            val url = "http://nattech.fib.upc.edu:40430/api/refugios/listar_cercania_usuario/$lat/$long/"
            Log.d(tag, "Iniciant la crida a l'API: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .apply {
                    if (token != null) {
                        addHeader("Authorization", "Bearer $token")
                    }
                }
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(tag, "Codi de resposta: ${response.code}")

                if (!response.isSuccessful) {
                    Log.e(tag, "Error en la resposta: ${response.code}")
                    return@withContext emptyList()
                }

                val responseBody = response.body?.string()
                if (responseBody == null) {
                    Log.e(tag, "El cos de la resposta és nul")
                    return@withContext emptyList()
                }

                try {
                    val refugiRespostes = json.decodeFromString<List<RefugiApiResponse>>(responseBody)
                    Log.d(tag, "S'han analitzat ${refugiRespostes.size} refugis")

                    return@withContext refugiRespostes.map { apiResponse ->
                        Refugi(
                            id = apiResponse.id,
                            name = apiResponse.nombre,
                            institution = apiResponse.institucion,
                            lat = apiResponse.latitud.toDouble(),
                            long = apiResponse.longitud.toDouble(),
                            distance = apiResponse.distancia,
                            hours = apiResponse.horario,
                            rating = apiResponse.valoracion,
                            imageUrl = apiResponse.imagen_local_url?.takeIf { it.isNotBlank() } ?: urlfall,
                            isFavorite = apiResponse.isFavorite,
                            tags = apiResponse.tags
                        )
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error analitzant JSON: ${e.message}")
                    e.printStackTrace()
                    return@withContext emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error obtenint refugis", e)
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun addRefugiToFavorites(token: String, refugiId: Int): Boolean = withContext(Dispatchers.IO) {
        val url = "http://nattech.fib.upc.edu:40430/api/usuarios/favorites/add/$refugiId/"
        val request = Request.Builder()
            .url(url)
            .post(okhttp3.RequestBody.create(null, ByteArray(0))) // Cuerpo vacío
            .addHeader("Authorization", "Bearer $token")
            .addHeader("accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(tag, "Error añadiendo a favoritos", e)
            return@withContext false
        }
    }

    suspend fun removeRefugiFromFavorites(token: String, refugiId: Int): Boolean = withContext(Dispatchers.IO) {
        val url = "http://nattech.fib.upc.edu:40430/api/usuarios/favorites/remove/$refugiId/"
        val request = Request.Builder()
            .url(url)
            .delete()
            .addHeader("Authorization", "Bearer $token")
            .addHeader("accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(tag, "Error eliminando de favoritos", e)
            return@withContext false
        }
    }

    suspend fun getAllTags(token: String): List<Tag> = withContext(Dispatchers.IO) {
        try {
            val url = "http://nattech.fib.upc.edu:40430/api/refugios/tags/"
            Log.d(tag, "Getting all tags: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(tag, "Tags response code: ${response.code}")

                if (!response.isSuccessful) {
                    Log.e(tag, "Error getting tags: ${response.code}")
                    return@withContext emptyList()
                }

                val responseBody = response.body?.string()
                if (responseBody == null) {
                    Log.e(tag, "Tags response body is null")
                    return@withContext emptyList()
                }

                try {
                    val tags = json.decodeFromString<List<Tag>>(responseBody)
                    Log.d(tag, "Parsed ${tags.size} tags")
                    return@withContext tags
                } catch (e: Exception) {
                    Log.e(tag, "Error parsing tags JSON: ${e.message}")
                    e.printStackTrace()
                    return@withContext emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting tags", e)
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun voteForTag(token: String, refugiId: Int, tagName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "http://nattech.fib.upc.edu:40430/api/refugios/$refugiId/tags/"
            Log.d(tag, "Voting for tag: $url, tag: $tagName")

            val requestBody = TagVoteRequest(tags = listOf(tagName))
            val jsonBody = json.encodeToString(TagVoteRequest.serializer(), requestBody)

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $token")
                .addHeader("accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(tag, "Vote tag response: ${response.code}")
                if (!response.isSuccessful) {
                    Log.e(tag, "Error voting for tag: ${response.code}, body: ${response.body?.string()}")
                }
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(tag, "Error voting for tag", e)
            return@withContext false
        }
    }

    suspend fun deleteVoteForTag(token: String, refugiId: Int, tagName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "http://nattech.fib.upc.edu:40430/api/refugios/$refugiId/tags/"
            Log.d(tag, "Deleting vote for tag: $url, tag: $tagName")

            val requestBody = TagVoteRequest(tags = listOf(tagName))
            val jsonBody = json.encodeToString(TagVoteRequest.serializer(), requestBody)

            val request = Request.Builder()
                .url(url)
                .delete(jsonBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $token")
                .addHeader("accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(tag, "Delete vote tag response: ${response.code}")
                if (!response.isSuccessful) {
                    Log.e(tag, "Error deleting vote for tag: ${response.code}, body: ${response.body?.string()}")
                }
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(tag, "Error deleting vote for tag", e)
            return@withContext false
        }
    }

    suspend fun getUserVotedTags(token: String, refugiId: Int): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "http://nattech.fib.upc.edu:40430/api/refugios/$refugiId/my-tags/"
            Log.d(tag, "Getting user voted tags: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(tag, "User voted tags response code: ${response.code}")

                if (!response.isSuccessful) {
                    Log.e(tag, "Error getting user voted tags: ${response.code}")
                    return@withContext emptyList()
                }

                val responseBody = response.body?.string()
                if (responseBody == null) {
                    Log.e(tag, "User voted tags response body is null")
                    return@withContext emptyList()
                }

                try {
                    // API returns a list of Tag objects with id and name, we need to extract just the names
                    val tagObjects = json.decodeFromString<List<Tag>>(responseBody)
                    val tagNames = tagObjects.map { it.name }
                    Log.d(tag, "User has voted for ${tagNames.size} tags: $tagNames")
                    return@withContext tagNames
                } catch (e: Exception) {
                    Log.e(tag, "Error parsing user voted tags JSON: ${e.message}")
                    e.printStackTrace()
                    return@withContext emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting user voted tags", e)
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
    suspend fun getNearbyEvents(
        latitude: Double,
        longitude: Double,
        range: Double = 1.0, // 1km by default
        categories: String? = null,
        dateStartRange: String? = null,
        dateEndRange: String? = null
    ): List<Event> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = "http://nattech.fib.upc.edu:40369/api/events/filter/"
            val urlBuilder = StringBuilder(baseUrl)

            val params = mutableListOf<String>()
            params.add("latitude=$latitude")
            params.add("longitude=$longitude")
            params.add("range=$range")

            if (categories != null) {
                params.add("categories=$categories")
            }
            if (dateStartRange != null) {
                params.add("date_start_range=$dateStartRange")
            }
            if (dateEndRange != null) {
                params.add("date_end_range=$dateEndRange")
            }

            if (params.isNotEmpty()) {
                urlBuilder.append("?")
                urlBuilder.append(params.joinToString("&"))
            }

            val url = urlBuilder.toString()
            Log.d(tag, "Getting nearby events: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .addHeader(
                    "X-CSRFTOKEN",
                    "ZwszaUXJAf45zogDYE8yxBtzTNQTVyxQeIq2pw3WmFNNGeAZteC4O6yt6rc7juaX"
                )
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(tag, "Events response code: ${response.code}")

                if (!response.isSuccessful) {
                    Log.e(tag, "Error getting events: ${response.code}")
                    return@withContext emptyList()
                }

                val responseBody = response.body?.string()
                if (responseBody == null) {
                    Log.e(tag, "Events response body is null")
                    return@withContext emptyList()
                }

                try {
                    val eventsResponse = json.decodeFromString<EventsResponse>(responseBody)
                    Log.d(tag, "Parsed ${eventsResponse.events.size} events")
                    return@withContext eventsResponse.events
                } catch (e: Exception) {
                    Log.e(tag, "Error parsing events JSON: ${e.message}")
                    e.printStackTrace()
                    return@withContext emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting events", e)
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
    suspend fun rateRefugiWithComment(token: String, refugiId: Int, rating: Double, comment: String = ""): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "http://nattech.fib.upc.edu:40430/api/refugios/$refugiId/valorar/$rating/"
            Log.d(tag, "Rating refugi with comment: $url, comment: $comment")

            val requestBody = RatingRequest(comentario = comment)
            val jsonBody = json.encodeToString(RatingRequest.serializer(), requestBody)

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $token")
                .addHeader("accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(tag, "Rate refugi with comment response: ${response.code}")
                if (!response.isSuccessful) {
                    Log.e(tag, "Error rating refugi with comment: ${response.code}, body: ${response.body?.string()}")
                }
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(tag, "Error rating refugi with comment", e)
            return@withContext false
        }
    }

    suspend fun getRefugiComments(token: String, refugiId: Int): List<CommentRating> = withContext(Dispatchers.IO) {
        try {
            val url = "http://nattech.fib.upc.edu:40430/api/refugios/$refugiId/valoraciones/"
            Log.d(tag, "Getting refugi comments: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("accept", "application/json")
                .addHeader("X-CSRFTOKEN", "7WEeIOEMvUv1gGmTTOp40yFp1zNZzutVm8CHXqKZhkeJnwGfooTAh3Kjed9dXq62")
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(tag, "Comments response code: ${response.code}")

                if (!response.isSuccessful) {
                    Log.e(tag, "Error getting comments: ${response.code}")
                    return@withContext emptyList()
                }

                val responseBody = response.body?.string()
                if (responseBody == null) {
                    Log.e(tag, "Comments response body is null")
                    return@withContext emptyList()
                }

                try {
                    val comments = json.decodeFromString<List<CommentRating>>(responseBody)
                    Log.d(tag, "Parsed ${comments.size} comments")
                    comments.forEach { comment ->
                        Log.d(tag, "Comment: ${comment.user} - ${comment.puntuacion}/5 - '${comment.comentario}' - ${comment.fecha}")
                    }
                    return@withContext comments
                } catch (e: Exception) {
                    Log.e(tag, "Error parsing comments JSON: ${e.message}")
                    e.printStackTrace()
                    return@withContext emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting comments", e)
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
}