package com.example.freskitobcn.Weather

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Current(
    val temp_c: Float,
    val feelslike_c: Float
)

@Serializable
data class WeatherResponse(val current: Current)

data class WeatherData(val tempC: Float, val feelsLikeC: Float)

object WeatherRepository {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        engine {
            https {
                trustManager = object : javax.net.ssl.X509TrustManager {
                    override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? = arrayOf()
                }
            }
        }
    }

    suspend fun fetchWeatherDataBarcelona(apiKey: String): WeatherData {
        return try {
            val response: WeatherResponse = client.get("https://api.weatherapi.com/v1/current.json") {
                url {
                    parameters.append("key", apiKey)
                    parameters.append("q", "Barcelona")
                }
            }.body()

            WeatherData(
                tempC = response.current.temp_c,
                feelsLikeC = response.current.feelslike_c
            )
        } catch (e: Exception) {
            println("Error al fer la petició a WeatherAPI: ${e.message}")
            throw e
        }
    }
}

