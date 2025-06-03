package com.example.freskitobcn.Refugi

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.freskitobcn.Location.rememberUserLocation
import com.example.freskitobcn.User.UserToken
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.runtime.remember
import com.example.freskitobcn.CommonUtils
import com.example.freskitobcn.CommonUtils.formatRating
import com.example.freskitobcn.R
import java.text.SimpleDateFormat
import java.util.*

// Event Data Classes
@kotlinx.serialization.Serializable
data class Event(
    val id: String,
    val name: String,
    val date_start: String,
    val date_end: String,
    val description: String,
    val price: String,
    val location: EventLocation,
    val categories: List<String>,
    val image: String?
)

@kotlinx.serialization.Serializable
data class EventLocation(
    val city: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

@kotlinx.serialization.Serializable
data class EventsResponse(
    val events: List<Event>
)

@Composable
fun RatingDialog(
    currentRating: Double,
    onRatingSelected: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    // Initialize slider to current rating, clamped between 1 and 5
    var sliderPosition by remember {
        mutableFloatStateOf(currentRating.toFloat().coerceIn(1f, 5f))
    }

    var comment by remember { mutableStateOf("") }

    AlertDialog(
                    onDismissRequest = onDismiss,
                    title = {
                        Text(
                            text = "Valorar refugi",
                            fontWeight = FontWeight.Bold,
                color = CommonUtils.DarkBlue
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Selecciona una puntuació de 1 a 5")

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Display current rating with stars
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < sliderPosition) Color(0xFFFFC107) else Color.LightGray,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Display selected rating as text
                Text(
                    text = sliderPosition.toInt().toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = CommonUtils.DarkBlue
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Slider for selecting rating from 1 to 5
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 1f..5f,
                    steps = 3, // 1, 2, 3, 4, 5 = 5 values, so 4 steps between min and max
                    colors = SliderDefaults.colors(
                        thumbColor = CommonUtils.DarkBlue,
                        activeTrackColor = CommonUtils.DarkBlue,
                        inactiveTrackColor = Color.LightGray
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Labels for min and max values
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("1", color = Color.Gray)
                    Text("5", color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.coment),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = CommonUtils.DarkBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = {
                        if (it.length <= 200) {
                            comment = it
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Escriu el teu comentari aquí...",
                            color = Color.Gray
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CommonUtils.DarkBlue,
                        focusedLabelColor = CommonUtils.DarkBlue,
                        cursorColor = CommonUtils.DarkBlue
                    ),
                    maxLines = 4,
                    singleLine = false
                )

                Text(
                    text = "${comment.length}/200",
                    fontSize = 12.sp,
                    color = if (comment.length > 180) Color.Red else Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.End
                )
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    onRatingSelected(sliderPosition.toInt(), comment.trim())
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = CommonUtils.DarkBlue,
                    contentColor = Color.White
                )
            ) {
                Text("Valorar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel·lar")
            }
        },
        containerColor = Color.White // <- Fuerza el fondo blanco
    )
}

@Composable
fun CommentsSection(
    comments: List<CommentRating>,
    modifier: Modifier = Modifier
) {
    if (comments.isNotEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.valuations),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(comments) { comment ->
                    CommentCard(comment = comment)
                }
            }

            if (comments.size > 3) {
                Text(
                    text = "Mostrant ${minOf(comments.size, 10)} comentaris",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CommentCard(
    comment: CommentRating,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (comment.photo != null && comment.photo.isNotBlank()) {
                    AsyncImage(
                        model = comment.photo,
                        contentDescription = "User Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default User",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = comment.user,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (index < comment.puntuacion) Color(0xFFFFC107) else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (comment.comentario.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = comment.comentario,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCommentDate(comment.fecha),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun formatCommentDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS+02:00", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString.substringBefore("T")
    } catch (e: Exception) {
        try {
            val fallbackFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+02:00", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = fallbackFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString.substringBefore("T")
        } catch (e2: Exception) {
            dateString.substringBefore("T")
        }
    }
}

@Composable
fun TagsSection(
    refugiTags: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    // Only show tags that have votes (count > 0)
    val tagsWithVotes = refugiTags.filter { it.value > 0 }

    if (tagsWithVotes.isNotEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.popular_tags),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tagsWithVotes.toList()) { (tagName, voteCount) ->
                    AssistChip(
                        onClick = { /* Non-clickable, just for display */ },
                        label = {
                            Text(
                                text = "$tagName ($voteCount)",
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = CommonUtils.DarkBlue,
                            labelColor = Color.White,
                            leadingIconContentColor = Color.White
                        ),
                        border = null,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EventsSection(
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    if (events.isNotEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.near_activities),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(events) { event ->
                    EventCard(
                        event = event,
                        onEventClick = onEventClick
                    )
                }
            }
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEventClick(event) },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Event name
            Text(
                text = event.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Date and location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = CommonUtils.DarkBlue
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatEventDate(event.date_start),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = CommonUtils.DarkBlue
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${event.location.address}, ${event.location.city}",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Categories
            if (event.categories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(event.categories) { category ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                                    fontSize = 12.sp
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = CommonUtils.DarkBlue.copy(alpha = 0.1f),
                                labelColor = CommonUtils.DarkBlue
                            ),
                            border = null,
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            // Price
            if (event.price.isNotEmpty() && event.price != "0.00") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Preu: ${event.price}€",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = CommonUtils.DarkBlue
                )
            }
        }
    }
}

private fun formatEventDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+00:00", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString.substringBefore("T")
    }
}

@Composable
fun EventDetailsDialog(
    event: Event,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = event.name,
                fontWeight = FontWeight.Bold,
                color = CommonUtils.DarkBlue,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Event image if available
                event.image?.let { imageUrl ->
                    if (imageUrl.isNotBlank()) {
                        val imageBuena = "http://nattech.fib.upc.edu:40369$imageUrl"
                        Log.e( "image" ,"Event image URL: $imageBuena")
                        AsyncImage(
                            model = imageBuena,
                            contentDescription = "Event Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = CommonUtils.DarkBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatEventDate(event.date_start),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Location
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = CommonUtils.DarkBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = event.location.address,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = event.location.city.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                            },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Price
                if (event.price.isNotEmpty() && event.price != "0.00") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Euro,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = CommonUtils.DarkBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${event.price}€",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = CommonUtils.DarkBlue
                        )
                    }
                }

                // Categories
                if (event.categories.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = CommonUtils.DarkBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(event.categories) { category ->
                                AssistChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = category.replaceFirstChar {
                                                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                                            },
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = CommonUtils.DarkBlue.copy(alpha = 0.1f),
                                        labelColor = CommonUtils.DarkBlue
                                    ),
                                    border = null,
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }
                }

                // Description
                Text(
                    text = "Descripció",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = event.description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color.DarkGray
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Tancar")
            }
        },
        dismissButton = null,
        containerColor = Color.White // <- Fuerza el fondo blanco
    )
}

