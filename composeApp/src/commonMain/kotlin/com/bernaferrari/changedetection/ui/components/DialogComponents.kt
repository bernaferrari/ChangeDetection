package com.bernaferrari.changedetection.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.hour
import com.bernaferrari.changedetection.shared.generated.resources.hours
import com.bernaferrari.changedetection.shared.generated.resources.min
import org.jetbrains.compose.resources.stringResource

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
    icon: ImageVector,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
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
    cancelText: String = "Cancel",
    confirmText: String = "OK",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onCancel) {
            Text(cancelText)
        }

        Spacer(modifier = Modifier.width(8.dp))

        TextButton(onClick = onConfirm) {
            Text(confirmText)
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
    val minutes = arrayOf(15, 30, 45, 60, 120, 240, 360, 720, 1440, 2880)
    var sliderValue by remember {
        mutableFloatStateOf(minutes.indexOfFirst { it == initialDelay }.coerceAtLeast(0).toFloat())
    }

    val minLabel = stringResource(Res.string.min)
    val hourLabel = stringResource(Res.string.hour)
    val hoursLabel = stringResource(Res.string.hours)

    val getTimeString = { value: Int ->
        when {
            value < 60 -> "$value $minLabel"
            value == 60 -> hourLabel
            else -> "${value / 60} $hoursLabel"
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
