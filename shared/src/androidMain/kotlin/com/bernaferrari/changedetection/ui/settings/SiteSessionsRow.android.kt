package com.bernaferrari.changedetection.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.bernaferrari.changedetection.browser.clearSiteSessions
import com.bernaferrari.changedetection.localization.stringResource
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.kit.GroupRow

@Composable
internal actual fun SiteSessionsRow() {
    val context = LocalContext.current.applicationContext
    var confirming by remember { mutableStateOf(false) }
    var cleared by remember { mutableStateOf(false) }
    GroupRow(
        title = stringResource(Res.string.site_sessions_title),
        modifier = Modifier.testTag("site-sessions"),
        supporting = stringResource(if (cleared) Res.string.site_sessions_cleared else Res.string.site_sessions_body),
        icon = Icons.AutoMirrored.Outlined.Logout,
        onClick = { confirming = true },
    )
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(Res.string.site_sessions_clear_title)) },
            text = { Text(stringResource(Res.string.site_sessions_clear_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        clearSiteSessions(context) { cleared = true }
                    },
                    modifier = Modifier.testTag("site-sessions-confirm"),
                ) { Text(stringResource(Res.string.site_sessions_clear_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}
