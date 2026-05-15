package com.family.kidstube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.family.kidstube.ui.FeedViewModel
import com.family.kidstube.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: FeedViewModel,
    onOpenVideo: (String) -> Unit,
    onLogoLongPress5: () -> Unit,
) {
    val state by vm.state.collectAsState()

    var selectedCategoryIdx by remember { mutableIntStateOf(0) }
    val chipLabels = remember(state.categories) {
        listOf("All") + state.categories.map { it.name }
    }
    val selectedCategoryId: String? = remember(selectedCategoryIdx, state.categories) {
        if (selectedCategoryIdx == 0) null else state.categories[selectedCategoryIdx - 1].id
    }
    val visibleVideos = remember(state.videos, selectedCategoryId) {
        if (selectedCategoryId == null) state.videos
        else state.videos.filter { it.categoryId == selectedCategoryId }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        BrandWordmark(onLongPress5 = onLogoLongPress5)
        ThinDivider()
        if (chipLabels.size > 1) {
            CategoryChips(
                categories = chipLabels,
                selectedIndex = selectedCategoryIdx,
                onSelect = { selectedCategoryIdx = it },
            )
            ThinDivider()
        }

        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                state.loading -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(6) { SkeletonCard() }
                    }
                }
                state.error != null && visibleVideos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.error ?: "")
                    }
                }
                visibleVideos.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No videos yet")
                    }
                }
                else -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(visibleVideos, key = { it.id }) { v ->
                            VideoCard(v, v.channelTitle) { onOpenVideo(v.id) }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}
