package com.bernaferrari.changedetection.addedit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** The page name field and the part-size slider own their state while used, and still follow outside updates. */
class AddFlowInputsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pageNameSendsTypingAndAcceptsATitleFromThePage() {
        var title by mutableStateOf("Example")
        val sent = mutableListOf<String>()
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                PageNameField(title, {
                    sent += it
                    title = it
                }, "example.com", enabled = true)
            }
        }
        val field = compose.onNodeWithContentDescription("Name")
        field.performTextReplacement("Price watch")
        compose.runOnIdle { assertEquals("Price watch", title) }

        // The page's own title arrives (or another screen edits it): the field shows it.
        compose.runOnIdle { title = "Studio Display" }
        field.assertTextEquals("Studio Display")

        // Typing continues from there, and the echo of the outside value is not sent back as an edit.
        field.performTextInput(" deals")
        compose.runOnIdle {
            assertEquals("Studio Display deals", title)
            assertEquals(listOf("Price watch", "Studio Display deals"), sent)
        }

        // Clearing shows the placeholder again.
        field.performTextClearance()
        compose.onNodeWithText("example.com").assertIsDisplayed()
        compose.runOnIdle { assertEquals("", title) }
    }

    @Test fun partSliderReportsDragsAndFollowsThePickedElement() {
        var part by mutableStateOf<PickedPart?>(PickedPart("#a", depth = 1, maxDepth = 4))
        val depths = mutableListOf<Int>()
        compose.setContent {
            ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                PickerPanel(part, "", emptyList(), { depth ->
                    depths += depth
                    part = part?.copy(depth = depth)
                }, {}, {}, {}, {}, {})
            }
        }
        val slider = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        slider.assertRangeInfoEquals(ProgressBarRangeInfo(1f, 0f..4f, steps = 3))

        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(3f) }
        compose.runOnIdle { assertEquals(3, depths.last()) }
        slider.assertRangeInfoEquals(ProgressBarRangeInfo(3f, 0f..4f, steps = 3))

        // The page moves the selection (a tap on another element of the same size): the thumb follows.
        compose.runOnIdle { part = part?.copy(depth = 0) }
        slider.assertRangeInfoEquals(ProgressBarRangeInfo(0f, 0f..4f, steps = 3))

        // A different element brings its own range.
        compose.runOnIdle { part = PickedPart("#b", depth = 2, maxDepth = 6) }
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)).assertRangeInfoEquals(ProgressBarRangeInfo(2f, 0f..6f, steps = 5))
    }
}
