package com.sportify.app.presentation.venue

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sportify.app.domain.model.Review
import com.sportify.app.domain.repository.ReviewRepository
import com.sportify.app.presentation.common.*
import com.sportify.app.presentation.theme.Orange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewsUi(val reviews: List<Review> = emptyList(), val posting: Boolean = false, val error: String? = null)

/** Scoped to the venue/{venueId} back-stack entry, so it reads the same `venueId` argument. */
@HiltViewModel
class ReviewsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ReviewRepository
) : ViewModel() {
    private val venueId: String = checkNotNull(savedStateHandle["venueId"])
    private val local = MutableStateFlow(ReviewsUi())

    val ui: StateFlow<ReviewsUi> = combine(repo.observeReviews(venueId), local) { r, l -> l.copy(reviews = r) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReviewsUi())

    fun post(venueName: String, rating: Int, comment: String, onDone: () -> Unit) {
        viewModelScope.launch {
            local.update { it.copy(posting = true, error = null) }
            repo.addReview(venueId, venueName, rating, comment.trim())
                .onSuccess { local.update { it.copy(posting = false) }; onDone() }
                .onFailure { e -> local.update { it.copy(posting = false, error = e.message) } }
        }
    }
}

@Composable
fun ReviewsSection(venueName: String, vm: ReviewsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var rating by remember { mutableIntStateOf(5) }
    var text by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Reviews · ${ui.reviews.size}")
        ui.reviews.forEach { r ->
            SportifyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(r.userName, fontWeight = FontWeight.Bold)
                        Stars(r.rating)
                    }
                    if (r.comment.isNotBlank())
                        Text(r.comment, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        SportifyCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Write a review", fontWeight = FontWeight.Bold)
                Row {
                    (1..5).forEach { i ->
                        IconButton({ rating = i }) {
                            Icon(if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                "$i stars", tint = Orange)
                        }
                    }
                }
                OutlinedTextField(text, { text = it }, placeholder = { Text("How was your game?") },
                    modifier = Modifier.fillMaxWidth())
                ErrorText(ui.error)
                PrimaryButton("Post review", { vm.post(venueName, rating, text) { text = "" } },
                    Modifier.fillMaxWidth(), loading = ui.posting)
            }
        }
    }
}

@Composable
private fun Stars(n: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { i ->
            Icon(if (i < n) Icons.Default.Star else Icons.Default.StarBorder, null,
                Modifier.size(16.dp), tint = Orange)
        }
    }
}
