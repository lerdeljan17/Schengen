package com.schengen.tracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.schengen.tracker.ui.components.YearCalendarView
import com.schengen.tracker.ui.screens.home.AvailableDaysHero
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayoutRegressionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun allWeeksOfAutumnMonthsCanBeScrolledIntoView() {
        assertWholeMonthsVisible(startWeekOnSunday = false)
    }

    @Test
    fun allWeeksRemainVisibleWithSundayWeekStart() {
        assertWholeMonthsVisible(startWeekOnSunday = true)
    }

    private fun assertWholeMonthsVisible(startWeekOnSunday: Boolean) {
        compose.setContent {
            MaterialTheme {
                YearCalendarView(
                    months = (0L..11L).map { YearMonth.of(2026, 3).plusMonths(it) },
                    trips = emptyList(),
                    today = LocalDate.of(2026, 9, 19),
                    startWeekOnSunday = startWeekOnSunday,
                    onDayClick = {},
                    onDayLongPress = {},
                    onMonthClick = {},
                    contentPadding = PaddingValues(12.dp),
                    initialPageIndex = 0,
                    modifier = Modifier.testTag("calendar")
                )
            }
        }
        listOf("2026-09", "2026-10", "2026-11", "2027-02").forEach { month ->
            val tag = "month-$month"
            compose.onNodeWithTag(tag).performScrollTo()
            val lastDay = YearMonth.parse(month).lengthOfMonth().toString()
            compose.onNode(
                hasText(lastDay) and hasAnyAncestor(hasTestTag(tag)),
                useUnmergedTree = true
            ).assertIsDisplayed()
            compose.onNode(
                hasText("1") and hasAnyAncestor(hasTestTag(tag)),
                useUnmergedTree = true
            ).assertIsDisplayed()
        }
    }

    @Test
    fun plannedTripsAndUsedCardsHaveEqualDimensionsWhenLabelWraps() {
        assertMatchingStatSizes(planned = true)
    }

    @Test
    fun windowAndUsedCardsHaveEqualDimensions() {
        assertMatchingStatSizes(planned = false)
    }

    private fun assertMatchingStatSizes(planned: Boolean) {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(240.dp)) {
                    AvailableDaysHero(
                        availableDays = 60,
                        afterPlannedDays = if (planned) 40 else null,
                        afterPlannedDate = if (planned) LocalDate.of(2026, 11, 30) else null,
                        usedDays = 30
                    )
                }
            }
        }
        val used = compose.onNodeWithTag("used-days-stat").getUnclippedBoundsInRoot()
        val window = compose.onNodeWithTag("window-stat").getUnclippedBoundsInRoot()
        assertEquals((used.right - used.left).value, (window.right - window.left).value, 1f)
        assertEquals((used.bottom - used.top).value, (window.bottom - window.top).value, 0.1f)
    }
}