@Composable
fun TagVotingDialog(
    allTags: List<Tag>,
    refugiTags: Map<String, Int>,
    userVotedTags: Set<String>, // Tags the user has already voted for
    onTagVote: (String) -> Unit,
    onTagDeleteVote: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.vote_tags),
                fontWeight = FontWeight.Bold,
                color = CommonUtils.DarkBlue
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_the_tags_to_vote_or_eliminate_from_your_votes),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = CommonUtils.DarkBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(allTags) { tag ->
                            val voteCount = refugiTags[tag.name] ?: 0
                            val hasUserVoted = userVotedTags.contains(tag.name)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = tag.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (voteCount > 0) {
                                        Text(
                                            text = "$voteCount ${stringResource(R.string.votes)}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                if (hasUserVoted) {
                                    // Show delete button if user has voted
                                    OutlinedButton(
                                        onClick = { onTagDeleteVote(tag.name) },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.Red,
                                            containerColor = Color.Transparent
                                        ),
                                        border = BorderStroke(1.dp, Color.Red),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.delete))
                                    }
                                } else {
                                    // Show vote button if user hasn't voted
                                    FilledTonalButton(
                                        onClick = { onTagVote(tag.name) },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = CommonUtils.DarkBlue,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ThumbUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.vote))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.close))
            }
        },
        dismissButton = null,
        containerColor = Color.White // <- Fuerza el fondo blanco
    )
}

