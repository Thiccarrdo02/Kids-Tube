package com.family.kidstube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.family.kidstube.data.model.VideoDto
import com.family.kidstube.ui.FeedViewModel
import com.family.kidstube.ui.components.EmptyState
import com.family.kidstube.ui.components.ThinDivider
import com.family.kidstube.ui.components.VideoCard

@Composable
fun LibraryScreen(
    vm: FeedViewModel,
    onOpenVideo: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val historyIds by vm.history.collectAsState()
    val favoriteIds by vm.favorites.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val byId: Map<String, VideoDto> = remember(state.videos) { state.videos.associateBy { it.id } }
    val orderedHistory = remember(historyIds, byId) { historyIds.mapNotNull { byId[it] } }
    val orderedFavorites = remember(favoriteIds, byId) { favoriteIds.mapNotNull { byId[it] } }
    val currentItems = if (selectedTab == 0) orderedHistory else orderedFavorites

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) { Text("Library") }
        ThinDivider()
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("History") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Favorites") })
        }
        if (currentItems.isEmpty()) {
            EmptyState(
                title = if (selectedTab == 0) "Nothing watched yet" else "No favorites yet",
                body = if (selectedTab == 0) {
                    "Videos you watch will show up here."
                } else {
                    "Tap the heart on a video to save it here."
                },
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(currentItems, key = { it.id }) { v ->
                    VideoCard(v, v.channelTitle) { onOpenVideo(v.id) }
                }
            }
        }
    }
}
