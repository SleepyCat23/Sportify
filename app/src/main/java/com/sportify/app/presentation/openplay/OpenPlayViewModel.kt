package com.sportify.app.presentation.openplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportify.app.domain.model.*
import com.sportify.app.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class OpenPlayUi(
    val myUid: String? = null,
    val games: List<OpenPlay> = emptyList(),
    val venues: List<Venue> = emptyList(),
    val players: List<Player> = emptyList(),
    val me: Player? = null,
    val invites: List<Invite> = emptyList(),
    val tab: Int = 0,
    val sportFilter: SportType? = null,
    val levelFilter: SkillLevel? = null,
    val message: String? = null
)

private data class Remote(
    val games: List<OpenPlay>, val venues: List<Venue>, val players: List<Player>,
    val me: Player?, val invites: List<Invite>
)
private data class Local(
    val tab: Int = 0, val sport: SportType? = null, val level: SkillLevel? = null, val message: String? = null
)

@HiltViewModel
class OpenPlayViewModel @Inject constructor(
    private val plays: OpenPlayRepository,
    private val matches: MatchRepository,
    venues: VenueRepository,
    private val auth: AuthRepository
) : ViewModel() {
    private val local = MutableStateFlow(Local())

    private val remote = combine(
        plays.observeOpenPlays(), venues.observeVenues(), matches.observePlayers(),
        matches.observeMyPlayer(), matches.observeInvites()
    ) { g, v, p, me, i -> Remote(g, v, p, me, i) }

    val ui: StateFlow<OpenPlayUi> = combine(remote, local) { r, l ->
        val uid = auth.currentUid
        val today = LocalDate.now().toString()
        OpenPlayUi(
            myUid = uid,
            // UC-08 alt flow: a full game is closed to the public (host and members still see it)
            games = r.games.filter { it.date >= today && (!it.isFull || it.hostId == uid || uid in it.members) },
            venues = r.venues, me = r.me, invites = r.invites,
            players = r.players.filter {
                (l.sport == null || it.sport == l.sport.name) && (l.level == null || it.level == l.level.name)
            },
            tab = l.tab, sportFilter = l.sport, levelFilter = l.level, message = l.message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OpenPlayUi())

    fun setTab(i: Int) = local.update { it.copy(tab = i) }
    fun setSport(s: SportType?) = local.update { it.copy(sport = s) }
    fun setLevel(l: SkillLevel?) = local.update { it.copy(level = l) }
    fun dismissMessage() = local.update { it.copy(message = null) }

    private fun run(ok: String? = null, block: suspend () -> Result<Unit>) = viewModelScope.launch {
        block().onSuccess { ok?.let { m -> local.update { it.copy(message = m) } } }
            .onFailure { e -> local.update { it.copy(message = e.message ?: "Something went wrong") } }
    }

    fun create(title: String, sport: SportType, venue: Venue, date: String, hour: Int, max: Int) =
        run("Game published") { plays.create(title.trim(), sport.name, venue.name, date, hour, max) }
    fun requestJoin(id: String) = run("Request sent to the host") { plays.requestJoin(id) }
    fun leave(id: String) = run { plays.leave(id) }
    fun decide(id: String, uid: String, approve: Boolean) = run { plays.decide(id, uid, approve) }
    fun delete(id: String) = run { plays.delete(id) }

    fun saveMe(level: SkillLevel, sport: SportType) = run("Matchmaking profile saved") {
        matches.saveMyPlayer(level.name, sport.name)
    }
    fun invite(p: Player) = run("Invite sent") { matches.invite(p) }
    fun respond(i: Invite, accept: Boolean) = run(if (accept) "Accepted — say hi!" else null) {
        matches.respond(i, accept)
    }
}
