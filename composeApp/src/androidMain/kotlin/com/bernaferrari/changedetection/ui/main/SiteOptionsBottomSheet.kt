package com.bernaferrari.changedetection.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteOptionsBottomSheet(
    site: Site,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    // Staggered animation states
    var showHeader by remember { mutableStateOf(false) }
    var showRefresh by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showHeader = true
        delay(50)
        showRefresh = true
        delay(50)
        showEdit = true
        delay(50)
        showDelete = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header with animation
            AnimatedVisibility(
                visible = showHeader,
                enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = spring(
                        dampingRatio = ExpressiveSpring.GENTLE_DAMPING,
                        stiffness = ExpressiveSpring.GENTLE_STIFFNESS
                    )
                )
            ) {
                Text(
                    text = site.title?.takeIf { it.isNotBlank() }
                        ?: site.url.replace(Regex("^https?://(www\\.)?"), ""),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedBottomSheetOption(
                visible = showRefresh,
                icon = Icons.Outlined.Refresh,
                text = stringResource(R.string.refresh),
                onClick = onRefresh
            )

            AnimatedBottomSheetOption(
                visible = showEdit,
                icon = Icons.Outlined.Edit,
                text = stringResource(R.string.edit),
                onClick = onEdit
            )

            AnimatedBottomSheetOption(
                visible = showDelete,
                icon = Icons.Outlined.Delete,
                text = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun AnimatedBottomSheetOption(
    visible: Boolean,
    icon: ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
            initialOffsetY = { 20 },
            animationSpec = spring(
                dampingRatio = ExpressiveSpring.GENTLE_DAMPING,
                stiffness = ExpressiveSpring.GENTLE_STIFFNESS
            )
        )
    ) {
        BottomSheetOption(
            icon = icon,
            text = text,
            tint = tint,
            onClick = onClick
        )
    }
}

@Composable
private fun BottomSheetOption(
    icon: ImageVector,
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}
