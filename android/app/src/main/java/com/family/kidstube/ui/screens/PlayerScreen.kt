package com.family.kidstube.ui.screens

import android.app.Activity
import android.content.res.Configuration
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.family.kidstube.ui.FeedViewModel
import com.family.kidstube.ui.components.ThinDivider
import com.family.kidstube.ui.components.VideoCard
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun PlayerScreen(
    videoId: String,
    vm: FeedViewModel,
    onBack: () -> Unit,
    onOpenVideo: (String) -> Unit,
) {
    val state by vm.state.collectAsState()
    val current = state.videos.firstOrNull { it.id == videoId }
    val upNext = remember(state.videos, current) {
        if (current == null) emptyList()
        else state.videos.filter { it.categoryId == current.categoryId && it.id != current.id }
    }

    LaunchedEffect(videoId) { vm.recordWatch(videoId) }

    val activity = LocalContext.current as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    DisposableEffect(isLandscape) {
        val window = activity?.window
        if (window != null) {
            if (isLandscape) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
        onDispose {
            if (window != null) {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            YouTubePlayer(videoId = videoId)
            if (!isLandscape) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
        }

        if (!isLandscape) {
            current?.let {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                    Text(it.title, style = MaterialTheme.typography.titleMedium, maxLines = 3)
                    Spacer(Modifier.height(6.dp))
                    Text(it.channelTitle.orEmpty(), color = Color(0xFF606060))
                }
                ThinDivider()
                Text("Up next", modifier = Modifier.padding(12.dp))
                LazyColumn(Modifier.fillMaxSize()) {
                    items(upNext, key = { it.id }) { v ->
                        VideoCard(v, v.channelTitle) { onOpenVideo(v.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubePlayer(videoId: String) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Locks down the IFrame player: no related videos at end, no annotations,
    // no "Watch on YouTube" link.
    // https://developers.google.com/youtube/player_parameters
    val opts = remember {
        IFramePlayerOptions.Builder()
            .controls(1)
            .rel(0)
            .ivLoadPolicy(3)
            .ccLoadPolicy(0)
            .build()
    }

    var playerView by remember { mutableStateOf<YouTubePlayerView?>(null) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            YouTubePlayerView(ctx).also { view ->
                view.enableAutomaticInitialization = false
                view.initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.loadVideo(videoId, 0f)
                    }
                }, /* handleNetworkEvents = */ true, opts)
                lifecycle.addObserver(view)
                playerView = view
            }
        },
        update = { /* navigation re-creates the screen per videoId */ },
    )

    DisposableEffect(Unit) {
        onDispose {
            playerView?.let {
                lifecycle.removeObserver(it)
                it.release()
            }
            playerView = null
        }
    }
}
