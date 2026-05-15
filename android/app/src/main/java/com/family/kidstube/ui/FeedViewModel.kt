package com.family.kidstube.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.family.kidstube.data.model.CategoryDto
import com.family.kidstube.data.model.VideoDto
import com.family.kidstube.data.prefs.AppPrefs
import com.family.kidstube.data.repo.FeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val videos: List<VideoDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
)

class FeedViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = AppPrefs(app)
    private val repo = FeedRepository(app, prefs)

    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    // List of recently watched video ids, newest first. Surfaced as state so
    // the Library tab updates without manual reloads.
    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    init {
        load(forceRefresh = false)
        viewModelScope.launch { _history.value = prefs.watchHistory() }
    }

    fun refresh() = load(forceRefresh = true)

    private fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = !forceRefresh, refreshing = forceRefresh, error = null)
            try {
                val feed = repo.loadFeed(forceRefresh)
                _state.value = FeedUiState(
                    loading = false,
                    refreshing = false,
                    videos = feed.videos,
                    categories = feed.categories,
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    loading = false,
                    refreshing = false,
                    error = t.message ?: "Failed to load",
                )
            }
        }
    }

    fun recordWatch(videoId: String) {
        viewModelScope.launch {
            prefs.pushHistory(videoId)
            _history.value = prefs.watchHistory()
        }
    }
}
