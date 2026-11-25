package com.sportify.app.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sportify.app.domain.model.*
import com.sportify.app.domain.repository.MatchRepository
import com.sportify.app.presentation.common.*
import com.sportify.app.presentation.theme.Green
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUi(val results: List<MatchResult> = emptyList(), val stats: PlayStats = PlayStats(0, 0), val message: String? = null)

@HiltViewModel
class StatsViewModel @Inject constructor(private val repo: MatchRepository) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)

    val ui: StateFlow<StatsUi> = combine(repo.observeMyResults(), message) { r, m -> StatsUi(r, r.toStats(), m) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUi())

    fun add(opponent: String, sport: SportType, mine: Int, theirs: Int) = viewModelScope.launch {
        repo.addResult(opponent.trim(), sport.name, mine, theirs)
            .onSuccess { message.value = "Score saved · stats updated" }
            .onFailure { message.value = it.message }
    }
}

/** UC-11 / UC-12: record a score and see history + win statistics. */
@Composable
fun StatsSection(vm: StatsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var opponent by remember { mutableStateOf("") }
    var sport by remember { mutableStateOf(SportType.TENNIS) }
    var mine by remember { mutableStateOf("") }
    var theirs by remember { mutableStateOf("") }
    val num = KeyboardOptions(keyboardType = KeyboardType.Number)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Matches" to "${ui.stats.matches}", "Wins" to "${ui.stats.wins}",
                "Win rate" to if (ui.stats.matches == 0) "–" else "${ui.stats.winRate}%").forEach { (k, v) ->
                SportifyCard(Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(k, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(v, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Green)
                    }
                }
            }
        }
        SportifyCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Record a match score", fontWeight = FontWeight.Bold)
                OutlinedTextField(opponent, { opponent = it }, label = { Text("Opponent") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SportType.entries.toList()) { s -> OptionChip(s.label, s == sport, { sport = s }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(mine, { mine = it.filter(Char::isDigit) }, label = { Text("Your score") }, singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                    OutlinedTextField(theirs, { theirs = it.filter(Char::isDigit) }, label = { Text("Their score") }, singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                }
                PrimaryButton("Save score", {
                    vm.add(opponent, sport, mine.toInt(), theirs.toInt()); opponent = ""; mine = ""; theirs = ""
                }, Modifier.fillMaxWidth(), enabled = opponent.isNotBlank() && mine.isNotEmpty() && theirs.isNotEmpty(), height = 44.dp)
                ui.message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            }
        }
        SectionLabel("History")
        if (ui.results.isEmpty()) Text("No matches recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        ui.results.forEach { r ->
            SportifyCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(if (r.won) "Win" else "Loss", if (r.won) BadgeTone.Success else BadgeTone.Neutral)
                        Text("vs ${r.opponent}")
                        TagChip(sportLabel(r.sport))
                    }
                    Text("${r.myScore} – ${r.theirScore}", fontWeight = FontWeight.Bold, color = Green)
                }
            }
        }
    }
}
