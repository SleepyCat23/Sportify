package com.sportify.app.domain.model

import org.junit.Assert.*
import org.junit.Test

class SocialModelTest {
    @Test fun openPlay_isFull_whenMembersReachMax() {
        assertFalse(OpenPlay(maxPlayers = 4, members = listOf("a", "b", "c")).isFull)
        assertTrue(OpenPlay(maxPlayers = 4, members = listOf("a", "b", "c", "d")).isFull)
    }

    @Test fun stats_countWinsAndWinRate() {
        val stats = listOf(
            MatchResult(myScore = 6, theirScore = 3), MatchResult(myScore = 2, theirScore = 6),
            MatchResult(myScore = 6, theirScore = 0), MatchResult(myScore = 6, theirScore = 4)
        ).toStats()
        assertEquals(4, stats.matches); assertEquals(3, stats.wins)
        assertEquals(1, stats.losses); assertEquals(75, stats.winRate)
    }

    @Test fun stats_emptyHistory_hasZeroWinRate() = assertEquals(0, emptyList<MatchResult>().toStats().winRate)
}
