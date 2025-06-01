package com.bernaferrari.changedetection.ui.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.bernaferrari.changedetection.BuildConfig
import com.bernaferrari.changedetection.R
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class AboutItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController
) {
    AboutScreenContent(
        onNavigateBack = { navController.popBackStack() },
        onLicensesClick = { navController.navigate("about_licenses") }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreenContent(
    onNavigateBack: () -> Unit,
    onLicensesClick: () -> Unit
) {
    val context = LocalContext.current
    val email = "bernaferrari2@gmail.com"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "© 2018 Bernardo Ferrari",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // App Actions
            item {
                AboutCard(title = "App") {
                    val appItems = listOf(
                        AboutItem(
                            title = stringResource(R.string.licenses),
                            icon = Icons.Default.Description,
                            onClick = { navController.navigate("about_licenses") }
                        ),
                        AboutItem(
                            title = stringResource(R.string.rate),
                            icon = Icons.Default.Star,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("market://details?id=${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        )
                    )
                    
                    appItems.forEach { item ->
                        AboutListItem(item = item)
                    }
                }
            }

            // Author Card
            item {
                AboutCard(title = stringResource(R.string.author)) {
                    val authorItems = listOf(
                        AboutItem(
                            title = "Bernardo Ferrari",
                            subtitle = "bernaferrari",
                            icon = Icons.Default.Person,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://www.reddit.com/user/bernaferrari")
                            }
                            context.startActivity(intent)
                        }
                        ),
                        AboutItem(
                            title = stringResource(R.string.github),
                            icon = Icons.Default.Code,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://github.com/bernaferrari/ChangeDetection")
                            }
                            context.startActivity(intent)
                        }
                        ),
                        AboutItem(
                            title = stringResource(R.string.email),
                            subtitle = email,
                            icon = Icons.Default.Email,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$email")
                                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject))
                            }
                            context.startActivity(intent)
                        }
                        )
                    )
                    
                    authorItems.forEach { item ->
                        AboutListItem(item = item)
                    }
                }
            }

            // Icon Designer Card
            item {
                AboutCard(title = stringResource(R.string.icon_designer)) {
                    AboutListItem(
                        item = AboutItem(
                            title = "GitHub",
                            icon = Icons.Default.Code,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://github.com/familqasimov")
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // Help Card
            item {
                AboutCard(title = stringResource(R.string.help)) {
                    AboutListItem(
                        item = AboutItem(
                            title = stringResource(R.string.bugs),
                            subtitle = email,
                            icon = Icons.Default.BugReport,
                            onClick = { sendEmailWithLogs(context, email) }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun AboutListItem(
    item: AboutItem
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = item.onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                item.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun sendEmailWithLogs(context: Context, email: String) {
    try {
        val log = StringBuilder()
        val process = Runtime.getRuntime().exec("logcat -d")
        val br = BufferedReader(InputStreamReader(process.inputStream))

        var line: String? = br.readLine()
        while (line != null) {
            log.append(line + "\n")
            line = br.readLine()
        }
        br.close()

        val file = File(context.cacheDir, "logs.txt")
        file.createNewFile()
        file.writeBytes(log.toString().toByteArray())

        val contentUri = FileProvider.getUriForFile(
            context,
            "com.bernaferrari.changedetection.files",
            file
        )

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(emailIntent, "Send email..."))
    } catch (e: Exception) {
        // Fallback to simple email intent without logs
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject))
        }
        context.startActivity(intent)
    }
}
