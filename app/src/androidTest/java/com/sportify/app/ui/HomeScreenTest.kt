package com.sportify.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.sportify.app.domain.model.SportType
import com.sportify.app.domain.model.Venue
import com.sportify.app.presentation.home.HomeScreen
import com.sportify.app.presentation.home.HomeState
import com.sportify.app.presentation.home.VenueItem
import com.sportify.app.presentation.theme.SportifyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val aceTennis = VenueItem(
        Venue(id = "v1", name = "Ace Tennis Club", city = "Bandung", address = "Jl. Dago 12",
            sports = listOf("TENNIS"), rating = 4.7),
        minPrice = 250_000.0, currency = "IDR"
    )

    private fun show(
        state: HomeState,
        onQuery: (String) -> Unit = {},
        onSport: (SportType?) -> Unit = {},
        onVenue: (String) -> Unit = {}
    ) = composeRule.setContent { SportifyTheme { HomeScreen(state, onQuery, onSport, onVenue) } }

    @Test
    fun showsVenueNameLocationAndPrice() {
        show(HomeState(venues = listOf(aceTennis), loading = false))

        composeRule.onNodeWithText("Ace Tennis Club").assertIsDisplayed()
        composeRule.onNodeWithText("Bandung · Jl. Dago 12", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("From", substring = true).assertIsDisplayed()
    }

    @Test
    fun showsAMessageWhenThereAreNoVenues() {
        show(HomeState(venues = emptyList(), loading = false))

        composeRule.onNodeWithText("No venues found").assertIsDisplayed()
    }

    @Test
    fun tappingAVenueReportsItsId() {
        var openedId: String? = null
        show(HomeState(venues = listOf(aceTennis), loading = false), onVenue = { openedId = it })

        composeRule.onNodeWithText("Ace Tennis Club").performClick()

        assertEquals("v1", openedId)
    }

    @Test
    fun typingInTheSearchBoxReportsTheQuery() {
        var query = ""
        show(HomeState(loading = false), onQuery = { query = it })

        // The label and the placeholder share the same text, so find the field by its action.
        composeRule.onNode(hasSetTextAction()).performTextInput("tennis")

        assertEquals("tennis", query)
    }

    @Test
    fun tappingASportChipReportsTheSport() {
        var picked: SportType? = null
        show(HomeState(venues = listOf(aceTennis), loading = false), onSport = { picked = it })

        composeRule.onNodeWithText("Padel").performClick()

        assertEquals(SportType.PADEL, picked)
    }
}
