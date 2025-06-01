package com.bernaferrari.changedetection.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CardState(
    val site: Site,
    val lastSnap: Snap?,
    val syncStatus: SyncStatus = SyncStatus.OK
)

class MainCardViewModel : ViewModel() {
    
    private val _cardStates = MutableStateFlow<Map<String, CardState>>(emptyMap())
    val cardStates: StateFlow<Map<String, CardState>> = _cardStates.asStateFlow()
    
    fun updateSite(site: Site, lastSnap: Snap? = null) {
        val currentState = _cardStates.value[site.id] ?: CardState(site, lastSnap)
        val newStatus = when (site.isSuccessful) {
            true -> SyncStatus.OK
            false -> SyncStatus.ERROR
        }
        
        _cardStates.value = _cardStates.value.toMutableMap().apply {
            put(site.id, currentState.copy(site = site, lastSnap = lastSnap ?: currentState.lastSnap, syncStatus = newStatus))
        }
    }
    
    fun startSyncing(siteId: String) {
        val currentState = _cardStates.value[siteId] ?: return
        _cardStates.value = _cardStates.value.toMutableMap().apply {
            put(siteId, currentState.copy(syncStatus = SyncStatus.LOADING))
        }
    }
    
    fun reloadSite(siteId: String, onReload: suspend (String) -> Unit) {
        viewModelScope.launch {
            startSyncing(siteId)
            try {
                onReload(siteId)
            } catch (e: Exception) {
                // Handle error - update state to error
                val currentState = _cardStates.value[siteId] ?: return@launch
                _cardStates.value = _cardStates.value.toMutableMap().apply {
                    put(siteId, currentState.copy(syncStatus = SyncStatus.ERROR))
                }
            }
        }
    }
}
