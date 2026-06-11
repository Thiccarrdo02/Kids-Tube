package com.family.kidstube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.family.kidstube.data.model.VideoDto
import com.family.kidstube.ui.FeedViewModel
import com.family.kidstube.ui.theme.BrandRed
import kotlin.random.Random

@Composable
fun TvHomeScreen(
    vm: FeedViewModel,
    onOpenVideo: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var shuffleSeed by remember { mutableIntStateOf(Random.nextInt()) }

    val visibleVideos = remember(state.videos, selectedCategoryId, shuffleSeed) {
        val filtered = when (selectedCategoryId) {
            null -> state.videos
            "shorts" -> state.videos.filter { it.durationSeconds in 1..60 }
            else -> state.videos.filter { it.categoryId == selectedCategoryId }
        }
        filtered.shuffled(Random(shuffleSeed))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(horizontal = 40.dp, vertical = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 44.dp, height = 30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(BrandRed),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("YouTube kids by Zawish", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                Text("TV", color = Color(0xFFB3B3B3), fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            TvActionButton("Refresh", Icons.Outlined.Refresh) { vm.refresh() }
            Spacer(Modifier.width(12.dp))
            TvActionButton("Shuffle", Icons.Outlined.Shuffle) { shuffleSeed = Random.nextInt() }
        }

        Spacer(Modifier.height(22.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                TvChip(
                    label = "All",
                    selected = selectedCategoryId == null,
                    onClick = { selectedCategoryId = null },
                )
            }
            item {
                TvChip(
                    label = "Shorts",
                    selected = selectedCategoryId == "shorts",
                    onClick = { selectedCategoryId = "shorts" },
                )
            }
            items(state.categories, key = { it.id }) { category ->
                TvChip(
                    label = category.name,
                    selected = selectedCategoryId == category.id,
                    onClick = { selectedCategoryId = category.id },
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        when {
            state.loading -> TvMessage("Loading videos...")
            state.error != null && visibleVideos.isEmpty() -> TvMessage(state.error ?: "Can't load videos")
            visibleVideos.isEmpty() -> TvMessage("No videos in this section yet")
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 230.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(visibleVideos, key = { it.id }) { video ->
                        TvVideoCard(video = video, onClick = { onOpenVideo(video.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TvActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (focused) Color.White else Color(0xFF272727))
            .border(2.dp, if (focused) BrandRed else Color.Transparent, RoundedCornerShape(50))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (focused) Color.Black else Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (focused) Color.Black else Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TvChip(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> Color.White
        selected -> BrandRed
        else -> Color(0xFF272727)
    }
    val fg = if (focused) Color.Black else Color.White
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(2.dp, if (focused) BrandRed else Color.Transparent, RoundedCornerShape(50))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TvVideoCard(video: VideoDto, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A1A))
            .border(3.dp, if (focused) BrandRed else Color.Transparent, RoundedCornerShape(8.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (video.durationSeconds > 0) {
                Text(
                    text = formatTvDuration(video.durationSeconds),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BrandRed),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (video.channelTitle?.firstOrNull()?.uppercaseChar() ?: 'K').toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    video.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    video.channelTitle.orEmpty(),
                    color = Color(0xFFB3B3B3),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TvMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = 18.sp)
    }
}

private fun formatTvDuration(s: Int): String {
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
