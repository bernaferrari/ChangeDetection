package com.bernaferrari.changedetection.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** The same compact, accessible palette chip in Settings and the page icon picker. */
@Composable
internal fun PaletteColorSwatch(color: Color, label: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
    Box(
        Modifier.size(width = 56.dp, height = 48.dp)
            .semantics { contentDescription = label }
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(width = 48.dp, height = 36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color)
                .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (selected || icon != null) {
                Icon(if (selected) Icons.Rounded.Check else requireNotNull(icon), null, Modifier.size(18.dp), tint = if (color.luminance() > 0.55f) Color.Black else Color.White)
            }
        }
    }
}
