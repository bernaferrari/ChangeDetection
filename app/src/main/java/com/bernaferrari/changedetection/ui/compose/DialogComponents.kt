package com.bernaferrari.changedetection.ui.compose

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.bernaferrari.changedetection.R

@Composable
fun DialogHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DialogSwitchItem(
    title: String,
    drawable: Drawable,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = drawable.toBitmap().asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun DialogSeparator(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun DialogActions(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onCancel) {
            Text(stringResource(android.R.string.cancel))
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        TextButton(onClick = onConfirm) {
            Text(stringResource(android.R.string.ok))
        }
    }
}

@Composable
fun DialogIntervalItem(
    title: String,
    initialDelay: Int,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val minutes = arrayOf(15, 30, 45, 60, 120, 240, 360, 720, 1440, 2880)
    var sliderValue by remember { 
        mutableFloatStateOf(minutes.indexOfFirst { it == initialDelay }.toFloat()) 
    }
    
    val getTimeString = { value: Int ->
        when {
            value < 60 -> "$value ${context.getString(R.string.min)}"
            value == 60 -> context.getString(R.string.hour)
            else -> "${value / 60} ${context.getString(R.string.hours)}"
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = getTimeString(minutes[sliderValue.toInt()]),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                sliderValue = newValue
                onValueChange(minutes[newValue.toInt()].toLong())
            },
            valueRange = 0f..(minutes.size - 1).toFloat(),
            steps = minutes.size - 2
        )
    }
}

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.painterResource

@Preview(showBackground = true)
@Composable
private fun DialogHeaderPreview() {
    MaterialTheme {
        Surface {
            DialogHeader(
                title = "Sync Settings",
                subtitle = "Configure how often to check for changes"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DialogSeparatorPreview() {
    MaterialTheme {
        Surface {
            DialogSeparator(title = "Advanced Options")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DialogActionsPreview() {
    MaterialTheme {
        Surface {
            DialogActions(
                onCancel = { },
                onConfirm = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DialogIntervalItemPreview() {
    MaterialTheme {
        Surface {
            DialogIntervalItem(
                title = "Sync Interval",
                initialDelay = 60,
                onValueChange = { }
            )
        }
    }
}
