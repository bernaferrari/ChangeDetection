@file:OptIn(ExperimentalTestApi::class)

package com.bernaferrari.changedetection.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.bernaferrari.changedetection.ChangeDetectionRootApp
import com.bernaferrari.changedetection.PlatformActions
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.di.ChangeDetectionKoinApp
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.util.ApplicationScope
import io.ktor.client.HttpClient
import java.nio.file.Files
import kotlinx.coroutines.cancel
import org.junit.Assert.*
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.plugin.module.dsl.startKoin

class EmptyHomeScrollTest {
    @Test
    fun emptyHomeScrollRespondsToTheFirstSwipeAndReturnsToTheTop() {
        val directory = Files.createTempDirectory("empty-home-scroll").toFile()
        val previousDirectory = System.getProperty("changedetection.dataDir")
        System.setProperty("changedetection.dataDir", directory.path)
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
        val lifecycleOwner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        val application = startKoin<ChangeDetectionKoinApp> { }
        val db = application.koin.get<ChangeDatabase>()
        var shortWindow by mutableStateOf(false)
        try {
            runDesktopComposeUiTest(width = 360, height = 780) {
                setContent {
                    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, LocalLifecycleOwner provides lifecycleOwner) {
                        Box(Modifier.size(360.dp, if (shortWindow) 520.dp else 780.dp)) {
                            ChangeDetectionRootApp(PlatformActions(appVersion = "Scroll test"))
                        }
                    }
                }
                waitUntil(timeoutMillis = 10_000) { onAllNodesWithText("Watch a page").fetchSemanticsNodes().isNotEmpty() }
                val content = onNode(hasScrollAction())
                fun position() = content.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange].value()
                waitForIdle()
                val button = onNode(hasText("Watch a page") and hasClickAction()).getUnclippedBoundsInRoot()
                val viewport = content.getUnclippedBoundsInRoot()
                assertTrue("Watch a page must fit fully in a normal phone viewport: button=$button, viewport=$viewport", button.bottom + 8.dp <= viewport.bottom)
                runOnIdle { shortWindow = true }
                waitForIdle()
                val before = position()
                content.performTouchInput { swipe(center, center.copy(y = center.y - 40f), durationMillis = 500) }
                waitForIdle()
                assertTrue("The first short drag must scroll empty Home, rather than move an invisible app bar", position() > before + 5f)
                content.performTouchInput { swipeUp() }
                onNodeWithText("Watch a page").assertIsDisplayed()
                repeat(3) { content.performTouchInput { swipeDown() } }
                waitForIdle()
                assertEquals("Returning to the top must not leave a hidden scroll offset", 0f, position(), 1f)
                runOnIdle { owner.viewModelStore.clear() }
            }
        } finally {
            owner.viewModelStore.clear()
            application.koin.get<MonitoringManager>().cancelWork()
            application.koin.get<ApplicationScope>().scope.cancel()
            application.koin.get<HttpClient>().close()
            db.close()
            stopKoin()
            if (previousDirectory == null) System.clearProperty("changedetection.dataDir") else System.setProperty("changedetection.dataDir", previousDirectory)
            directory.deleteRecursively()
        }
    }
}
