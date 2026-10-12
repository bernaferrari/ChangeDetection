package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.localization.stringResource

/** Owns the history decision; callers receive only the action explicitly confirmed by the user. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AddressChangeDialog(onCancel: () -> Unit, onKeepHistory: () -> Unit, onStartFresh: () -> Unit) {
    var urlHistoryChoice by rememberSaveable { mutableStateOf(UrlHistoryChoice.KeepHistory) }
    val spacing = MaterialTheme.spacing
    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = stringResource(Res.string.address_changed_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()).selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Text(
                    text = stringResource(Res.string.address_changed_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                UrlHistoryChoice.entries.forEach { choice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = spacing.touchTarget)
                            .selectable(
                                selected = urlHistoryChoice == choice,
                                role = Role.RadioButton,
                                onClick = { urlHistoryChoice = choice },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = urlHistoryChoice == choice,
                            onClick = null,
                        )
                        Text(
                            text = stringResource(
                                if (choice == UrlHistoryChoice.StartFresh) {
                                    Res.string.address_changed_start_fresh
                                } else {
                                    Res.string.address_changed_keep_history
                                },
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (urlHistoryChoice == UrlHistoryChoice.StartFresh) onStartFresh() else onKeepHistory()
                },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(stringResource(Res.string.continue_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

private enum class UrlHistoryChoice { KeepHistory, StartFresh }
