package com.bernaferrari.changedetection.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.model.ThemeMode
import com.bernaferrari.sdkmonitor.ui.settings.ThemeModeToggle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onAboutClick: () -> Unit = {},
    onBackgroundSyncClick: () -> Unit = {},
    onRecreateActivity: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBackgroundSyncDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            SettingsHeroSection(
                isBackgroundSyncEnabled = uiState.backgroundSync,
                syncInterval = uiState.syncInterval,
                onManageSync = { showBackgroundSyncDialog = true }
            )
        }
        item {
            SectionLabel(text = stringResource(id = R.string.settings_section_appearance))
            ThemeSettingsCard(
                selectedTheme = uiState.themeMode,
                onThemeSelected = viewModel::updateThemeMode
            )
        }
        item {
            SectionLabel(text = stringResource(id = R.string.settings_monitoring_section))
            SettingsCard(
                icon = Icons.Default.Sync,
                title = stringResource(id = R.string.settings_background_sync_title),
                subtitle = if (uiState.backgroundSync) {
                    stringResource(id = R.string.settings_background_sync_every, uiState.syncInterval)
                } else {
                    stringResource(id = R.string.settings_background_sync_disabled)
                },
                onClick = { showBackgroundSyncDialog = true },
                trailing = {
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    Switch(
                        checked = uiState.backgroundSync,
                        onCheckedChange = {
                            haptic.performHapticFeedback(
                                androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                            )
                            viewModel.updateBackgroundSync(it)
                        }
                    )
                }
            )
        }
        item {
            SectionLabel(text = stringResource(id = R.string.settings_about_section))
            SettingsCard(
                icon = Icons.Default.Info,
                title = stringResource(id = R.string.settings_about_title),
                subtitle = stringResource(id = R.string.settings_about_subtitle),
                onClick = onAboutClick
            )
        }
        item {
            SettingsQuickActions(
                onContact = {
                    val email = "bernaferrari2+changedetection@gmail.com"
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$email")
                        putExtra(Intent.EXTRA_SUBJECT, "Change Detection – Support")
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.settings_action_contact)))
                },
                onDocs = onBackgroundSyncClick
            )
        }
    }

    if (showBackgroundSyncDialog) {
        BackgroundSyncDialog(
            onDismiss = { showBackgroundSyncDialog = false },
            viewModel = viewModel
        )
    }
}

@Composable
private fun SettingsHeroSection(
    isBackgroundSyncEnabled: Boolean,
    syncInterval: Long,
    onManageSync: () -> Unit
) {
    val subtitle = if (isBackgroundSyncEnabled) {
        stringResource(id = R.string.settings_hero_subtitle_sync_on)
    } else {
        stringResource(id = R.string.settings_hero_subtitle_sync_off)
    }
    val statusText = if (isBackgroundSyncEnabled) {
        stringResource(id = R.string.settings_background_sync_every, syncInterval)
    } else {
        stringResource(id = R.string.settings_background_sync_disabled)
    }
    val heroGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.secondary
        ),
        start = Offset(0f, 0f),
        end = Offset(600f, 400f)
    )
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(heroGradient)
            .padding(horizontal = 28.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = contentColor.copy(alpha = 0.15f)
        ) {
            Text(
                text = stringResource(id = R.string.settings_hero_badge),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Text(
            text = stringResource(id = R.string.settings_hero_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor.copy(alpha = 0.9f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = contentColor
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_background_sync_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.85f)
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor
                )
            }
        }
        Button(
            onClick = onManageSync,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = contentColor,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = stringResource(id = R.string.settings_hero_action))
        }
    }
}

@Composable
private fun ThemeSettingsCard(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.settings_theme_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeMode.entries.forEach { theme ->
                    Box(modifier = Modifier.weight(1f)) {
                        ThemeModeToggle(
                            themeMode = theme,
                            isSelected = selectedTheme == theme.name,
                            onClick = { onThemeSelected(theme.name) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            trailing?.invoke()
        }
    }
}

@Composable
private fun SettingsQuickActions(
    onContact: () -> Unit,
    onDocs: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(text = stringResource(id = R.string.settings_quick_actions_title))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ) {
            Column {
                QuickActionRow(
                    icon = Icons.Filled.Email,
                    title = stringResource(id = R.string.settings_action_contact),
                    onClick = onContact
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                QuickActionRow(
                    icon = Icons.Default.Info,
                    title = stringResource(id = R.string.settings_action_docs),
                    onClick = onDocs
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun QuickActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
