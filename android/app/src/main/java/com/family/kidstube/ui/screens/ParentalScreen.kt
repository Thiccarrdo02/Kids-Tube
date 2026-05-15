package com.family.kidstube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.family.kidstube.data.prefs.AppPrefs
import com.family.kidstube.ui.FeedViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalScreen(
    vm: FeedViewModel,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val prefs = remember { AppPrefs(ctx) }
    val scope = rememberCoroutineScope()

    var stage by remember { mutableStateOf<Stage>(Stage.Checking) }
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        stage = if (prefs.isPinSet()) Stage.EnterPin else Stage.SetPin
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parental settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        }
    ) { inner ->
        Column(
            Modifier.fillMaxSize().background(Color.White).padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (stage) {
                Stage.Checking -> Text("Loading...")
                Stage.SetPin -> {
                    Text("Set a 4-digit PIN to protect parental settings")
                    OutlinedTextField(
                        value = pin, onValueChange = { if (it.length <= 4) pin = it.filter { c -> c.isDigit() } },
                        label = { Text("New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                    OutlinedTextField(
                        value = pinConfirm, onValueChange = { if (it.length <= 4) pinConfirm = it.filter { c -> c.isDigit() } },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                    msg?.let { Text(it, color = Color.Red) }
                    Button(
                        enabled = pin.length == 4 && pin == pinConfirm,
                        onClick = {
                            scope.launch {
                                prefs.setPin(pin)
                                msg = null
                                stage = Stage.Authed
                            }
                        }
                    ) { Text("Save PIN") }
                }
                Stage.EnterPin -> {
                    Text("Enter 4-digit PIN")
                    OutlinedTextField(
                        value = pin, onValueChange = { if (it.length <= 4) pin = it.filter { c -> c.isDigit() } },
                        label = { Text("PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                    msg?.let { Text(it, color = Color.Red) }
                    Button(
                        enabled = pin.length == 4,
                        onClick = {
                            scope.launch {
                                if (prefs.verifyPin(pin)) { msg = null; pin = ""; stage = Stage.Authed }
                                else msg = "Wrong PIN"
                            }
                        }
                    ) { Text("Unlock") }
                }
                Stage.Authed -> SettingsPanel(
                    prefs = prefs,
                    onClearCache = {
                        scope.launch {
                            prefs.clearFeedCache()
                            ctx.cacheDir.deleteRecursively()
                            vm.refresh()
                        }
                    },
                    onForceRefresh = { vm.refresh() },
                )
            }
        }
    }
}

private enum class Stage { Checking, SetPin, EnterPin, Authed }

@Composable
private fun SettingsPanel(
    prefs: AppPrefs,
    onClearCache: () -> Unit,
    onForceRefresh: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val backendFlow = prefs.backendUrl.collectAsState(initial = "")
    var url by remember(backendFlow.value) { mutableStateOf(backendFlow.value) }
    var saved by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = url, onValueChange = { url = it },
        label = { Text("Backend URL") },
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            scope.launch { prefs.setBackendUrl(url); saved = true }
        }) { Text("Save") }
        OutlinedButton(onClick = onClearCache) { Text("Clear cache") }
        OutlinedButton(onClick = onForceRefresh) { Text("Force refresh") }
    }
    if (saved) Text("Saved. Pull to refresh on Home to reload.")
    Spacer(Modifier.height(8.dp))
    Text("Tip: long-press the KidsTube logo 5 times to reopen this screen.",
        color = Color(0xFF606060))
}
