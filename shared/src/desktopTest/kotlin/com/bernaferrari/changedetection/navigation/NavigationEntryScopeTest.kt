package com.bernaferrari.changedetection.navigation

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NavigationEntryScopeTest {
    @get:Rule val compose = createComposeRule()

    private class Draft(val page: String) : ViewModel() {
        var text by mutableStateOf("Saved $page")
        var cleared = false
        override fun onCleared() {
            cleared = true
        }
    }

    @Test fun draftsBelongToTheirRouteAndAreDiscardedOnlyAfterPop() {
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
        val stack = mutableStateListOf("home")
        val models = mutableMapOf<String, Draft>()
        compose.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                NavDisplay(
                    backStack = stack,
                    onBack = { stack.removeLastOrNull() },
                    entryDecorators = rememberAppEntryDecorators(),
                    entryProvider = entryProvider {
                        entry<String> { route ->
                            if (route.startsWith("edit")) {
                                val draft = viewModel { Draft(route) }
                                models[route] = draft
                                Text(draft.text)
                            } else {
                                Text(route)
                            }
                        }
                    },
                )
            }
        }
        compose.runOnIdle { stack.add("edit A") }
        compose.onNodeWithText("Saved edit A").assertIsDisplayed()
        val first = models.getValue("edit A")
        compose.runOnIdle {
            first.text = "Unsaved A"
            stack.add("preview")
        }
        compose.onNodeWithText("preview").assertIsDisplayed()
        compose.runOnIdle {
            assertFalse(first.cleared)
            stack.removeLastOrNull()
        }
        compose.onNodeWithText("Unsaved A").assertIsDisplayed()
        compose.runOnIdle {
            assertSame(first, models["edit A"])
            stack.add("edit B")
        }
        compose.onNodeWithText("Saved edit B").assertIsDisplayed()
        val second = models.getValue("edit B")
        assertNotSame(first, second)
        compose.runOnIdle { stack.removeLastOrNull() }
        compose.onNodeWithText("Unsaved A").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(second.cleared)
            stack.removeLastOrNull()
        }
        compose.onNodeWithText("home").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(first.cleared)
            stack.add("edit A")
        }
        compose.onNodeWithText("Saved edit A").assertIsDisplayed()
        compose.runOnIdle {
            assertNotSame(first, models["edit A"])
            stack.clear()
            stack.add("home")
        }
        compose.waitForIdle()
        compose.runOnIdle { owner.viewModelStore.clear() }
    }
}
