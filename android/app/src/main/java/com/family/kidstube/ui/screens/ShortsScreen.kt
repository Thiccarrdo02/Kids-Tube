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
import com.family.kidstube.ui.FeedViewModel
import com.family.kidstube.ui.components.SkeletonCard
import com.family.kidstube.ui.components.ThinDivider
import com.family.kidstube.ui.components.VideoCard

@Composable
fun ShortsScreen(
    vm: FeedViewModel,
    onOpenVideo: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val shorts = remember(state.videos) { state.videos.filter { it.durationSeconds in 1..60 } }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Shorts")
        }
        ThinDivider()
        when {
            state.loading -> LazyColumn(Modifier.fillMaxSize()) { items(6) { SkeletonCard() } }
            shorts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No Shorts under 60s yet")
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(shorts, key = { it.id }) { v ->
                    VideoCard(v, v.channelTitle) { onOpenVideo(v.id) }
                }
            }
        }
    }
}
