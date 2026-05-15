package com.family.kidstube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.family.kidstube.data.model.VideoDto
import com.family.kidstube.ui.theme.BrandRed
import com.family.kidstube.ui.theme.Divider
import com.family.kidstube.ui.theme.SubtleGray

@Composable
fun BrandWordmark(onLongPress5: () -> Unit) {
    var taps by remember { mutableStateOf(0) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                val now = System.currentTimeMillis()
                taps = if (now - lastTapAt > 1200) 1 else taps + 1
                lastTapAt = now
                if (taps >= 5) { taps = 0; onLongPress5() }
            }
    ) {
        Box(
            Modifier
                .size(width = 30.dp, height = 22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(BrandRed),
            contentAlignment = Alignment.Center,
        ) {
            // White play triangle
            Box(Modifier.size(10.dp)) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val w = size.width; val h = size.height
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, 0f); lineTo(w, h / 2f); lineTo(0f, h); close()
                    }
                    drawPath(path, Color.White)
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        Text("KidsTube", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun CategoryChips(
    categories: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories.size) { i ->
            val selected = i == selectedIndex
            val bg = if (selected) Color(0xFF0F0F0F) else Color(0xFFF2F2F2)
            val fg = if (selected) Color.White else Color(0xFF0F0F0F)
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) { Text(categories[i], color = fg, fontSize = 13.sp) }
        }
    }
}

@Composable
fun VideoCard(video: VideoDto, channelName: String?, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(bottom = 12.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFFEDEDED))) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (video.durationSeconds > 0) {
                Text(
                    text = formatDuration(video.durationSeconds),
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(BrandRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (video.channelTitle?.firstOrNull()?.uppercaseChar() ?: 'K').toString(),
                    color = Color.White, fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    video.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    buildString {
                        append(channelName ?: video.channelTitle.orEmpty())
                        video.addedAt?.let { append(" • "); append(relativeFromIso(it)) }
                    },
                    color = SubtleGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun SkeletonCard() {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFFEDEDED)))
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFEDEDED)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth(0.9f).height(14.dp).background(Color(0xFFEDEDED)))
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth(0.5f).height(12.dp).background(Color(0xFFEDEDED)))
            }
        }
    }
}

@Composable
fun ThinDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
}

private fun formatDuration(s: Int): String {
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

private fun relativeFromIso(iso: String): String {
    return try {
        val instant = java.time.Instant.parse(iso)
        val secs = java.time.Duration.between(instant, java.time.Instant.now()).seconds
        when {
            secs < 60 -> "just now"
            secs < 3600 -> "${secs / 60} min ago"
            secs < 86_400 -> "${secs / 3600} hr ago"
            secs < 86_400 * 30 -> "${secs / 86_400} days ago"
            secs < 86_400 * 365 -> "${secs / (86_400 * 30)} mo ago"
            else -> "${secs / (86_400 * 365)} yr ago"
        }
    } catch (_: Throwable) { "" }
}
