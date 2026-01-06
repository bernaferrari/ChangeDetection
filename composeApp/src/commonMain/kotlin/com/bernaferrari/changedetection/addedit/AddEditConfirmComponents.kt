package com.bernaferrari.changedetection.addedit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.browser.BrowserSessionData
import com.bernaferrari.changedetection.extensions.normalizeUrl
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.color_theme
import com.bernaferrari.changedetection.shared.generated.resources.complete_setup
import com.bernaferrari.changedetection.shared.generated.resources.save
import com.bernaferrari.changedetection.shared.generated.resources.site_name
import com.bernaferrari.changedetection.shared.generated.resources.site_name_placeholder
import com.bernaferrari.changedetection.shared.generated.resources.start_monitoring
import io.ktor.http.Url
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditConfirmScreen(
    url: String,
    title: String,
    isEditing: Boolean = false,
    selectedColors: Pair<Int, Int>,
    isSaving: Boolean,
    saveError: String?, // Keep parameter for API compatibility, though unused in specific UI logic here
    browserData: BrowserSessionData?, // Keep param for compatibility
    useCookies: Boolean, // Keep param for compatibility
    cookieBytes: Long, // Keep param for compatibility
    onTitleChange: (String) -> Unit,
    onColorChange: (Pair<Int, Int>) -> Unit,
    onUseCookiesChange: (Boolean) -> Unit, // Keep param for compatibility
    onNavigateUp: () -> Unit,
    onSave: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val domain = remember(url) { extractDomain(url) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.complete_setup)) },
                navigationIcon = {
                    IconButton(onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateUp()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Box(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.ime)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave()
                        },
                        enabled = title.isNotBlank() && !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (title.isNotBlank() && !isSaving) {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(selectedColors.first),
                                                Color(selectedColors.second)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        if (isEditing) Icons.Default.Check else Icons.Default.RocketLaunch,
                                        contentDescription = null,
                                        tint = if (title.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        stringResource(if (isEditing) Res.string.save else Res.string.start_monitoring),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (title.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Gradient Hero Banner - shows site visually
            GradientHeroBanner(
                title = title,
                domain = domain,
                selectedColors = selectedColors
            )

            // Site Name Input
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(Res.string.site_name)) },
                placeholder = { Text(stringResource(Res.string.site_name_placeholder)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester), // Focus requester
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(selectedColors.first),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Color Theme Section - Pill-style selector
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(Res.string.color_theme),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ColorThemeSelector(
                    selectedColors = selectedColors,
                    availableColors = defaultAvailableColors,
                    onColorChange = { colors ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onColorChange(colors)
                    }
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun GradientHeroBanner(
    title: String,
    domain: String,
    selectedColors: Pair<Int, Int>
) {
    val displayTitle = title.ifBlank { domain.ifBlank { "Website" } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(selectedColors.first), Color(selectedColors.second))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (domain.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = domain,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorThemeSelector(
    selectedColors: Pair<Int, Int>,
    availableColors: List<Pair<Int, Int>>,
    onColorChange: (Pair<Int, Int>) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(availableColors) { colors ->
            GradientPill(
                colors = colors,
                isSelected = colors == selectedColors,
                onClick = { onColorChange(colors) }
            )
        }
    }
}

@Composable
private fun GradientPill(
    colors: Pair<Int, Int>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .height(44.dp)
            .width(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(colors.first), Color(colors.second))
                )
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(22.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(visible = isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Original gradient colors from GradientColors.kt
private val defaultAvailableColors = listOf(
    Pair(0xff40C4FF.toInt(), 0xff536DFE.toInt()), // Light Blue A200 to Indigo A200
    Pair(0xfffeb47b.toInt(), 0xffff7e5f.toInt()), // Ed's Sunset Gradient
    Pair(0xffF54EA2.toInt(), 0xffFF7676.toInt()), // Pink -> Pink
    Pair(0xff17EAD9.toInt(), 0xff448AFF.toInt()), // Cyan -> Blue
    Pair(0xff20e281.toInt(), 0xff00bf8f.toInt()), // Green -> Light-Green
    Pair(0xff7117EA.toInt(), 0xffEA6060.toInt()), // Purple -> Instagram-Purple
    Pair(0xffFBB03B.toInt(), 0xffD4145A.toInt()), // Sanguine
    Pair(0xffFEAC5E.toInt(), 0xff5d0058.toInt()), // Yellow -> Purple
    Pair(0xffffb88c.toInt(), 0xffde6262.toInt()), // A Lost Memory
    Pair(0xff1e3c72.toInt(), 0xff2a5298.toInt()), // Eternal Constance
    Pair(0xff3AA17E.toInt(), 0xff00537E.toInt()), // New Leaf
    Pair(0xff29ABE2.toInt(), 0xff4F00BC.toInt()), // Deep Sea
    Pair(0xffEBC08D.toInt(), 0xffF24645.toInt()), // Fizzy Peach
    Pair(0xff622774.toInt(), 0xffC53364.toInt())  // Dark-Purple -> Light-Purple
)

private fun extractDomain(url: String): String {
    return runCatching {
        val sanitized = url.trim()
        if (sanitized.isEmpty()) return@runCatching ""
        val normalized = sanitized.normalizeUrl() ?: "https://$sanitized"
        val host = Url(normalized).host
        host.removePrefix("www.")
    }.getOrDefault("")
}
