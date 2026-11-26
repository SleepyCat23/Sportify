package com.sportify.app.presentation.openplay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sportify.app.domain.model.*
import com.sportify.app.presentation.common.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenPlayRoute(vm: OpenPlayViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var creating by remember { mutableStateOf(false) }

    LaunchedEffect(ui.message) { ui.message?.let { snackbar.showSnackbar(it); vm.dismissMessage() } }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Play", fontWeight = FontWeight.Bold) }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            TabRow(selectedTabIndex = ui.tab) {
                Tab(ui.tab == 0, { vm.setTab(0) }, text = { Text("Open games") })
                Tab(ui.tab == 1, { vm.setTab(1) }, text = { Text("Matchmaking") })
            }
            if (ui.tab == 0) GamesTab(ui, vm, onCreate = { creating = true }) else MatchmakingTab(ui, vm)
        }
    }
    if (creating) CreateGameDialog(ui.venues, { creating = false }) { t, s, v, d, h, m ->
        vm.create(t, s, v, d, h, m); creating = false
    }
}

@Composable
private fun GamesTab(ui: OpenPlayUi, vm: OpenPlayViewModel, onCreate: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Join a game with other players", color = MaterialTheme.colorScheme.onSurfaceVariant)
                PrimaryButton("Create a game", onCreate, Modifier.fillMaxWidth())
            }
        }
        if (ui.games.isEmpty()) item { Text("No open games yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(ui.games, key = { it.id }) { g -> GameCard(g, ui.myUid, vm) }
    }
}

@Composable
private fun GameCard(g: OpenPlay, uid: String?, vm: OpenPlayViewModel) {
    val isHost = g.hostId == uid
    val isMember = uid in g.members
    val requested = uid in g.requests
    SportifyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(g.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TagChip(sportLabel(g.sport))
            }
            Text("${g.venueName} · ${g.date} · ${g.timeLabel}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${g.members.size}/${g.maxPlayers} players · host ${g.hostName}", style = MaterialTheme.typography.bodySmall)
            if (g.isFull) StatusBadge("Full", BadgeTone.Warning)

            if (isHost) {
                g.requests.forEach { r ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${g.names[r] ?: "Player"} wants to join", Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryButton("Approve", { vm.decide(g.id, r, true) }, height = 40.dp, enabled = !g.isFull)
                            OutlineButton("Decline", { vm.decide(g.id, r, false) }, height = 40.dp)
                        }
                    }
                }
                OutlineButton("Delete game", { vm.delete(g.id) }, Modifier.fillMaxWidth(), height = 44.dp)
            } else when {
                isMember -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge("Joined", BadgeTone.Success)
                    OutlineButton("Leave", { vm.leave(g.id) }, height = 40.dp)
                }
                requested -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge("Requested", BadgeTone.Warning)
                    OutlineButton("Withdraw", { vm.leave(g.id) }, height = 40.dp)
                }
                else -> PrimaryButton("Request to join", { vm.requestJoin(g.id) }, Modifier.fillMaxWidth(), height = 44.dp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateGameDialog(
    venues: List<Venue>, onDismiss: () -> Unit,
    onCreate: (String, SportType, Venue, String, Int, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var sport by remember { mutableStateOf(SportType.TENNIS) }
    var venue by remember { mutableStateOf(venues.firstOrNull()) }
    var date by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var hour by remember { mutableIntStateOf(18) }
    var max by remember { mutableIntStateOf(4) }

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Create a game") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                SectionLabel("Sport")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SportType.entries.forEach { OptionChip(it.label, it == sport, { sport = it }) }
                }
                SectionLabel("Venue")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    venues.forEach { OptionChip(it.name, it.id == venue?.id, { venue = it }) }
                }
                SectionLabel("Date")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1L..7L).map { LocalDate.now().plusDays(it) }) { d -> OptionChip("${d.dayOfMonth}/${d.monthValue}", d == date, { date = d }) }
                }
                SectionLabel("Start hour")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((7..21).toList()) { h -> OptionChip("%02d:00".format(h), h == hour, { hour = h }) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Players needed")
                    OutlinedButton({ if (max > 2) max-- }) { Text("−") }
                    Text("$max", fontWeight = FontWeight.Bold)
                    OutlinedButton({ if (max < 30) max++ }) { Text("+") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { venue?.let { onCreate(title, sport, it, date.toString(), hour, max) } },
                enabled = title.isNotBlank() && venue != null) { Text("Publish") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchmakingTab(ui: OpenPlayUi, vm: OpenPlayViewModel) {
    var editing by remember(ui.me) { mutableStateOf(ui.me == null) }
    var level by remember(ui.me) { mutableStateOf(runCatching { SkillLevel.valueOf(ui.me?.level.orEmpty()) }.getOrDefault(SkillLevel.INTERMEDIATE)) }
    var sport by remember(ui.me) { mutableStateOf(runCatching { SportType.valueOf(ui.me?.sport.orEmpty()) }.getOrDefault(SportType.TENNIS)) }
    val myInvites = ui.invites
    val received = myInvites.filter { it.toUid == ui.myUid && it.status == "PENDING" }
    val sentTo = myInvites.filter { it.fromUid == ui.myUid }.associateBy { it.toUid }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SportifyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editing) {
                        Text("Your level & sport", fontWeight = FontWeight.Bold)
                        Text("Set these once so other players can find you.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SkillLevel.entries.forEach { OptionChip(it.label, it == level, { level = it }) }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SportType.entries.forEach { OptionChip(it.label, it == sport, { sport = it }) }
                        }
                        PrimaryButton("Save", { vm.saveMe(level, sport) }, Modifier.fillMaxWidth(), height = 44.dp)
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { TagChip(level.label); TagChip(sport.label) }
                            LinkText("Edit", { editing = true })
                        }
                    }
                }
            }
        }
        if (received.isNotEmpty()) {
            item { SectionLabel("Invites for you") }
            items(received, key = { it.id }) { i ->
                SportifyCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${i.fromName} invites you to play ${sportLabel(i.sport)}", Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryButton("Accept", { vm.respond(i, true) }, height = 40.dp)
                            OutlineButton("Decline", { vm.respond(i, false) }, height = 40.dp)
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Find a partner")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { OptionChip("Any sport", ui.sportFilter == null, { vm.setSport(null) }) }
                    items(SportType.entries.toList()) { s -> OptionChip(s.label, ui.sportFilter == s, { vm.setSport(if (ui.sportFilter == s) null else s) }) }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { OptionChip("Any level", ui.levelFilter == null, { vm.setLevel(null) }) }
                    items(SkillLevel.entries.toList()) { l -> OptionChip(l.label, ui.levelFilter == l, { vm.setLevel(if (ui.levelFilter == l) null else l) }) }
                }
            }
        }
        if (ui.players.isEmpty()) item { Text("No players match these filters.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(ui.players, key = { it.uid }) { p ->
            val sent = sentTo[p.uid]
            SportifyCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(p.name, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TagChip(sportLabel(p.sport)); TagChip(runCatching { SkillLevel.valueOf(p.level).label }.getOrDefault(p.level))
                        }
                    }
                    when (sent?.status) {
                        "PENDING" -> StatusBadge("Invited", BadgeTone.Warning)
                        "ACCEPTED" -> StatusBadge("Accepted", BadgeTone.Success)
                        "DECLINED" -> StatusBadge("Declined", BadgeTone.Neutral)
                        else -> PrimaryButton("Invite", { vm.invite(p) }, height = 40.dp, enabled = ui.me != null)
                    }
                }
            }
        }
    }
}
