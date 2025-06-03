package com.example.freskitobcn.Refugi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import com.example.freskitobcn.CommonUtils.formatRating
import com.example.freskitobcn.R

fun formatDistance(distanceInMeters: Int): String {
    return if (distanceInMeters < 1000) {
        "${(distanceInMeters / 100) * 100}m" // Aproximar a la centena más cercana
    } else {
        String.format("%.1fKm", distanceInMeters / 1000.0) // Aproximar a un decimal
    }
}

@Composable
fun PreviewRefugi(
    name: String,
    institution: String,
    distance: Int,
    rating: Double,
    imageUrl: String,
    isFavorite: Boolean,
    onClick: () -> Unit = {}, // Manejo del clic en el refugio
    onFavoriteClick: (Boolean) -> Unit = {}, // Manejo del clic en el ícono de favorito
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }, // Llama al onClick cuando se hace clic en el refugio
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = stringResource(R.string.refugi_image),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(40.dp), // Espacio reservado para el icono animado
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = stringResource(R.string.favorite),
                        tint = animateColorAsState(
                            targetValue = if (isFavorite) Color.Red else Color.White,
                            animationSpec = tween(durationMillis = 500)
                        ).value,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onFavoriteClick(!isFavorite) }
                            )
                            .size(
                                animateFloatAsState(
                                    targetValue = if (isFavorite) 32.dp.value else 24.dp.value,
                                    animationSpec = tween(durationMillis = 500)
                                ).value.dp
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black // Changed from Color.LightGray to Color.Black for better visibility
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Institution (if available)
                if (institution.isNotEmpty()) {
                    Text(
                        text = institution,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Distance and Rating row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Distance
                    Text(
                        text = formatDistance(distance),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    // Rating with stars
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stars
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Estrella ${index + 1}",
                                tint = if (index < rating) Color(0xFFFFC107) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        // Rating text
                        Text(
                            text = " ${formatRating(rating)}",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}