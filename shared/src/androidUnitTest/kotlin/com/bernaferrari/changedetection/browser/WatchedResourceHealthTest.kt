package com.bernaferrari.changedetection.browser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
class WatchedResourceHealthTest {
    @Test fun failedWatchedDependencyRevokesAlreadyReadyCapture() = runTest {
        val h = PageCaptureHealth()
        h.begin("https://example.test")
        h.dependencies(mapOf("https://example.test/background.png" to CaptureResourceKind.IMAGE))
        h.validate()
        h.resourceError("https://example.test/background.png", httpStatus = 404)
        h.resourceError("https://example.test/background.png")
        val e = runCatching { h.validate() }.exceptionOrNull()
        assertTrue(e is CaptureNotReadyException)
        assertEquals(CaptureIssue.RESOURCE_FAILED, (e as CaptureNotReadyException).issue)
        assertEquals(listOf(CaptureResourceFailure(CaptureResourceKind.IMAGE, httpStatus = 404)), e.resources)
        h.validate(textOnly = true)
        h.resourceError("https://example.test/background.png", certificateRejected = true)
        val refused = runCatching { h.validate(textOnly = true) }.exceptionOrNull() as CaptureNotReadyException
        assertEquals(listOf(CaptureResourceFailure(CaptureResourceKind.IMAGE, certificateRejected = true)), refused.resources)
    }

    @Test fun repeatedDependencyCollectionDoesNotInventOverflow() = runTest {
        val h = PageCaptureHealth()
        h.begin("https://example.test")
        val urls = (0 until 300).map { "https://example.test/$it.png" }
        h.dependencies(urls.associateWith { CaptureResourceKind.IMAGE })
        h.dependencies(urls.associateWith { CaptureResourceKind.IMAGE })
        h.resourceError("https://example.test/favicon.ico")
        h.validate()
    }

    @Test fun observedNonWatchedFailureDoesNotInventAMissingDependency() = runTest {
        val h = PageCaptureHealth()
        h.begin("https://example.test")
        h.dependencies(mapOf("https://example.test/background.png" to CaptureResourceKind.IMAGE))
        h.resourceError("https://example.test/favicon.ico")
        h.validate()
    }

    @Test fun cachedPreviewDependenciesSurviveAnotherSelectionAndRecoveryNeedsNewDocument() = runTest {
        val h = PageCaptureHealth()
        h.begin("https://example.test")
        h.dependencies(mapOf("https://example.test/a.png" to CaptureResourceKind.IMAGE))
        h.dependencies(mapOf("https://example.test/b.png" to CaptureResourceKind.IMAGE))
        h.resourceError("https://example.test/a.png")
        assertTrue(runCatching { h.validate() }.isFailure)
        h.begin("https://example.test")
        h.validate()
    }

    @Test fun fragmentReferencesShareTheUnderlyingHttpResource() = runTest {
        val h = PageCaptureHealth()
        h.begin("https://example.test")
        h.dependencies(mapOf("https://example.test/sprite.svg?theme=dark#badge" to CaptureResourceKind.IMAGE))
        h.resourceError("https://example.test/sprite.svg?theme=dark")
        assertTrue(runCatching { h.validate() }.exceptionOrNull() is CaptureNotReadyException)
    }

    @Test fun resourceIdentityPreservesQueryParametersAndEncodedHashes() = runTest {
        val h = PageCaptureHealth()
        h.begin("https://example.test")
        h.dependencies(mapOf("https://example.test/a%23b.svg?theme=dark#badge" to CaptureResourceKind.IMAGE))
        h.resourceError("https://example.test/a%23b.svg?theme=light")
        h.validate()
        h.resourceError("https://example.test/a%23b.svg?theme=dark")
        assertTrue(runCatching { h.validate() }.exceptionOrNull() is CaptureNotReadyException)
    }

    @Test fun oversizedFailedUrlCannotSilentlyPassValidation() = runTest {
        val h = PageCaptureHealth()
        h.begin("https://example.test")
        val url = "https://example.test/image?signature=" + "a".repeat(4096)
        h.dependencies(mapOf(url to CaptureResourceKind.IMAGE))
        h.resourceError(url)
        assertTrue(runCatching { h.validate() }.exceptionOrNull() is CaptureNotReadyException)
        h.begin("https://example.test")
        h.validate()
    }

    @Test fun duplicateFailuresAtCapacityDoNotInventOverflow() = runTest {
        val h = PageCaptureHealth()
        h.begin("https://example.test")
        h.dependencies(mapOf("https://example.test/watched.png" to CaptureResourceKind.IMAGE))
        repeat(512) { h.resourceError("https://example.test/optional-$it") }
        h.resourceError("https://example.test/optional-0")
        h.validate()
    }
}
