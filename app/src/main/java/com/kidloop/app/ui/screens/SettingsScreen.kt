package com.kidloop.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kidloop.app.R
import com.kidloop.app.data.model.DailyUsage
import com.kidloop.app.data.model.ItemType
import com.kidloop.app.data.model.KidLoopSettings
import com.kidloop.app.data.model.PlaylistItem
import com.kidloop.app.ui.components.PinSetupDialog
import com.kidloop.app.util.ParsedYouTubeItem
import com.kidloop.app.util.YouTubeUrlParser

const val START_KID_MODE_BUTTON_TEST_TAG = "StartKidModeButton"
const val ADD_LINK_BUTTON_TEST_TAG = "AddLinkButton"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: KidLoopSettings,
    dailyUsage: DailyUsage,
    onAddLink: (PlaylistItem) -> Unit,
    onRemoveLink: (String) -> Unit,
    onReorderLink: (Int, Int) -> Unit,
    onSavePin: (String) -> Unit,
    onUpdateSessionLimit: (Int) -> Unit,
    onUpdateDailyLimit: (Int) -> Unit,
    onResetTodayUsage: () -> Unit,
    onToggleAllowVolume: (Boolean) -> Unit,
    onStartKidMode: () -> Unit
) {
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showResetUsageConfirm by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val noVideosError = stringResource(R.string.settings_validation_no_videos)
    val noPinError = stringResource(R.string.settings_validation_no_pin)
    val dailyExhaustedTemplate = stringResource(R.string.settings_daily_exhausted)

    // First launch check: force PIN setup if not configured
    if (!settings.isPinConfigured) {
        PinSetupDialog(
            isInitialSetup = true,
            onPinSaved = { pin ->
                onSavePin(pin)
            },
            onDismiss = {}
        )
    }

    if (showPinDialog) {
        PinSetupDialog(
            isInitialSetup = false,
            onPinSaved = { pin ->
                onSavePin(pin)
                showPinDialog = false
            },
            onDismiss = { showPinDialog = false }
        )
    }

    if (showAddLinkDialog) {
        AddLinkDialog(
            onConfirm = { item ->
                onAddLink(item)
                showAddLinkDialog = false
                validationError = null
            },
            onDismiss = { showAddLinkDialog = false }
        )
    }

    if (showResetUsageConfirm) {
        AlertDialog(
            onDismissRequest = { showResetUsageConfirm = false },
            title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetTodayUsage()
                        showResetUsageConfirm = false
                        validationError = null
                    }
                ) {
                    Text(stringResource(R.string.action_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetUsageConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (validationError != null) {
                        Text(
                            text = validationError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (!settings.isPinConfigured) {
                                validationError = noPinError
                            } else if (settings.items.isEmpty()) {
                                validationError = noVideosError
                            } else if (dailyUsage.usedMinutesToday >= settings.dailyLimitMinutes) {
                                validationError = String.format(dailyExhaustedTemplate, settings.dailyLimitMinutes)
                            } else {
                                validationError = null
                                onStartKidMode()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag(START_KID_MODE_BUTTON_TEST_TAG),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_launch_kid_mode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Section 1: Video Playlist
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_section_playlist),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { showAddLinkDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag(ADD_LINK_BUTTON_TEST_TAG)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_add))
                    }
                }
            }

            if (settings.items.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.settings_empty_playlist),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                itemsIndexed(settings.items, key = { _, item -> item.id }) { index, item ->
                    PlaylistItemCard(
                        item = item,
                        index = index,
                        totalCount = settings.items.size,
                        onMoveUp = { onReorderLink(index, index - 1) },
                        onMoveDown = { onReorderLink(index, index + 1) },
                        onDelete = { onRemoveLink(item.id) }
                    )
                }
            }

            // Section 2: Parent Security
            item {
                Text(
                    text = stringResource(R.string.settings_section_security),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (settings.isPinConfigured) Icons.Default.CheckCircle else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (settings.isPinConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Column {
                                Text(
                                    text = stringResource(
                                        if (settings.isPinConfigured) R.string.settings_pin_configured
                                        else R.string.settings_setup_pin
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showPinDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                stringResource(
                                    if (settings.isPinConfigured) R.string.settings_change_pin
                                    else R.string.settings_setup_pin
                                )
                            )
                        }
                    }
                }
            }

            // Section 3: Screen Time Limits
            item {
                Text(
                    text = stringResource(R.string.settings_section_limits),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Session limit slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.settings_session_limit, settings.sessionLimitMinutes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Slider(
                            value = settings.sessionLimitMinutes.toFloat(),
                            onValueChange = { onUpdateSessionLimit(it.toInt()) },
                            valueRange = 5f..120f,
                            steps = 22,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Daily limit slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = stringResource(R.string.settings_daily_limit, settings.dailyLimitMinutes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Slider(
                            value = settings.dailyLimitMinutes.toFloat(),
                            onValueChange = { onUpdateDailyLimit(it.toInt()) },
                            valueRange = 15f..240f,
                            steps = 14,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Today's total usage display & reset
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.settings_today_usage,
                                    dailyUsage.usedMinutesToday,
                                    settings.dailyLimitMinutes
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )

                            OutlinedButton(
                                onClick = { showResetUsageConfirm = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.action_reset))
                            }
                        }
                    }
                }
            }

            // Section 4: Hardware & Audio Control
            item {
                Text(
                    text = stringResource(R.string.settings_section_hardware),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_allow_volume_keys),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.settings_allow_volume_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = settings.allowVolumeKeys,
                            onCheckedChange = { onToggleAllowVolume(it) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PlaylistItemCard(
    item: PlaylistItem,
    index: Int,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type badge icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (item.type == ItemType.PLAYLIST) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.type == ItemType.PLAYLIST) Icons.AutoMirrored.Filled.PlaylistPlay else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (item.type == ItemType.PLAYLIST) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.originalUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Up / Down / Delete actions
            IconButton(
                onClick = onMoveUp,
                enabled = index > 0
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = stringResource(R.string.action_move_up)
                )
            }

            IconButton(
                onClick = onMoveDown,
                enabled = index < totalCount - 1
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = stringResource(R.string.action_move_down)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddLinkDialog(
    onConfirm: (PlaylistItem) -> Unit,
    onDismiss: () -> Unit
) {
    var rawUrl by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val invalidUrlError = stringResource(R.string.dialog_add_link_invalid)
    val videoTypeLabel = stringResource(R.string.dialog_add_link_type_video)
    val playlistTypeLabel = stringResource(R.string.dialog_add_link_type_playlist)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_link_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = rawUrl,
                    onValueChange = {
                        rawUrl = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.dialog_add_link_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text(stringResource(R.string.dialog_add_link_title_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = YouTubeUrlParser.parse(rawUrl)
                    if (parsed == null) {
                        errorMessage = invalidUrlError
                    } else {
                        val isPlaylist = parsed is ParsedYouTubeItem.Playlist
                        val defaultTitle = if (isPlaylist) {
                            "$playlistTypeLabel (${parsed.id})"
                        } else {
                            "$videoTypeLabel (${parsed.id})"
                        }
                        val title = titleInput.trim().ifBlank { defaultTitle }
                        val item = PlaylistItem(
                            id = parsed.id,
                            type = if (isPlaylist) ItemType.PLAYLIST else ItemType.VIDEO,
                            title = title,
                            originalUrl = parsed.originalUrl
                        )
                        onConfirm(item)
                    }
                }
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
