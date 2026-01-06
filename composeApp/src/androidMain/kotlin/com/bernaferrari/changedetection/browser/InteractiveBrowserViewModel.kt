package com.bernaferrari.changedetection.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.extensions.normalizeUrl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.ref.WeakReference



class InteractiveBrowserViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(InteractiveBrowserUiState())
    val uiState: StateFlow<InteractiveBrowserUiState> = _uiState.asStateFlow()
    
    // Add WebView reference for JavaScript communication - using WeakReference to prevent memory leaks
    private var webViewRef: WeakReference<android.webkit.WebView>? = null
    
    fun setWebView(webView: android.webkit.WebView) {
        this.webViewRef = WeakReference(webView)
    }
    
    fun setInitialUrl(url: String) {
        val normalized = url.normalizeUrl() ?: url
        _uiState.value = _uiState.value.copy(
            currentUrl = normalized,
            hasSessionData = normalized.isNotBlank()
        )
    }
    
    fun applyZappedElements(zappedElements: List<ZappedElement>) {
        if (zappedElements.isNotEmpty()) {
            val selectors = zappedElements.map { "\"${it.selector}\"" }.joinToString(",")
            webViewRef?.get()?.post {
                webViewRef?.get()?.evaluateJavascript(
                    "window.applyZappedElements([$selectors]);",
                    null
                )
            }
        }
    }
    
    fun setCurrentUrl(url: String) {
        val normalized = url.normalizeUrl() ?: url
        _uiState.value = _uiState.value.copy(currentUrl = normalized)
    }
    
    fun setPageTitle(title: String) {
        _uiState.value = _uiState.value.copy(pageTitle = title)
    }
    
    fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
    }
    
    fun setCapturedCookies(cookies: String) {
        _uiState.value = _uiState.value.copy(
            capturedCookies = cookies,
            hasSessionData = cookies.isNotBlank() || _uiState.value.currentUrl.isNotBlank()
        )
    }
    
    fun setUserAgent(userAgent: String) {
        _uiState.value = _uiState.value.copy(userAgent = userAgent)
    }
    
    fun hydrateFromSession(session: BrowserSessionData) {
        val normalizedUrl = session.url.ifBlank { _uiState.value.currentUrl }
        val hasMeaningfulSessionData = session.selectedElements.isNotEmpty() ||
            session.zappedElements.isNotEmpty() ||
            session.cookies.isNotBlank() ||
            normalizedUrl.isNotBlank()

        _uiState.value = _uiState.value.copy(
            currentUrl = normalizedUrl,
            pageTitle = session.title.ifBlank { _uiState.value.pageTitle },
            capturedCookies = session.cookies,
            userAgent = session.userAgent,
            selectedElements = session.selectedElements,
            zappedElements = session.zappedElements,
            hasSessionData = hasMeaningfulSessionData
        )

        applyZappedElements(session.zappedElements)
    }

    
    fun toggleLoginCapture() {
        val currentMode = _uiState.value.interactionMode
        val newMode = if (currentMode == InteractionMode.LOGIN_CAPTURE) {
            InteractionMode.NORMAL
        } else {
            InteractionMode.LOGIN_CAPTURE
        }
        
        _uiState.value = _uiState.value.copy(
            interactionMode = newMode
        )
        
        // Set JavaScript interaction mode
        setJavaScriptMode(newMode)
    }
    
    fun toggleElementSelection() {
        val currentMode = _uiState.value.interactionMode
        val newMode = if (currentMode == InteractionMode.ELEMENT_SELECT) {
            InteractionMode.NORMAL
        } else {
            InteractionMode.ELEMENT_SELECT
        }
        
        _uiState.value = _uiState.value.copy(
            interactionMode = newMode
        )
        
        setJavaScriptMode(newMode)
    }
    
    fun toggleElementZapping() {
        val currentMode = _uiState.value.interactionMode
        val newMode = if (currentMode == InteractionMode.ELEMENT_ZAP) {
            InteractionMode.NORMAL
        } else {
            InteractionMode.ELEMENT_ZAP
        }
        
        _uiState.value = _uiState.value.copy(
            interactionMode = newMode
        )
        
        setJavaScriptMode(newMode)
    }
    
    fun reapplyInteractionMode() {
        setJavaScriptMode(_uiState.value.interactionMode)
    }

    private fun setJavaScriptMode(mode: InteractionMode) {
        val jsMode = when (mode) {
            InteractionMode.LOGIN_CAPTURE -> "login"
            InteractionMode.ELEMENT_SELECT -> "select"
            InteractionMode.ELEMENT_ZAP -> "zap"
            InteractionMode.NORMAL -> "normal"
        }
        
        // Actually execute JavaScript to set the interaction mode
        webViewRef?.get()?.post {
            webViewRef?.get()?.evaluateJavascript(
                """
                (function() {
                    if (typeof window.setInteractionMode === 'function') {
                        window.setInteractionMode('$jsMode');
                    } else {
                        window.interactionMode = '$jsMode';
                    }
                    console.log('Interaction mode set to: $jsMode');
                })();
                """.trimIndent(),
                null
            )
        }
    }
    
    fun handleElementInteraction(selector: String, elementInfoJson: String) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val mode = currentState.interactionMode
                if (mode != InteractionMode.ELEMENT_SELECT && mode != InteractionMode.ELEMENT_ZAP) {
                    return@launch
                }

                val elementJson = JSONObject(elementInfoJson)
                val parentChain = parseParentChain(elementJson)
                val description = extractElementDescription(elementJson)

                if (parentChain.isEmpty() || parentChain.size == 1) {
                    val target = parentChain.firstOrNull()
                    val finalSelector = target?.selector ?: selector
                    val finalDescription = target?.displayName ?: description
                    commitElementChange(mode, finalSelector, finalDescription)
                } else {
                    _uiState.value = currentState.copy(
                        parentChain = parentChain,
                        selectedParentIndex = 0,
                        showParentSelector = true,
                        pendingElementSelector = selector,
                        pendingElementDescription = description,
                        pendingInteractionMode = mode
                    )
                    previewParentAtIndex(0)
                }
            } catch (e: Exception) {
                // Ignore malformed payloads for now
            }
        }
    }
    
    fun handleLoginDetection(formDataJson: String) {
        viewModelScope.launch {
            try {
                // Parse form data and capture login information
                _uiState.value = _uiState.value.copy(
                    hasLoginData = true,
                    hasSessionData = true
                )
            } catch (e: Exception) {
                // Handle parsing error
            }
        }
    }
    
    private fun commitElementChange(mode: InteractionMode, selector: String, description: String) {
        val currentState = _uiState.value
        when (mode) {
            InteractionMode.ELEMENT_SELECT -> {
                val updated = currentState.selectedElements.toMutableList()
                val existing = updated.indexOfFirst { it.selector == selector }
                if (existing >= 0) {
                    updated.removeAt(existing)
                } else {
                    updated.add(
                        SelectedElement(
                            selector = selector,
                            description = description,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                _uiState.value = currentState.copy(
                    selectedElements = updated,
                    hasSessionData = true,
                    pendingInteractionMode = null,
                    pendingElementSelector = null,
                    pendingElementDescription = ""
                )
            }

            InteractionMode.ELEMENT_ZAP -> {
                val updated = currentState.zappedElements.toMutableList()
                val existing = updated.indexOfFirst { it.selector == selector }
                if (existing >= 0) {
                    updated.removeAt(existing)
                    restoreElement(selector)
                } else {
                    updated.add(
                        ZappedElement(
                            selector = selector,
                            description = description,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    hideElement(selector)
                }
                _uiState.value = currentState.copy(
                    zappedElements = updated,
                    hasSessionData = true,
                    pendingInteractionMode = null,
                    pendingElementSelector = null,
                    pendingElementDescription = ""
                )
            }

            else -> Unit
        }
    }

    private fun extractElementDescription(elementJson: JSONObject): String {
        return try {
            val text = elementJson.optString("text").trim()
            val tagName = elementJson.optString("tagName").ifBlank { "element" }
            val className = elementJson.optString("className")

            when {
                text.isNotBlank() && text.length > 3 -> text.take(60)
                className.isNotBlank() -> "$tagName.$className"
                else -> tagName.lowercase()
            }
        } catch (_: Exception) {
            "element"
        }
    }

    private fun hideElement(selector: String) {
        webViewRef?.get()?.post {
            webViewRef?.get()?.evaluateJavascript(
                """
                try {
                    const element = document.querySelector('$selector');
                    if (element) {
                        if (typeof window.removeZapHighlight === 'function') {
                            window.removeZapHighlight(element);
                        }
                        if (typeof window.storeOriginalDisplay === 'function') {
                            window.storeOriginalDisplay(element);
                        }
                        element.style.display = 'none';
                        element.setAttribute('data-zapped', 'true');
                        if (window.zappedElements) {
                            window.zappedElements.add('$selector');
                        }
                    }
                } catch (e) {
                    console.error('Failed to hide element', e);
                }
                """.trimIndent(),
                null
            )
        }
    }

    private fun restoreElement(selector: String) {
        webViewRef?.get()?.post {
            webViewRef?.get()?.evaluateJavascript(
                """
                try {
                    const element = document.querySelector('$selector');
                    if (element) {
                        if (typeof window.removeZapHighlight === 'function') {
                            window.removeZapHighlight(element);
                        }
                    }
                    if (window.zappedElements) {
                        window.zappedElements.delete('$selector');
                    }
                } catch (e) {
                    console.error('Failed to restore element', e);
                }
                """.trimIndent(),
                null
            )
        }
    }

    private fun parseParentChain(elementJson: JSONObject): List<ParentElement> {
        val chain = elementJson.optJSONArray("parentChain") ?: return emptyList()
        val items = mutableListOf<ParentElement>()
        for (i in 0 until chain.length()) {
            val parent = chain.optJSONObject(i) ?: continue
            items.add(
                ParentElement(
                    selector = parent.optString("selector"),
                    displayName = parent.optString("displayName", parent.optString("selector")),
                    tagName = parent.optString("tagName"),
                    className = parent.optString("className"),
                    id = parent.optString("id"),
                    index = parent.optInt("index", i)
                )
            )
        }
        return items
    }

    private fun previewParentAtIndex(index: Int) {
        webViewRef?.get()?.post {
            webViewRef?.get()?.evaluateJavascript(
                "window.previewParentAtIndex($index);",
                null
            )
        }
    }

    private fun clearPreviewHighlight() {
        webViewRef?.get()?.post {
            webViewRef?.get()?.evaluateJavascript(
                "window.clearPreviewHighlight();",
                null
            )
        }
    }

    fun selectParentAtIndex(index: Int) {
        val currentState = _uiState.value
        if (index >= 0 && index < currentState.parentChain.size) {
            val parent = currentState.parentChain[index]
            _uiState.value = currentState.copy(
                selectedParentIndex = index,
                pendingElementDescription = parent.displayName
            )
            
            previewParentAtIndex(index)
        }
    }
    
    fun hideParentSelector() {
        _uiState.value = _uiState.value.copy(
            showParentSelector = false,
            parentChain = emptyList(),
            selectedParentIndex = 0,
            pendingElementSelector = null,
            pendingElementDescription = "",
            pendingInteractionMode = null
        )
        clearPreviewHighlight()
    }
    
    fun confirmParentSelection() {
        val currentState = _uiState.value
        if (currentState.parentChain.isNotEmpty() && currentState.selectedParentIndex < currentState.parentChain.size) {
            val selectedParent = currentState.parentChain[currentState.selectedParentIndex]

            val mode = currentState.pendingInteractionMode ?: currentState.interactionMode
            commitElementChange(mode, selectedParent.selector, selectedParent.displayName)
        }
        
        hideParentSelector()
    }

    fun showZapManager() {
        _uiState.value = _uiState.value.copy(
            showZapManager = !_uiState.value.showZapManager
        )
    }
    
    fun clearAllZappedElements() {
        val current = _uiState.value
        _uiState.value = current.copy(
            zappedElements = emptyList(),
            hasSessionData = current.selectedElements.isNotEmpty() || current.capturedCookies.isNotBlank()
        )
        
        // Clear visual zapping from webpage
        webViewRef?.get()?.post {
            webViewRef?.get()?.evaluateJavascript(
                """
                try {
                    if (typeof window.removeZapHighlight === 'function') {
                        document.querySelectorAll('[data-zapped="true"]').forEach(element => {
                            window.removeZapHighlight(element);
                            if (typeof window.restoreOriginalDisplay === 'function') {
                                window.restoreOriginalDisplay(element);
                            } else {
                                element.style.display = '';
                            }
                        });
                    }
                    if (window.zappedElements) {
                        window.zappedElements.clear();
                    }
                    console.log('All zapped elements cleared');
                } catch (e) {
                    console.error('Failed to clear zapped elements:', e);
                }
                """.trimIndent(),
                null
            )
        }
    }
    
    fun removeZappedElement(selector: String) {
        val currentElements = _uiState.value.zappedElements.toMutableList()
        currentElements.removeAll { it.selector == selector }

        val current = _uiState.value
        _uiState.value = current.copy(
            zappedElements = currentElements,
            hasSessionData = current.selectedElements.isNotEmpty() || currentElements.isNotEmpty() || current.capturedCookies.isNotBlank()
        )
        
        // Restore element in webpage
        webViewRef?.get()?.post {
            webViewRef?.get()?.evaluateJavascript(
                """
                try {
                    const element = document.querySelector('$selector');
                    if (element) {
                        if (typeof window.removeZapHighlight === 'function') {
                            window.removeZapHighlight(element);
                        }
                        if (typeof window.restoreOriginalDisplay === 'function') {
                            window.restoreOriginalDisplay(element);
                        } else {
                            element.style.display = '';
                        }
                        if (window.zappedElements) {
                            window.zappedElements.delete('$selector');
                        }
                        console.log('Element restored: $selector');
                    }
                } catch (e) {
                    console.error('Failed to restore element:', e);
                }
                """.trimIndent(),
                null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clear the WebView reference to prevent memory leaks
        webViewRef?.clear()
        webViewRef = null
    }
}

data class InteractiveBrowserUiState(
    val currentUrl: String = "",
    val pageTitle: String = "",
    val isLoading: Boolean = false,
    val interactionMode: InteractionMode = InteractionMode.NORMAL,
    val capturedCookies: String = "",
    val userAgent: String = "",
    val selectedElements: List<SelectedElement> = emptyList(),
    val zappedElements: List<ZappedElement> = emptyList(),
    val hasLoginData: Boolean = false,
    val hasSessionData: Boolean = false,
    val parentChain: List<ParentElement> = emptyList(),
    val selectedParentIndex: Int = 0,
    val showParentSelector: Boolean = false,
    val pendingElementSelector: String? = null,
    val pendingElementDescription: String = "",
    val pendingInteractionMode: InteractionMode? = null,
    val showZapManager: Boolean = false
)


