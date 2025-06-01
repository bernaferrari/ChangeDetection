package com.bernaferrari.changedetection.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.getColorFromAttr
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.repo.Snap

@Composable
fun SnapshotRowItem(
    snap: Snap,
    isSelected: Boolean = false,
    onSelectionChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val textColor = if (isSelected) {
        Color(ContextCompat.getColor(context, R.color.md_blue_A200))
    } else {
        Color(context.getColorFromAttr(R.attr.strongColor))
    }
    
    val subtitleColor = if (isSelected) {
        Color(ContextCompat.getColor(context, R.color.md_blue_A200))
    } else {
        Color(ContextCompat.getColor(context, R.color.FontWeak))
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelectionChanged(!isSelected) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = snap.timestamp.convertTimestampToDate(),
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            
            Text(
                text = snap.contentSize.readableFileSize(),
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor
            )
        }
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(ContextCompat.getColor(context, R.color.md_blue_A200))),
                contentAlignment = Alignment.Center
            ) {
                // You could add a checkmark icon here
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SnapshotRowItemPreview() {
    MaterialTheme {
        Surface {
            SnapshotRowItem(
                snap = Snap(
                    siteId = "1",
                    timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
                    contentSize = 15420,
                    contentType = "text/html",
                    contentCharset = "UTF-8"
                ),
                isSelected = false,
                onSelectionChanged = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SnapshotRowItemSelectedPreview() {
    MaterialTheme {
        Surface {
            SnapshotRowItem(
                snap = Snap(
                    siteId = "1",
                    timestamp = System.currentTimeMillis() - 86400000, // 1 day ago
                    contentSize = 8924,
                    contentType = "text/html",
                    contentCharset = "UTF-8"
                ),
                isSelected = true,
                onSelectionChanged = { }
            )
        }
    }
}
