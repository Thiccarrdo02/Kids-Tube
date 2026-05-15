package com.family.kidstube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.kidstube.data.model.VideoDto
import com.family.kidstube.ui.FeedViewModel
import com.family.kidstube.ui.components.EmptyState
import com.family.kidstube.ui.components.ThinDivider
import com.family.kidstube.ui.components.VideoCard
import com.family.kidstube.ui.theme.BrandRed

@Composable
fun SubscriptionsScreen(
    vm: FeedViewModel,
    onOpenVideo: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val grouped: List<Pair<String, List<VideoDto>>> = remember(state.videos) {
        state.videos
            .filter { !it.channelTitle.isNullOrBlank() }
            .groupBy { it.channelTitle!! }
            .toList()
            .sortedBy { it.first.lowercase() }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) { Text("Subscriptions") }
        ThinDivider()
        if (grouped.isEmpty()) {
            EmptyState(
                title = "No channels yet",
                body = "Channels you've added videos from will be grouped here.",
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                grouped.forEach { (channel, vids) ->
                    item(key = "h-$channel") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Box(
                                Modifier.size(28.dp).clip(CircleShape).background(BrandRed),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    channel.first().uppercaseChar().toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(channel, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    items(vids, key = { it.id }) { v -> VideoCard(v, channel) { onOpenVideo(v.id) } }
                }
            }
        }
    }
}
