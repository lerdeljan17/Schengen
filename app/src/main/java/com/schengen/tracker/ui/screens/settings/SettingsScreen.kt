package com.schengen.tracker.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.schengen.tracker.domain.Profile
import com.schengen.tracker.ui.AppViewModel
import com.schengen.tracker.ui.components.AppTopBar
import com.schengen.tracker.ui.components.SettingsDivider
import com.schengen.tracker.ui.components.SettingsRow
import com.schengen.tracker.ui.components.SettingsSection
import com.schengen.tracker.ui.theme.AppearanceMode
import com.schengen.tracker.ui.theme.ColorThemeSpec
import com.schengen.tracker.ui.theme.ColorThemes
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeState by viewModel.themeState.collectAsState()
    val context = LocalContext.current
    val signInAccount by viewModel.signInManager.account.collectAsState()

    var appearanceDialog by remember { mutableStateOf(false) }
    var themeDialog by remember { mutableStateOf(false) }
    var profilesDialog by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var addProfileDialog by remember { mutableStateOf(false) }
    var editProfile by remember { mutableStateOf<Profile?>(null) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result.data)
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        if (hasForegroundLocationPermission(context)) {
            viewModel.setLocationTracking(true)
        }
    }
    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val hasForeground = hasForegroundLocationPermission(context)
        viewModel.setLocationTracking(hasForeground)
        if (
            hasForeground &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !hasBackgroundLocationPermission(context)
        ) {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) viewModel.exportCsv(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importCsv(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        AppTopBar(title = "Settings")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                SettingsSection(title = "Preferences") {
                    SettingsRow(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "Start week on Sunday",
                        subtitle = "Use Sunday as the first day of the week.",
                        trailing = {
                            Switch(
                                checked = themeState.startWeekOnSunday,
                                onCheckedChange = viewModel::setStartWeekOnSunday
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Person,
                        title = "Passport profiles",
                        subtitle = uiState.profiles.firstOrNull { it.id == uiState.activeProfileId }?.let {
                            "Active: ${it.name}"
                        } ?: "Manage traveler profiles",
                        onClick = { profilesDialog = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Outlined.LocationOn,
                        title = "Location tracking",
                        subtitle = uiState.locationStatusMessage
                            ?: "Auto-record entries/exits using device location.",
                        trailing = {
                            Switch(
                                checked = uiState.locationTrackingEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        if (hasForegroundLocationPermission(context)) {
                                            viewModel.setLocationTracking(true)
                                            if (
                                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                                !hasBackgroundLocationPermission(context)
                                            ) {
                                                backgroundPermissionLauncher.launch(
                                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                                )
                                            }
                                        } else {
                                            val permissions = buildList {
                                                add(Manifest.permission.ACCESS_FINE_LOCATION)
                                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            }
                                            foregroundPermissionLauncher.launch(permissions.toTypedArray())
                                        }
                                    } else {
                                        viewModel.setLocationTracking(false)
                                    }
                                }
                            )
                        }
                    )
                    if (uiState.locationTrackingEnabled) {
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Outlined.LocationOn,
                            title = "Check now",
                            subtitle = "Resolve current country and update trips.",
                            onClick = { viewModel.runLocationCheckNow() }
                        )
                    }
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Notifications,
                        title = "Overstay alerts",
                        subtitle = "Notify me when I'm close to the 90-day limit.",
                        trailing = {
                            Switch(
                                checked = uiState.overstayAlertsEnabled,
                                onCheckedChange = viewModel::setOverstayAlertsEnabled
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "Appearance") {
                    SettingsRow(
                        icon = Icons.Outlined.DarkMode,
                        title = "Appearance mode",
                        subtitle = appearanceLabel(themeState.appearanceMode),
                        onClick = { appearanceDialog = true }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Outlined.ColorLens,
                        title = "Color theme",
                        subtitle = ColorThemes.byId(themeState.colorThemeId).displayName,
                        onClick = { themeDialog = true }
                    )
                }
            }

            item {
                SettingsSection(title = "Data") {
                    SettingsRow(
                        icon = Icons.Outlined.Backup,
                        title = "Backup trips",
                        subtitle = "Save your trips as a CSV file.",
                        onClick = {
                            exportLauncher.launch("schengen-backup-${java.time.LocalDate.now()}.csv")
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Restore,
                        title = "Restore trips",
                        subtitle = "Import trips from a CSV backup.",
                        onClick = {
                            importLauncher.launch(arrayOf("text/*", "text/csv", "application/csv"))
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Delete,
                        title = "Delete all trips",
                        subtitle = "Remove every trip from the app.",
                        titleColor = MaterialTheme.colorScheme.error,
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = { deleteConfirm = true }
                    )
                }
            }

            item {
                SettingsSection(title = "Sync") {
                    SettingsRow(
                        icon = if (signInAccount != null) Icons.Outlined.CloudSync else Icons.Outlined.CloudOff,
                        title = "Sync status",
                        subtitle = syncStatusLabel(
                            signedIn = signInAccount?.email,
                            lastSyncAt = uiState.lastSyncAt,
                            error = uiState.syncErrorMessage
                        ),
                        onClick = {
                            if (signInAccount == null) signInLauncher.launch(viewModel.signInIntent())
                        }
                    )
                    if (signInAccount != null) {
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Outlined.Sync,
                            title = if (uiState.syncInProgress) "Syncing…" else "Sync now",
                            subtitle = "Upload your trips to Google Drive AppData.",
                            onClick = { if (!uiState.syncInProgress) viewModel.syncNow() }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Outlined.CloudSync,
                            title = "Restore from Drive",
                            subtitle = "Replace local data with the latest Drive snapshot.",
                            onClick = { if (!uiState.syncInProgress) viewModel.restoreFromDrive() }
                        )
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Outlined.CloudOff,
                            title = "Sign out",
                            subtitle = "Disconnect Google account from this device.",
                            onClick = { viewModel.signOut() }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = "Support") {
                    SettingsRow(
                        icon = Icons.Outlined.Email,
                        title = "Contact support",
                        subtitle = "Email us about a bug or feature request.",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@schengentracker.app")
                                putExtra(Intent.EXTRA_SUBJECT, "Schengen Tracker support")
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    if (appearanceDialog) {
        ChoiceDialog(
            title = "Appearance mode",
            options = AppearanceMode.values().toList(),
            selected = themeState.appearanceMode,
            labelOf = ::appearanceLabel,
            onDismiss = { appearanceDialog = false },
            onSelect = {
                viewModel.setAppearanceMode(it)
                appearanceDialog = false
            }
        )
    }

    if (themeDialog) {
        ChoiceDialog(
            title = "Color theme",
            options = ColorThemes.all,
            selected = ColorThemes.byId(themeState.colorThemeId),
            labelOf = ColorThemeSpec::displayName,
            onDismiss = { themeDialog = false },
            onSelect = {
                viewModel.setColorTheme(it.id)
                themeDialog = false
            }
        )
    }

    if (profilesDialog) {
        ProfilesDialog(
            profiles = uiState.profiles,
            activeId = uiState.activeProfileId,
            onSelect = viewModel::selectProfile,
            onEdit = { editProfile = it },
            onAdd = { addProfileDialog = true },
            onDismiss = { profilesDialog = false }
        )
    }

    if (addProfileDialog) {
        AddProfileDialog(
            onDismiss = { addProfileDialog = false },
            onCreate = { name, passport ->
                viewModel.addProfile(name, passport)
                addProfileDialog = false
            }
        )
    }

    editProfile?.let { profile ->
        EditProfileDialog(
            profile = profile,
            onDismiss = { editProfile = null },
            onSave = { name, passport ->
                viewModel.updateProfile(profile.id, name, passport)
                editProfile = null
            },
            onDelete = {
                viewModel.deleteProfile(profile.id)
                editProfile = null
            }
        )
    }

    if (deleteConfirm) {
        DeleteAllTripsDialog(
            onDismiss = { deleteConfirm = false },
            onConfirm = { allProfiles ->
                viewModel.deleteAllTrips(allProfiles)
                deleteConfirm = false
            }
        )
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = { onSelect(option) }
                        )
                        Spacer(Modifier.padding(start = 4.dp))
                        Text(
                            labelOf(option),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun ProfilesDialog(
    profiles: List<Profile>,
    activeId: Long?,
    onSelect: (Long) -> Unit,
    onEdit: (Profile) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Passport profiles") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (profiles.isEmpty()) {
                    Text("No profiles yet.")
                }
                profiles.forEach { profile ->
                    val active = profile.id == activeId
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = active,
                            onClick = { onSelect(profile.id) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.name)
                            if (profile.passportNumber.isNotBlank()) {
                                Text(
                                    "Passport: ${profile.passportNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = { onEdit(profile) }) { Text("Edit") }
                    }
                }
                Button(onClick = onAdd) { Text("Add profile") }
            }
        }
    )
}

@Composable
private fun AddProfileDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var passport by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onCreate(name, passport) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Add passport profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = passport,
                    onValueChange = { passport = it },
                    label = { Text("Passport number") },
                    singleLine = true
                )
            }
        }
    )
}

@Composable
private fun EditProfileDialog(
    profile: Profile,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var passport by remember(profile.id) { mutableStateOf(profile.passportNumber) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(name, passport) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Edit profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = passport,
                    onValueChange = { passport = it },
                    label = { Text("Passport number") },
                    singleLine = true
                )
                TextButton(onClick = onDelete) {
                    Text("Delete profile", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

@Composable
private fun DeleteAllTripsDialog(
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    var allProfiles by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(allProfiles) }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Delete all trips?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This permanently removes your trip data. Make a backup first if you might want it later.")
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = allProfiles,
                        onCheckedChange = { allProfiles = it }
                    )
                    Spacer(Modifier.padding(start = 8.dp))
                    Text("Delete trips for ALL profiles")
                }
            }
        }
    )
}

private fun appearanceLabel(mode: AppearanceMode): String = when (mode) {
    AppearanceMode.SYSTEM -> "System"
    AppearanceMode.LIGHT -> "Light"
    AppearanceMode.DARK -> "Dark"
}

private fun syncStatusLabel(signedIn: String?, lastSyncAt: Long, error: String?): String {
    if (error != null) return error
    if (signedIn == null) return "Tap to sign in with Google."
    val sub = if (lastSyncAt > 0L) {
        val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        "Signed in as $signedIn. Last synced: ${df.format(Date(lastSyncAt))}"
    } else "Signed in as $signedIn. Not synced yet."
    return sub
}

private fun hasForegroundLocationPermission(context: android.content.Context): Boolean {
    val hasFine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return hasFine || hasCoarse
}

private fun hasBackgroundLocationPermission(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}
