package com.schengen.tracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppRoute(
    val route: String,
    val label: String,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector
) {
    Home(
        route = "home",
        label = "Home",
        iconOutlined = Icons.Outlined.Home,
        iconFilled = Icons.Rounded.Home
    ),
    Trips(
        route = "trips",
        label = "Trips",
        iconOutlined = Icons.AutoMirrored.Outlined.List,
        iconFilled = Icons.AutoMirrored.Rounded.List
    ),
    Calendar(
        route = "calendar",
        label = "Calendar",
        iconOutlined = Icons.Outlined.CalendarMonth,
        iconFilled = Icons.Rounded.CalendarMonth
    ),
    Settings(
        route = "settings",
        label = "Settings",
        iconOutlined = Icons.Outlined.Settings,
        iconFilled = Icons.Rounded.Settings
    );

    companion object {
        val bottomBarRoutes = listOf(Home, Trips, Calendar, Settings)
    }
}
