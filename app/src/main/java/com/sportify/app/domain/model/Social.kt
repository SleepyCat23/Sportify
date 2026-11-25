package com.sportify.app.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

enum class SkillLevel(val label: String) {
    BEGINNER("Beginner"), INTERMEDIATE("Intermediate"), ADVANCED("Advanced")
}

/** UC: venue review. One review per user per venue (doc id = venueId_userId). */
data class Review(
    @DocumentId val id: String = "",
    val venueId: String = "", val venueName: String = "",
    val userId: String = "", val userName: String = "",
    val rating: Int = 5, val comment: String = "",
    val createdAt: Timestamp? = null
)

/** UC-08/09: public open-play game. [members] includes the host; [requests] are pending join requests. */
data class OpenPlay(
    @DocumentId val id: String = "",
    val hostId: String = "", val hostName: String = "",
    val title: String = "", val sport: String = "", val venueName: String = "",
    val date: String = "", val startHour: Int = 18, val maxPlayers: Int = 4,
    val members: List<String> = emptyList(),
    val requests: List<String> = emptyList(),
    val names: Map<String, String> = emptyMap(),   // uid -> display name
    val createdAt: Timestamp? = null
) {
    @get:Exclude
    val isFull: Boolean get() = members.size >= maxPlayers

    @get:Exclude
    val timeLabel: String get() = "%02d:00".format(startHour)
}

/** UC-10: public matchmaking profile (players/{uid}). */
data class Player(
    @DocumentId val uid: String = "",
    val name: String = "", val photoUrl: String = "",
    val level: String = SkillLevel.INTERMEDIATE.name,
    val sport: String = SportType.TENNIS.name
)

data class Invite(
    @DocumentId val id: String = "",
    val fromUid: String = "", val fromName: String = "",
    val toUid: String = "", val toName: String = "",
    val sport: String = "", val status: String = "PENDING", // PENDING | ACCEPTED | DECLINED
    val createdAt: Timestamp? = null
)

/** UC-11/12: a recorded match score. */
data class MatchResult(
    @DocumentId val id: String = "",
    val userId: String = "", val opponent: String = "", val sport: String = "",
    val myScore: Int = 0, val theirScore: Int = 0,
    val createdAt: Timestamp? = null
) {
    @get:Exclude
    val won: Boolean get() = myScore > theirScore
}

data class PlayStats(val matches: Int, val wins: Int) {
    val losses: Int get() = matches - wins
    val winRate: Int get() = if (matches == 0) 0 else wins * 100 / matches
}

fun List<MatchResult>.toStats() = PlayStats(size, count { it.won })
