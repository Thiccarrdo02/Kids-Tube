package com.family.kidstube.ui.screens

import android.app.Activity
import android.content.res.Configuration
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.family.kidstube.ui.FeedViewModel
import com.family.kidstube.ui.components.ThinDivider
import com.family.kidstube.ui.components.VideoCard
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
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

    // Player state lifted up so we can show our own error overlay on top of
    // (and visually replacing) YouTube's "Watch on YouTube" error UI.
    var playerError by remember(videoId) { mutableStateOf<PlayerConstants.PlayerError?>(null) }

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
            YouTubePlayer(
                videoId = videoId,
                onError = { e -> playerError = e },
            )
            // Opaque overlay hides the YouTube "Watch on YouTube" CTA and
            // gives the kid a clean way out + a button to jump to the next
            // suggested video.
            if (playerError != null) {
                PlayerErrorOverlay(
                    error = playerError!!,
                    nextVideoId = upNext.firstOrNull()?.id,
                    onBack = onBack,
                    onPlayNext = { nextId -> onOpenVideo(nextId) },
                )
            }
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
private fun PlayerErrorOverlay(
    error: PlayerConstants.PlayerError,
    nextVideoId: String?,
    onBack: () -> Unit,
    onPlayNext: (String) -> Unit,
) {
    val (title, body) = when (error) {
        PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ->
            "Can't play this video here" to
                "The channel owner only allows this video on YouTube. Pick another one."
        PlayerConstants.PlayerError.VIDEO_NOT_FOUND ->
            "Video unavailable" to "It may have been removed or made private."
        PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST ->
            "Couldn't load video" to "Try a different video."
        PlayerConstants.PlayerError.HTML_5_PLAYER ->
            "Player error" to "Restart the app or try a different video."
        PlayerConstants.PlayerError.UNKNOWN ->
            "Something went wrong" to "Try a different video."
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                color = Color(0xFFB3B3B3),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF272727))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) { Text("Back", color = Color.White, fontSize = 14.sp) }
                if (nextVideoId != null) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFFF0000))
                            .clickable { onPlayNext(nextVideoId) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) { Text("Play next", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun YouTubePlayer(
    videoId: String,
    onError: (PlayerConstants.PlayerError) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Locks down the IFrame player: no related videos at end, no annotations,
    // no "Watch on YouTube" link.
    // https://developers.google.com/youtube/player_parameters
    //
    // origin() is critical: YouTube's IFrame Player API silently refuses to
    // play many videos (including most kid / music content) when the iframe
    // URL doesn't include an origin parameter. The library's WebView base URL
    // is https://www.youtube.com, so we match that here. Without it the player
    // surfaces YouTube's own "Video unavailable - Error 152" UI.
    val opts = remember {
        IFramePlayerOptions.Builder()
            .controls(1)
            .rel(0)
            .ivLoadPolicy(3)
            .ccLoadPolicy(0)
            .origin("https://www.youtube.com")
            .build()
    }

    var playerView by remember { mutableStateOf<YouTubePlayerView?>(null) }
    val currentOnError by rememberUpdatedState(onError)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            YouTubePlayerView(ctx).also { view ->
                view.enableAutomaticInitialization = false
                view.initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.loadVideo(videoId, 0f)
                    }
                    override fun onError(
                        youTubePlayer: YouTubePlayer,
                        error: PlayerConstants.PlayerError,
                    ) {
                        currentOnError(error)
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
