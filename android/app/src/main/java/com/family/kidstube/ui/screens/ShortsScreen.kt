package com.family.kidstube.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.family.kidstube.data.model.VideoDto
import com.family.kidstube.ui.FeedViewModel
import com.family.kidstube.ui.components.EmptyState
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortsScreen(
    vm: FeedViewModel,
    onOpenVideo: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val shorts = remember(state.videos) { state.videos.filter { it.durationSeconds in 1..60 } }

    when {
        state.loading -> Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Loading Shorts...", color = Color.White)
        }
        shorts.isEmpty() -> EmptyState(
            title = "No Shorts yet",
            body = "Shorts will appear here when you add videos that are under 60 seconds.",
        )
        else -> {
            val pagerState = rememberPagerState(pageCount = { shorts.size })
            val scope = rememberCoroutineScope()

            LaunchedEffect(pagerState.currentPage, shorts) {
                shorts.getOrNull(pagerState.currentPage)?.let { vm.recordWatch(it.id) }
            }

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().background(Color.Black),
            ) { page ->
                val video = shorts[page]
                ShortPage(
                    video = video,
                    isCurrent = page == pagerState.currentPage,
                    onEnded = {
                        scope.launch {
                            val next = if (pagerState.currentPage >= shorts.lastIndex) 0 else pagerState.currentPage + 1
                            pagerState.animateScrollToPage(next)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ShortPage(
    video: VideoDto,
    isCurrent: Boolean,
    onEnded: () -> Unit,
) {
    var playerState by remember(video.id) { mutableStateOf(PlayerConstants.PlayerState.UNKNOWN) }
    var playerError by remember(video.id) { mutableStateOf<PlayerConstants.PlayerError?>(null) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (isCurrent) {
            YouTubePlayer(
                videoId = video.id,
                onPlayerReady = {},
                onError = { playerError = it },
                onStateChange = { state ->
                    playerState = state
                    if (state == PlayerConstants.PlayerState.ENDED) onEnded()
                },
            )
        } else {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (playerError != null) {
            Box(
                Modifier.fillMaxSize().background(Color(0xE6000000)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text("This Short can't play here", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Swipe for another approved Short.", color = Color(0xFFB3B3B3), fontSize = 13.sp)
                }
            }
        }

        if (playerError == null && playerState == PlayerConstants.PlayerState.ENDED) {
            Box(Modifier.fillMaxSize().background(Color.Black))
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color(0x66000000))
                .padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 18.dp),
        ) {
            Text(
                video.title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                video.channelTitle.orEmpty(),
                color = Color(0xFFE0E0E0),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
