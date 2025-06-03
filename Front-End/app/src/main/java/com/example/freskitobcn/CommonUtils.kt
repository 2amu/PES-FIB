package com.example.freskitobcn

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import java.util.Locale

object CommonUtils {
    val Pink = Color(0xffd59bb3)
    val LightPink = Color(0xFFe1c9c9)
    val DarkBlue = Color(0xFF141e78)

    fun idiomaToLangCode(idioma: String): String {
        return when (idioma) {
            "Català" -> "ca"
            "Castellano" -> "es"
            else -> "en"
        }
    }

    fun setAppLocale(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun restartActivity(context: Context) {
        val activity = context as? Activity
        activity?.recreate()
    }

    @SuppressLint("DefaultLocale")
    fun formatRating(rating: Double): String {
        return String.format("%.1f", rating)
    }
}