package com.bernaferrari.changedetection.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class License(
    val name: String,
    val copyright: String,
    val author: String,
    val licenseType: String,
    val url: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutLicensesScreen(
    navController: NavController
) {
    AboutLicensesScreenContent(
        onNavigateBack = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutLicensesScreenContent(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    val licenses = listOf(
        License(
            name = "The Android Open Source Project",
            copyright = "2018",
            author = "Google Inc.",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "java-diff-utils",
            copyright = "2018",
            author = "Tobias (wumpz)",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "Dagger",
            copyright = "2012",
            author = "The Dagger Authors",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "Android Iconics",
            copyright = "2017",
            author = "Mike Penz",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "RxJava: Reactive Extensions for the JVM",
            copyright = "2016-present",
            author = "RxJava Contributors",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "RxAndroid: Reactive Extensions for Android",
            copyright = "2015",
            author = "The RxAndroid authors",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "Groupie",
            copyright = "2016",
            author = "",
            licenseType = "MIT License"
        ),
        License(
            name = "material-about-library",
            copyright = "2016",
            author = "Daniel Stone",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "Logger",
            copyright = "2017",
            author = "Orhan Obut",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "Jsoup",
            copyright = "2009-2018",
            author = "Jonathan Hedley <jonathan@hedley.net>",
            licenseType = "MIT License"
        ),
        License(
            name = "OkHttp",
            copyright = "",
            author = "",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "Material Dialogs",
            copyright = "2018",
            author = "Aidan Michael Follestad",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "Kotlin",
            copyright = "2010-2017",
            author = "JetBrains",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "Jetpack Compose",
            copyright = "2021",
            author = "Google Inc.",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "Hilt",
            copyright = "2020",
            author = "Google Inc.",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "Room",
            copyright = "2017",
            author = "Google Inc.",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "DataStore",
            copyright = "2020",
            author = "Google Inc.",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "Navigation Compose",
            copyright = "2021",
            author = "Google Inc.",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "Material 3",
            copyright = "2021",
            author = "Google Inc.",
            licenseType = "Apache License 2.0"
        ),
        License(
            name = "Coil",
            copyright = "2021",
            author = "Colin White",
            licenseType = "Apache License 2.0"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open Source Licenses") },
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
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(licenses) { license ->
                LicenseCard(
                    license = license,
                    onClick = {
                        val licenseUrl = when (license.licenseType) {
                            "Apache License 2.0" -> "https://www.apache.org/licenses/LICENSE-2.0"
                            "MIT License" -> "https://opensource.org/licenses/MIT"
                            else -> null
                        }
                        
                        licenseUrl?.let { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LicenseCard(
    license: License,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = license.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                if (license.copyright.isNotEmpty() && license.author.isNotEmpty()) {
                    Text(
                        text = "© ${license.copyright} ${license.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (license.author.isNotEmpty()) {
                    Text(
                        text = license.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = license.licenseType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
