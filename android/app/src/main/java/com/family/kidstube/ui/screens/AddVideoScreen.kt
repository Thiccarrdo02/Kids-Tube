package com.family.kidstube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.family.kidstube.data.api.Network
import com.family.kidstube.data.model.AddRequest
import com.family.kidstube.data.prefs.AppPrefs
import com.family.kidstube.ui.FeedViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVideoScreen(
    vm: FeedViewModel,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val prefs = remember { AppPrefs(ctx) }
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsState()

    var adminPw by remember { mutableStateOf("") }
    var pwLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        adminPw = prefs.getAdminPassword().orEmpty()
        pwLoaded = true
    }

    var url by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var newCategoryName by remember { mutableStateOf("") }
    var useNewCategory by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var resultMsg by remember { mutableStateOf<String?>(null) }
    var errMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add a video") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = Color.White,
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Paste a YouTube video, playlist, or channel URL. For playlists / channels, every item will be added.",
                color = Color(0xFF606060),
            )

            OutlinedTextField(
                value = adminPw,
                onValueChange = { adminPw = it },
                label = { Text("Admin password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (pwLoaded && adminPw.isNotEmpty()) {
                Text(
                    "Password is saved on this device.",
                    color = Color(0xFF606060),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("YouTube URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Category picker. Tap a chip to assign; pick "+ New" to type a name.
            Text("Category", style = MaterialTheme.typography.titleMedium)
            FlowChips(
                categories = state.categories.map { it.id to it.name },
                selectedId = if (useNewCategory) null else selectedCategoryId,
                onPick = { id ->
                    useNewCategory = false
                    selectedCategoryId = id
                },
                onPickNew = { useNewCategory = true; selectedCategoryId = null },
                showNewSelected = useNewCategory,
            )
            if (useNewCategory) {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("New category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (resultMsg != null) {
                Text(
                    resultMsg!!,
                    color = Color(0xFF1B5E20),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                )
            }
            if (errMsg != null) {
                Text(
                    errMsg!!,
                    color = Color(0xFFC62828),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                )
            }

            val canSubmit = !busy &&
                adminPw.isNotBlank() &&
                url.isNotBlank() &&
                (selectedCategoryId != null || (useNewCategory && newCategoryName.isNotBlank()))

            Button(
                enabled = canSubmit,
                onClick = {
                    scope.launch {
                        busy = true; resultMsg = null; errMsg = null
                        try {
                            prefs.setAdminPassword(adminPw)
                            val baseUrl = prefs.backendUrl.first()
                            val api = Network.feedApi(ctx, baseUrl)
                            val resp = api.addVideo(
                                AddRequest(
                                    password = adminPw,
                                    url = url.trim(),
                                    categoryId = if (useNewCategory) null else selectedCategoryId,
                                    categoryName = if (useNewCategory) newCategoryName.trim() else null,
                                )
                            )
                            if (resp.ok == true) {
                                val n = resp.saved ?: 0
                                resultMsg = "Saved $n video${if (n == 1) "" else "s"}. Refreshing feed…"
                                url = ""
                                newCategoryName = ""
                                vm.refresh()
                            } else {
                                errMsg = resp.error ?: "Save failed"
                            }
                        } catch (t: Throwable) {
                            errMsg = com.family.kidstube.data.repo.FeedRepository.friendlyMessage(t)
                        } finally {
                            busy = false
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End),
            ) { Text(if (busy) "Saving…" else "Add") }
        }
    }
}

@Composable
private fun FlowChips(
    categories: List<Pair<String, String>>,
    selectedId: String?,
    onPick: (String) -> Unit,
    onPickNew: () -> Unit,
    showNewSelected: Boolean,
) {
    // Simple wrap-row built from FlowRow polyfill -- material3 has FlowRow in
    // 1.4+, but to stay safe with our BOM we use a manual Row with wrap.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val perRow = 3
        val all: List<Triple<String?, String, Boolean>> = categories.map { (id, name) ->
            Triple(id, name, id == selectedId)
        } + listOf(Triple<String?, String, Boolean>(null, "+ New", showNewSelected))
        all.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (id, name, selected) ->
                    val bg = if (selected) Color(0xFF0F0F0F) else Color(0xFFF2F2F2)
                    val fg = if (selected) Color.White else Color(0xFF0F0F0F)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(bg)
                            .clickable { if (id == null) onPickNew() else onPick(id) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text(name, color = fg) }
                }
            }
        }
    }
}
