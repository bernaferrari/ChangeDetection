package com.bernaferrari.changedetection.detailsWebview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.repo.source.WebResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComparisonUiState(
    val isLoading: Boolean = false,
    val comparison: VersionComparison? = null,
    val error: String? = null
)

@HiltViewModel
class ComparisonViewModel @Inject constructor(
    private val comparisonService: ComparisonService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComparisonUiState())
    val uiState: StateFlow<ComparisonUiState> = _uiState.asStateFlow()

    fun loadVersionsForSite(siteId: String) {
        viewModelScope.launch {
            _uiState.value = ComparisonUiState(isLoading = true)

            when (val result = comparisonService.getVersionsForSite(siteId)) {
                is WebResult.Success -> {
                    _uiState.value = ComparisonUiState(comparison = result.data)
                }

                is WebResult.Error -> {
                    _uiState.value = ComparisonUiState(error = result.description)
                }
            }
        }
    }

    fun clearState() {
        _uiState.value = ComparisonUiState()
    }
}
