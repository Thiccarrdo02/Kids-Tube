package com.family.kidstube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.family.kidstube.data.model.VideoDto
import com.family.kidstube.ui.FeedViewModel
import com.family.kidstube.ui.components.ThinDivider
import com.family.kidstube.ui.components.VideoCard

@Composable
fun LibraryScreen(
    vm: FeedViewModel,
    onOpenVideo: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val historyIds by vm.history.collectAsState()

    val byId: Map<String, VideoDto> = remember(state.videos) { state.videos.associateBy { it.id } }
    val orderedHistory = remember(historyIds, byId) { historyIds.mapNotNull { byId[it] } }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) { Text("Library") }
        ThinDivider()
        if (orderedHistory.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing watched yet")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(orderedHistory, key = { it.id }) { v ->
                    VideoCard(v, v.channelTitle) { onOpenVideo(v.id) }
                }
            }
        }
    }
}
