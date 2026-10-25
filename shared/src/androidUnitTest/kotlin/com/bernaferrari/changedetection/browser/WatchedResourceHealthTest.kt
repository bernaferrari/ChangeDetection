package com.bernaferrari.changedetection.browser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
class WatchedResourceHealthTest {
 @Test fun failedWatchedDependencyRevokesAlreadyReadyCapture()=runTest{val h=PageCaptureHealth();h.begin("https://example.test");h.dependencies(listOf("https://example.test/background.png"));h.validate();h.resourceError("https://example.test/background.png");val e=runCatching{h.validate()}.exceptionOrNull();assertTrue(e is CaptureNotReadyException);assertEquals(CaptureIssue.RESOURCE_FAILED,(e as CaptureNotReadyException).issue)}
 @Test fun repeatedDependencyCollectionDoesNotInventOverflow()=runTest{val h=PageCaptureHealth();h.begin("https://example.test");val urls=(0 until 300).map{"https://example.test/$it.png"};h.dependencies(urls);h.dependencies(urls);h.resourceError("https://example.test/favicon.ico");h.validate()}
 @Test fun observedNonWatchedFailureDoesNotInventAMissingDependency()=runTest{val h=PageCaptureHealth();h.begin("https://example.test");h.dependencies(listOf("https://example.test/background.png"));h.resourceError("https://example.test/favicon.ico");h.validate()}
 @Test fun cachedPreviewDependenciesSurviveAnotherSelectionAndRecoveryNeedsNewDocument()=runTest{val h=PageCaptureHealth();h.begin("https://example.test");h.dependencies(listOf("https://example.test/a.png"));h.dependencies(listOf("https://example.test/b.png"));h.resourceError("https://example.test/a.png");assertTrue(runCatching{h.validate()}.isFailure);h.begin("https://example.test");h.validate()}
}