@Composable
fun DetailsRefugi(
    refugiName: String,
    onBackClick: () -> Unit = {},
    navigateToMap: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val refugiState = remember { mutableStateOf<Refugi?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    val showRatingDialog = remember { mutableStateOf(false) }

    val ratingFeedback = remember { mutableStateOf<String?>(null) }
    val showRatingFeedback = remember { mutableStateOf(false) }

    val repository = remember { RefugiApiRepository() }
    val userToken = remember { UserToken(context) }

    val userLocation = rememberUserLocation(context)

    val allTags = remember { mutableStateOf<List<Tag>>(emptyList()) }
    val userVotedTags = remember { mutableStateOf<Set<String>>(emptySet()) } // Track user's votes from API
    val isLoadingTags = remember { mutableStateOf(false) }
    val tagsFeedback = remember { mutableStateOf<String?>(null) }
    val showTagsFeedback = remember { mutableStateOf(false) }
    val showTagVotingDialog = remember { mutableStateOf(false) }

    val nearbyEvents = remember { mutableStateOf<List<Event>>(emptyList()) }
    val isLoadingEvents = remember { mutableStateOf(false) }
    val showEventDetailsDialog = remember { mutableStateOf(false) }
    val selectedEvent = remember { mutableStateOf<Event?>(null) }

    val refugiComments = remember { mutableStateOf<List<CommentRating>>(emptyList()) }

    // Load the refugi details when the component is first composed
    LaunchedEffect(refugiName) {
        isLoading.value = true
        errorMessage.value = null

        try {
            val token = userToken.tokenFlow.first()
            val location = userLocation.value ?: LatLng(41.38895, 2.11319) // Ubicación predeterminada

            if (token != null) {
                // Load refugi details
                val refugis = repository.getRefugisWithFavorites(
                    token,
                    location.latitude,
                    location.longitude
                )

                val refugi = refugis.find { it.name == refugiName }

                if (refugi != null) {
                    refugiState.value = refugi

                    // Load all available tags and user's voted tags
                    isLoadingTags.value = true
                    val tags = repository.getAllTags(token)
                    allTags.value = tags

                    // Load user's voted tags for this refugi
                    val votedTagNames = repository.getUserVotedTags(token, refugi.id)
                    userVotedTags.value = votedTagNames.toSet()
                    Log.d("DetailsRefugi", "User voted tags loaded: ${votedTagNames}")

                    isLoadingTags.value = false
                } else {
                    errorMessage.value = "No s'ha pogut trobar informació per a $refugiName"
                }
            } else {
                errorMessage.value = "No s'ha pogut obtenir el token d'usuari"
            }
        } catch (e: Exception) {
            errorMessage.value = "Error: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }

    // Load nearby events and comments when refugi is loaded
    LaunchedEffect(refugiState.value) {
        refugiState.value?.let { refugi ->
            // Load comments
            coroutineScope.launch {
                val token = userToken.tokenFlow.first()
                if (token != null) {
                    try {
                        val comments = repository.getRefugiComments(token, refugi.id)
                        refugiComments.value = comments
                        Log.d("DetailsRefugi", "Loaded ${comments.size} comments for refugi ${refugi.name}")
                    } catch (e: Exception) {
                        Log.e("DetailsRefugi", "Error loading comments: ${e.message}")
                    }
                }
            }

            // Load nearby events
            isLoadingEvents.value = true
            try {
                val events = repository.getNearbyEvents(
                    latitude = refugi.lat,
                    longitude = refugi.long,
                    range = 1.0 // 1km radius
                )
                nearbyEvents.value = events
                Log.d("DetailsRefugi", "Loaded ${events.size} nearby events")
            } catch (e: Exception) {
                Log.e("DetailsRefugi", "Error loading events: ${e.message}")
            } finally {
                isLoadingEvents.value = false
            }
        }
    }

    // Show temporary feedback message for rating
    LaunchedEffect(showRatingFeedback) {
        if (showRatingFeedback.value) {
            kotlinx.coroutines.delay(2000) // Show for 2 seconds
            showRatingFeedback.value = false
            ratingFeedback.value = null
        }
    }

    // Show temporary feedback message for tags
    LaunchedEffect(showTagsFeedback) {
        if (showTagsFeedback.value) {
            kotlinx.coroutines.delay(2000) // Show for 2 seconds
            showTagsFeedback.value = false
            tagsFeedback.value = null
        }
    }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = CommonUtils.DarkBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = refugiName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )

                // Favorite button
                refugiState.value?.let { refugi ->
                    val context = LocalContext.current
                    val coroutineScope = rememberCoroutineScope()
                    val repository = remember { RefugiApiRepository() }
                    val userToken = remember { UserToken(context) }

                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (refugi.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = animateColorAsState(
                                targetValue = if (refugi.isFavorite) Color.Red else Color.White,
                                animationSpec = tween(durationMillis = 500)
                            ).value,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        coroutineScope.launch {
                                            val token = userToken.tokenFlow.first()
                                            if (token != null) {
                                                val willBeFavorite = !refugi.isFavorite
                                                val success = if (willBeFavorite) {
                                                    repository.addRefugiToFavorites(
                                                        token,
                                                        refugi.id
                                                    )
                                                } else {
                                                    repository.removeRefugiFromFavorites(
                                                        token,
                                                        refugi.id
                                                    )
                                                }
                                                if (success) {
                                                    refugiState.value =
                                                        refugi.copy(isFavorite = willBeFavorite)
                                                }
                                            }
                                        }
                                    }
                                )
                                .size(
                                    animateFloatAsState(
                                        targetValue = if (refugi.isFavorite) 32.dp.value else 24.dp.value,
                                        animationSpec = tween(durationMillis = 500)
                                    ).value.dp
                                )
                        )
                    }
                }
            }

            // Rest of the details content
            Text(
                text = stringResource(R.string.details_about, refugiName),
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            when {
                isLoading.value -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.Gray)
                    }
                }

                errorMessage.value != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = errorMessage.value ?: "",
                                color = Color.Red,
                                modifier = Modifier.padding(16.dp)
                            )

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isLoading.value = true
                                        errorMessage.value = null

                                        val result = runCatching {
                                            val token = userToken.tokenFlow.first()
                                            val location = userLocation.value ?: LatLng(41.38895, 2.11319)

                                            if (token != null) {
                                                val refugis = repository.getRefugisWithFavorites(
                                                    token,
                                                    location.latitude,
                                                    location.longitude
                                                )

                                                refugis.find { it.name == refugiName }
                                            } else {
                                                errorMessage.value = "No s'ha pogut obtenir el token d'usuari"
                                                null
                                            }
                                        }

                                        result.onSuccess { refugi ->
                                            if (refugi != null) {
                                                refugiState.value = refugi
                                            } else {
                                                errorMessage.value = "No s'ha pogut trobar informació per a $refugiName"
                                            }
                                        }.onFailure { e ->
                                            errorMessage.value = "Error: ${e.message}"
                                        }

                                        isLoading.value = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CommonUtils.DarkBlue
                                )
                            ) {
                                Text("Tornar a intentar")
                            }
                        }
                    }
                }

                refugiState.value != null -> {
                    val refugi = refugiState.value!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        // Image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        ) {
                            AsyncImage(
                                model = refugi.imageUrl,
                                contentDescription = "Refugi Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Rating with clickable stars and a rating button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            // Stars display
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                repeat(5) { index ->
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = if (index < refugi.rating) Color(0xFFFFC107) else Color.LightGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Text(
                                    text = " (${formatRating(refugi.rating)}/5)",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            // Button to rate
                            OutlinedButton(
                                onClick = { showRatingDialog.value = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White,
                                    containerColor = CommonUtils.DarkBlue
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.value),
                                    color = Color.White
                                )
                            }
                        }

                        // Show feedback message after rating
                        AnimatedVisibility(
                            visible = showRatingFeedback.value,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Surface(
                                color = if (ratingFeedback.value?.startsWith("Error") == true)
                                    Color(0xFFFFDDDD) else Color(0xFFDDFFDD),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = ratingFeedback.value ?: "",
                                    modifier = Modifier.padding(12.dp),
                                    color = if (ratingFeedback.value?.startsWith("Error") == true)
                                        Color.Red else Color(0xFF006400)
                                )
                            }
                        }

                        // Tags Section - Only show tags with votes
                        TagsSection(
                            refugiTags = refugi.tags,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Button to vote for tags
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OutlinedButton(
                                onClick = {
                                    // Refresh user voted tags before opening dialog
                                    coroutineScope.launch {
                                        val token = userToken.tokenFlow.first()
                                        if (token != null) {
                                            isLoadingTags.value = true
                                            val votedTagNames = repository.getUserVotedTags(token, refugi.id)
                                            userVotedTags.value = votedTagNames.toSet()
                                            Log.d("DetailsRefugi", "User voted tags refreshed before dialog: ${votedTagNames}")
                                            isLoadingTags.value = false
                                            showTagVotingDialog.value = true
                                        }
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CommonUtils.DarkBlue,
                                    containerColor = Color.Transparent
                                ),
                                border = BorderStroke(1.dp, CommonUtils.DarkBlue)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.vote_tags))
                            }
                        }

                        // Show feedback message after tag voting
                        AnimatedVisibility(
                            visible = showTagsFeedback.value,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Surface(
                                color = if (tagsFeedback.value?.startsWith("Error") == true)
                                    Color(0xFFFFDDDD) else Color(0xFFDDFFDD),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = tagsFeedback.value ?: "",
                                    modifier = Modifier.padding(12.dp),
                                    color = if (tagsFeedback.value?.startsWith("Error") == true)
                                        Color.Red else Color(0xFF006400)
                                )
                            }
                        }

                        if (isLoadingEvents.value) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = CommonUtils.DarkBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            EventsSection(
                                events = nearbyEvents.value,
                                onEventClick = { event ->
                                    selectedEvent.value = event
                                    showEventDetailsDialog.value = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (nearbyEvents.value.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Hours of operation
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = CommonUtils.DarkBlue,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = stringResource(R.string.timetable),
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )

                                Text(
                                    text = refugi.hours,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // Location - Navigate to internal map
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navigateToMap()
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = CommonUtils.DarkBlue,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.location),
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )

                                Text(
                                    text = stringResource(R.string.see_at_map),
                                    fontSize = 16.sp,
                                    color = CommonUtils.DarkBlue
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = CommonUtils.DarkBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                tint = CommonUtils.DarkBlue,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = stringResource(R.string.distance),
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )

                                Text(
                                    text = refugi.distance.toString() + "m",
                                    fontSize = 16.sp
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        CommentsSection(
                            comments = refugiComments.value,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (refugiComments.value.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        }

                        Text(
                            text = stringResource(R.string.more_information),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = stringResource(R.string.more_info_message),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Actions Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Map Button - Takes 2/3 of the width - Using your internal map
                            Button(
                                onClick = {
                                    // Navigate to the map tab
                                    navigateToMap()
                                },
                                modifier = Modifier
                                    .weight(2f)
                                    .padding(end = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CommonUtils.DarkBlue
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.see_at_map), color = Color.White)
                            }

                            Button(
                                onClick = {
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Visita ${refugi.name} a ${refugi.distance} de distància. Coordenades: ${refugi.lat}, ${refugi.long}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir refugi"))
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Gray
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Rating dialog - Updated to include comment functionality
                    if (showRatingDialog.value) {
                        RatingDialog(
                            currentRating = refugi.rating,
                            onRatingSelected = { newRating, comment ->
                                coroutineScope.launch {
                                    val token = userToken.tokenFlow.first()
                                    if (token != null) {
                                        // Use the new API method that includes comments
                                        val success = repository.rateRefugiWithComment(
                                            token,
                                            refugi.id,
                                            newRating.toDouble(),
                                            comment
                                        )

                                        if (success) {
                                            try {
                                                val updatedComments = repository.getRefugiComments(token, refugi.id)
                                                refugiComments.value = updatedComments
                                                Log.d("DetailsRefugi", "Reloaded ${updatedComments.size} comments after rating")
                                            } catch (e: Exception) {
                                                Log.e("DetailsRefugi", "Error reloading comments after rating: ${e.message}")
                                            }

                                            val successMessage = if (comment.isNotBlank()) {
                                                "Valoració i comentari enviats correctament!"
                                            } else {
                                                "Valoració enviada correctament!"
                                            }
                                            ratingFeedback.value = successMessage
                                            showRatingFeedback.value = true
                                        } else {
                                            // Show error message
                                            ratingFeedback.value = "Error: No s'ha pogut valorar el refugi"
                                            showRatingFeedback.value = true
                                        }
                                        showRatingDialog.value = false
                                    }
                                }
                            },
                            onDismiss = { showRatingDialog.value = false }
                        )
                    }

                    // Tag voting dialog
                    if (showTagVotingDialog.value) {
                        TagVotingDialog(
                            allTags = allTags.value,
                            refugiTags = refugi.tags,
                            userVotedTags = userVotedTags.value,
                            onTagVote = { tagName ->
                                coroutineScope.launch {
                                    val token = userToken.tokenFlow.first()
                                    if (token != null) {
                                        Log.d("DetailsRefugi", "Voting for tag: $tagName")
                                        val success = repository.voteForTag(token, refugi.id, tagName)
                                        if (success) {
                                            // Refresh both refugi data and user voted tags from server
                                            val location = userLocation.value ?: LatLng(41.38895, 2.11319)
                                            val refugis = repository.getRefugisWithFavorites(
                                                token,
                                                location.latitude,
                                                location.longitude
                                            )
                                            val updatedRefugi = refugis.find { it.name == refugiName }
                                            if (updatedRefugi != null) {
                                                refugiState.value = updatedRefugi
                                            }

                                            // Refresh user voted tags from API
                                            val votedTagNames = repository.getUserVotedTags(token, refugi.id)
                                            userVotedTags.value = votedTagNames.toSet()
                                            Log.d("DetailsRefugi", "After voting, user voted tags: ${votedTagNames}")

                                            // Show success message
                                            tagsFeedback.value = "Vot per '$tagName' enviat correctament!"
                                            showTagsFeedback.value = true
                                        } else {
                                            // Show error message
                                            tagsFeedback.value = "Error: No s'ha pogut votar l'etiqueta"
                                            showTagsFeedback.value = true
                                        }
                                    }
                                }
                            },
                            onTagDeleteVote = { tagName ->
                                coroutineScope.launch {
                                    val token = userToken.tokenFlow.first()
                                    if (token != null) {
                                        Log.d("DetailsRefugi", "Deleting vote for tag: $tagName")
                                        val success = repository.deleteVoteForTag(token, refugi.id, tagName)
                                        if (success) {
                                            // Refresh both refugi data and user voted tags from server
                                            val location = userLocation.value ?: LatLng(41.38895, 2.11319)
                                            val refugis = repository.getRefugisWithFavorites(
                                                token,
                                                location.latitude,
                                                location.longitude
                                            )
                                            val updatedRefugi = refugis.find { it.name == refugiName }
                                            if (updatedRefugi != null) {
                                                refugiState.value = updatedRefugi
                                            }

                                            // Refresh user voted tags from API
                                            val votedTagNames = repository.getUserVotedTags(token, refugi.id)
                                            userVotedTags.value = votedTagNames.toSet()
                                            Log.d("DetailsRefugi", "After deleting vote, user voted tags: ${votedTagNames}")

                                            // Show success message
                                            tagsFeedback.value = "Vot per '$tagName' eliminat correctament!"
                                            showTagsFeedback.value = true
                                        } else {
                                            // Show error message
                                            tagsFeedback.value = "Error: No s'ha pogut eliminar el vot"
                                            showTagsFeedback.value = true
                                        }
                                    }
                                }
                            },
                            onDismiss = { showTagVotingDialog.value = false },
                            isLoading = isLoadingTags.value
                        )
                    }

                    if (showEventDetailsDialog.value && selectedEvent.value != null) {
                        EventDetailsDialog(
                            event = selectedEvent.value!!,
                            onDismiss = {
                                showEventDetailsDialog.value = false
                                selectedEvent.value = null
                            }
                        )
                    }
                }
            }
        }
    }
}