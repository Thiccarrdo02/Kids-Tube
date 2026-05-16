package com.family.kidstube.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.family.kidstube.data.model.VideoDto
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

    // Smarter Up next: same channel first, then same category, then everything
    // else shuffled. Keeps a YouTube-like feel without ever recommending
    // anything outside the parent-approved feed.
    val upNext: List<VideoDto> = remember(state.videos, current) {
        if (current == null) return@remember emptyList()
        val others = state.videos.filter { it.id != current.id }
        val sameChannel = others.filter { it.channelId == current.channelId && it.channelId != null }
        val sameCategory = others.filter {
            it !in sameChannel && it.categoryId == current.categoryId && it.categoryId != null
        }
        val rest = others
            .filter { it !in sameChannel && it !in sameCategory }
            .shuffled()
        sameChannel + sameCategory + rest
    }

    LaunchedEffect(videoId) { vm.recordWatch(videoId) }

    val activity = LocalContext.current as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var playerError by remember(videoId) { mutableStateOf<PlayerConstants.PlayerError?>(null) }
    var playerEnded by remember(videoId) { mutableStateOf(false) }
    // Track play/paused/buffering so we can show our own big play button
    // when the user has paused.
    var playerState by remember(videoId) { mutableStateOf(PlayerConstants.PlayerState.UNKNOWN) }
    // We hold the actual YouTubePlayer instance so the tap-to-pause overlay
    // can call play()/pause() directly.
    var ytPlayer by remember { mutableStateOf<YouTubePlayer?>(null) }

    // Immersive system bars while in landscape. Two separate effects:
    //   - keyed on isLandscape: just toggles the immersive flags
    //   - keyed on Unit: only fires on screen tear-down, restores the
    //     orientation. (Combining them caused the orientation to reset
    //     to UNSPECIFIED on every rotation, which fought our fullscreen
    //     toggle.)
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
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
        onDispose { /* no orientation reset here -- see the Unit effect below */ }
    }

    DisposableEffect(Unit) {
        onDispose {
            val window = activity?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(
            Modifier
                .fillMaxWidth()
                .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier.aspectRatio(16f / 9f))
                .background(Color.Black)
        ) {
            YouTubePlayer(
                videoId = videoId,
                onPlayerReady = { ytPlayer = it },
                onError = { playerError = it },
                onStateChange = { st ->
                    playerState = st
                    when (st) {
                        PlayerConstants.PlayerState.ENDED -> playerEnded = true
                        PlayerConstants.PlayerState.PLAYING,
                        PlayerConstants.PlayerState.BUFFERING -> playerEnded = false
                        else -> {}
                    }
                },
            )

            // Tap-to-pause overlay. Uses detectTapGestures + zIndex(1f) so
            // it intercepts taps above the YouTube WebView but below our
            // higher-zIndex fullscreen button. padding(end/bottom) carves
            // out the fullscreen button's region so taps there don't
            // also toggle pause.
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(end = 64.dp, bottom = 56.dp)
                    .zIndex(1f)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            val p = ytPlayer
                            if (p != null) {
                                if (playerState == PlayerConstants.PlayerState.PLAYING) p.pause()
                                else p.play()
                            }
                        }
                    }
            )

            // Big center play icon when paused / unstarted, so kids know
            // to tap. Hidden during PLAYING / BUFFERING.
            val showCenterPlay = playerError == null && !playerEnded && when (playerState) {
                PlayerConstants.PlayerState.PAUSED,
                PlayerConstants.PlayerState.UNSTARTED,
                PlayerConstants.PlayerState.UNKNOWN,
                PlayerConstants.PlayerState.VIDEO_CUED -> true
                else -> false
            }
            if (showCenterPlay) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }

            // Fullscreen toggle. pointerInput + awaitEachGesture is used
            // here (instead of .clickable) because the YouTube WebView under
            // us would otherwise sometimes intercept the tap; awaitEachGesture
            // consumes the DOWN event explicitly so it never reaches the
            // WebView's onTouchEvent.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .zIndex(10f)
                    .padding(end = 8.dp, bottom = 8.dp)
                    .size(width = 48.dp, height = 40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC000000))
                    .pointerInput(isLandscape) {
                        detectTapGestures {
                            activity?.requestedOrientation = if (isLandscape) {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isLandscape) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                    contentDescription = "Toggle fullscreen",
                    tint = Color.White,
                )
            }

            if (!isLandscape) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
            }

            // Loading screen covers YouTube's poster/preview screen (which
            // briefly shows the YT logo + title before the player kicks in).
            // Hidden as soon as we transition into BUFFERING or PLAYING.
            val showLoading = playerError == null && when (playerState) {
                PlayerConstants.PlayerState.UNKNOWN,
                PlayerConstants.PlayerState.UNSTARTED -> true
                else -> false
            }
            if (showLoading) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = Color(0xFFFF0000),
                        strokeWidth = 3.dp,
                    )
                }
            }

            // End-screen replacement -- covers any YouTube end-card grid with
            // OUR vetted up-next list.
            if (playerEnded && playerError == null) {
                EndScreenOverlay(
                    suggestions = upNext.take(4),
                    onPlayNext = { id -> onOpenVideo(id) },
                    onBack = onBack,
                )
            }

            if (playerError != null) {
                PlayerErrorOverlay(
                    error = playerError!!,
                    nextVideoId = upNext.firstOrNull()?.id,
                    onBack = onBack,
                    onPlayNext = { id -> onOpenVideo(id) },
                )
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
private fun EndScreenOverlay(
    suggestions: List<VideoDto>,
    onPlayNext: (String) -> Unit,
    onBack: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF0000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                "Up next",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(10.dp))
            if (suggestions.isEmpty()) {
                Text(
                    "Nothing else to watch right now.",
                    color = Color(0xFFB3B3B3),
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFFF0000))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text("Back to home", color = Color.White, fontSize = 13.sp)
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions, key = { it.id }) { v ->
                        SuggestionCard(v) { onPlayNext(v.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(video: VideoDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color(0xFF272727))
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            video.title,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
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
        PlayerConstants.PlayerError.REQUEST_MISSING_HTTP_REFERER ->
            "Network blocked" to "The device's browser couldn't send the right headers to YouTube. Restart the app."
        PlayerConstants.PlayerError.UNKNOWN ->
            "Something went wrong" to "Try a different video."
    }

    Box(
        Modifier.fillMaxSize().background(Color(0xFF0F0F0F)),
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
                    ) {
                        Text(
                            "Play next",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubePlayer(
    videoId: String,
    onPlayerReady: (YouTubePlayer) -> Unit,
    onError: (PlayerConstants.PlayerError) -> Unit,
    onStateChange: (PlayerConstants.PlayerState) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current

    // Fully chrome-less player: no controls means no YouTube logo, no
    // "Watch on YouTube" link, no share button -- nothing for a kid to
    // tap that would escape the app. We drive play/pause via our own
    // overlay on top.
    val opts = remember {
        IFramePlayerOptions.Builder(context)
            .controls(0)
            .rel(0)
            .ivLoadPolicy(3)
            .ccLoadPolicy(0)
            .modestBranding(1)
            .fullscreen(0)
            .build()
    }

    var playerView by remember { mutableStateOf<YouTubePlayerView?>(null) }
    val currentOnReady by rememberUpdatedState(onPlayerReady)
    val currentOnError by rememberUpdatedState(onError)
    val currentOnState by rememberUpdatedState(onStateChange)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            YouTubePlayerView(ctx).also { view ->
                view.enableAutomaticInitialization = false
                view.initialize(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        currentOnReady(youTubePlayer)
                        youTubePlayer.loadVideo(videoId, 0f)
                    }
                    override fun onError(
                        youTubePlayer: YouTubePlayer,
                        error: PlayerConstants.PlayerError,
                    ) {
                        currentOnError(error)
                    }
                    override fun onStateChange(
                        youTubePlayer: YouTubePlayer,
                        state: PlayerConstants.PlayerState,
                    ) {
                        currentOnState(state)
                    }
                }, /* handleNetworkEvents = */ true, opts)

                // Lock the WebView down: no long-press menu (which can show
                // "Open in browser") and only YouTube embed URLs may load --
                // taps on any hidden YouTube link are dropped on the floor.
                view.findWebViewRecursive()?.let { wv ->
                    wv.isLongClickable = false
                    wv.setOnLongClickListener { true }
                    wv.webViewClient = AllowOnlyEmbedWebViewClient()
                }

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

// Allowlist for WebView navigation: only the request URLs YouTube actually
// needs to render the embed (iframe doc, player JS, API calls, thumbnail
// CDNs). Any click that tries to take the WebView to the YouTube watch
// page, the YouTube app via intent://, or any other host is dropped.
private class AllowOnlyEmbedWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return !isEmbedAllowed(request.url.toString())
    }

    private fun isEmbedAllowed(url: String): Boolean {
        val allowedPrefixes = listOf(
            "https://www.youtube.com/embed/",
            "https://www.youtube.com/iframe_api",
            "https://www.youtube.com/s/",
            "https://www.youtube.com/youtubei/",
            "https://www.youtube.com/api/",
            "https://www.youtube.com/generate_204",
            "https://www.youtube.com/error_204",
            "https://www.youtube-nocookie.com/",
            "https://i.ytimg.com/",
            "https://yt3.ggpht.com/",
            "https://yt3.googleusercontent.com/",
            "https://www.gstatic.com/",
            "https://fonts.gstatic.com/",
            "https://fonts.googleapis.com/",
            "about:blank",
        )
        return allowedPrefixes.any { url.startsWith(it) }
    }
}

// Walks the YouTubePlayerView's view tree to find the underlying WebView.
private fun View.findWebViewRecursive(): WebView? {
    if (this is WebView) return this
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).findWebViewRecursive()?.let { return it }
        }
    }
    return null
}
