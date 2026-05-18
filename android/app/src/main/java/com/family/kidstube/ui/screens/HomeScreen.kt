package com.family.kidstube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.kidstube.ui.FeedViewModel
import com.family.kidstube.ui.components.*
import com.family.kidstube.ui.theme.BrandRed
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: FeedViewModel,
    onOpenVideo: (String) -> Unit,
    onLogoLongPress5: () -> Unit,
) {
    val state by vm.state.collectAsState()

    var selectedCategoryIdx by remember { mutableIntStateOf(0) }
    var shuffleEnabled by remember { mutableStateOf(false) }
    var shuffleSeed by remember { mutableIntStateOf(Random.nextInt()) }
    val chipLabels = remember(state.categories) {
        listOf("All") + state.categories.map { it.name }
    }
    val selectedCategoryId: String? = remember(selectedCategoryIdx, state.categories) {
        if (selectedCategoryIdx == 0) null else state.categories.getOrNull(selectedCategoryIdx - 1)?.id
    }
    val visibleVideos = remember(state.videos, selectedCategoryId) {
        if (selectedCategoryId == null) state.videos
        else state.videos.filter { it.categoryId == selectedCategoryId }
    }
    val displayVideos = remember(visibleVideos, shuffleEnabled, shuffleSeed) {
        if (shuffleEnabled) visibleVideos.shuffled(Random(shuffleSeed)) else visibleVideos
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandWordmark(onLongPress5 = onLogoLongPress5)
            Spacer(Modifier.weight(1f))
            // Decorative-only: a YouTube-mobile feel without functioning escape
            // hatches (the icons don't navigate anywhere a kid could misuse).
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Cast, contentDescription = null, tint = Color(0xFF0F0F0F))
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = Color(0xFF0F0F0F))
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = Color(0xFF0F0F0F))
            }
            Box(
                Modifier
                    .padding(end = 12.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(BrandRed),
                contentAlignment = Alignment.Center,
            ) { Text("Z", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }
        ThinDivider()
        if (chipLabels.size > 1) {
            CategoryChips(
                categories = chipLabels,
                selectedIndex = selectedCategoryIdx,
                onSelect = { selectedCategoryIdx = it },
            )
            ThinDivider()
        }
        if (visibleVideos.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        shuffleEnabled = !shuffleEnabled
                        if (shuffleEnabled) shuffleSeed = Random.nextInt()
                    },
                ) {
                    Icon(
                        Icons.Outlined.Shuffle,
                        contentDescription = "Shuffle feed",
                        tint = if (shuffleEnabled) BrandRed else Color(0xFF0F0F0F),
                    )
                }
                Text(
                    if (shuffleEnabled) "Shuffle on" else "Newest first",
                    color = Color(0xFF606060),
                    fontSize = 13.sp,
                )
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = {
                        visibleVideos.randomOrNull()?.let { onOpenVideo(it.id) }
                    },
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = "Play random video")
                }
            }
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
                    EmptyState(
                        title = "Can't load videos",
                        body = state.error,
                        actionLabel = "Try again",
                        onAction = { vm.refresh() },
                    )
                }
                visibleVideos.isEmpty() -> {
                    EmptyState(
                        title = "No videos yet",
                        body = "Tap the YouTube logo 5 times to open parental settings and add a video.",
                    )
                }
                else -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(displayVideos, key = { it.id }) { v ->
                            VideoCard(v, v.channelTitle) { onOpenVideo(v.id) }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}
