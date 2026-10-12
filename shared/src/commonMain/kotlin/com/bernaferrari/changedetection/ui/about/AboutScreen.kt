@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.about

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.about_developer_name
import com.bernaferrari.changedetection.shared.generated.resources.about_elsewhere
import com.bernaferrari.changedetection.shared.generated.resources.about_license_short
import com.bernaferrari.changedetection.shared.generated.resources.about_story
import com.bernaferrari.changedetection.shared.generated.resources.about_view_licenses
import com.bernaferrari.changedetection.shared.generated.resources.about_view_on_github
import com.bernaferrari.changedetection.shared.generated.resources.app_name
import com.bernaferrari.changedetection.shared.generated.resources.brand_tagline
import com.bernaferrari.changedetection.shared.generated.resources.settings_about_value
import com.bernaferrari.changedetection.shared.generated.resources.settings_feedback
import com.bernaferrari.changedetection.shared.generated.resources.settings_privacy_line
import com.bernaferrari.changedetection.ui.kit.AppMark
import com.bernaferrari.changedetection.ui.kit.GroupLabel
import com.bernaferrari.changedetection.ui.kit.GroupRow
import com.bernaferrari.changedetection.ui.kit.ListGroup
import com.bernaferrari.changedetection.localization.stringResource
import org.jetbrains.compose.resources.painterResource
import com.bernaferrari.changedetection.shared.generated.resources.github_logo
import com.bernaferrari.changedetection.shared.generated.resources.x_logo
import com.bernaferrari.changedetection.shared.generated.resources.reddit_logo
import com.bernaferrari.changedetection.shared.generated.resources.linkedin_logo

/**
 * About, in the Expressive style: the app mark on a slowly turning shape, the two things people come here
 * for (the code and a way to reach the author) as bold container cards, a short personal note, and the
 * author's other places as one connected group.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onSocialClick: (String) -> Unit,
    onGitHubClick: (String) -> Unit,
    onContactClick: () -> Unit,
    onLicenseUrlClick: (String) -> Unit,
    appVersion: String = "",
) {
    var showLicensesDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { com.bernaferrari.changedetection.ui.navigation.AppTopBar(title = null, onBack = onBack) },
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 600.dp).fillMaxWidth().testTag("about-list"), contentPadding = PaddingValues(bottom = 40.dp)) {
                item(key = "hero") { Hero(appVersion) }
                item(key = "actions") {
                    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min).padding(start = 16.dp, end = 16.dp, top = 32.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionCard(
                            title = stringResource(Res.string.about_view_on_github),
                            body = stringResource(Res.string.about_license_short),
                            icon = Icons.Default.Code,
                            container = MaterialTheme.colorScheme.primaryContainer,
                            content = MaterialTheme.colorScheme.onPrimaryContainer,
                            badge = MaterialShapes.Cookie9Sided.toShape(),
                            shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 12.dp, bottomEnd = 12.dp),
                            onClick = { onGitHubClick("https://github.com/bernaferrari/ChangeDetection") },
                            modifier = Modifier.weight(1f),
                        )
                        ActionCard(
                            title = stringResource(Res.string.settings_feedback),
                            body = "bernaferrari2@gmail.com",
                            icon = Icons.Default.Email,
                            container = MaterialTheme.colorScheme.secondaryContainer,
                            content = MaterialTheme.colorScheme.onSecondaryContainer,
                            badge = MaterialShapes.Clover4Leaf.toShape(),
                            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 32.dp, bottomEnd = 32.dp),
                            onClick = onContactClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item(key = "note") { Note(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) }
                item(key = "licenses") {
                    ListGroup(Modifier.padding(top = 8.dp)) {
                        GroupRow(
                            title = stringResource(Res.string.about_view_licenses),
                            icon = Icons.Default.Description,
                            onClick = { showLicensesDialog = true },
                        )
                    }
                }
                item(key = "elsewhere") {
                    GroupLabel(stringResource(Res.string.about_elsewhere))
                    Elsewhere(onSocialClick)
                }
                item(key = "privacy") {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 24.dp, start = 32.dp, end = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(20.dp).testTag("privacy-shield"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            stringResource(Res.string.settings_privacy_line),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    if (showLicensesDialog) {
        AboutLicensesDialog(onDismiss = { showLicensesDialog = false }, onUrlClick = onLicenseUrlClick)
    }
}

/** The one hero moment: the mark on a soft, slowly turning Expressive shape, then name, tagline and version. */
@Composable
private fun Hero(appVersion: String) {
    val spin by rememberInfiniteTransition(label = "hero").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin",
    )
    val shape = MaterialShapes.Cookie12Sided.toShape()
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.padding(top = 8.dp).size(168.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().rotate(spin).clip(shape).background(MaterialTheme.colorScheme.primaryContainer))
            AppMark(Modifier.size(96.dp))
        }
        Text(
            stringResource(Res.string.app_name),
            Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.brand_tagline),
            Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (appVersion.isNotBlank()) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    stringResource(Res.string.settings_about_value, appVersion),
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A bold, colored card for a primary action, with its icon on an Expressive shape. */
@Composable
private fun ActionCard(
    title: String,
    body: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    badge: Shape,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(onClick = onClick, shape = shape, color = container, contentColor = content, modifier = modifier.fillMaxHeight().heightIn(min = 152.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(48.dp).clip(badge).background(content), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(22.dp), tint = container)
                }
                Box(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp), tint = content.copy(alpha = 0.7f))
            }
            Text(title, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.8f), maxLines = 2)
        }
    }
}

/** The author's note, set like a quote. */
@Composable
private fun Note(modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp, 12.dp, 32.dp, 32.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.FormatQuote, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(Res.string.about_story), style = MaterialTheme.typography.bodyLarge)
            Text(
                "— " + stringResource(Res.string.about_developer_name),
                Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** The author's other places as one connected group: rounded outer ends, tight inner joins. */
@Composable
private fun Elsewhere(onClick: (String) -> Unit) {
    val links = listOf(
        Triple("GitHub", "https://github.com/bernaferrari", Res.drawable.github_logo),
        Triple("X", "https://x.com/bernaferrari", Res.drawable.x_logo),
        Triple("Reddit", "https://www.reddit.com/user/bernaferrari", Res.drawable.reddit_logo),
        Triple("LinkedIn", "https://www.linkedin.com/in/bernaferrari", Res.drawable.linkedin_logo),
    )
    FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp), maxItemsInEachRow = 4) {
        links.forEachIndexed { index, (label, url, icon) ->
            val shape = when (index) {
                0 -> RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                links.lastIndex -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 28.dp, bottomEnd = 28.dp)
                else -> RoundedCornerShape(8.dp)
            }
            Surface(
                onClick = { onClick(url) },
                shape = shape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
