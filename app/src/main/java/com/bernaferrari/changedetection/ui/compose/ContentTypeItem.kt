package com.bernaferrari.changedetection.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bernaferrari.changedetection.R

@Composable
fun ContentTypeItem(
    contentType: String,
    count: Int,
    onRemove: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val (iconRes, iconColor) = when {
        contentType == "application/pdf" -> 
            R.drawable.ic_picture_as_pdf_black_24dp to Color(ContextCompat.getColor(context, R.color.md_red_500))
        contentType.split("/").firstOrNull() == "image" -> 
            R.drawable.ic_image_black_24dp to Color(ContextCompat.getColor(context, R.color.md_green_500))
        contentType == "text/html" -> 
            R.drawable.ic_description_black_24dp to Color(ContextCompat.getColor(context, R.color.md_blue_500))
        else -> 
            R.drawable.ic_insert_drive_file_black_24dp to Color(ContextCompat.getColor(context, R.color.md_grey_500))
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = contentType,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = pluralStringResource(R.plurals.items, count, count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = onRemove
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color(ContextCompat.getColor(context, R.color.FontWeak))
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentTypeItemHtmlPreview() {
    MaterialTheme {
        Surface {
            ContentTypeItem(
                contentType = "text/html",
                count = 15,
                onRemove = { },
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentTypeItemPdfPreview() {
    MaterialTheme {
        Surface {
            ContentTypeItem(
                contentType = "application/pdf",
                count = 3,
                onRemove = { },
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContentTypeItemImagePreview() {
    MaterialTheme {
        Surface {
            ContentTypeItem(
                contentType = "image/jpeg",
                count = 8,
                onRemove = { },
                onClick = { }
            )
        }
    }
}
