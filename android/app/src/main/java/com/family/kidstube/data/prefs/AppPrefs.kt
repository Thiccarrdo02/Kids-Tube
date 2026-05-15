package com.family.kidstube.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.family.kidstube.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore by preferencesDataStore(name = "kidstube_prefs")

class AppPrefs(private val ctx: Context) {

    private val KEY_BACKEND = stringPreferencesKey("backend_url")
    private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
    private val KEY_HISTORY = stringPreferencesKey("watch_history")
    private val KEY_CACHED_FEED = stringPreferencesKey("cached_feed_json")
    private val KEY_CACHED_AT = stringPreferencesKey("cached_feed_at")

    val backendUrl: Flow<String> = ctx.dataStore.data.map {
        it[KEY_BACKEND] ?: BuildConfig.BACKEND_URL
    }

    suspend fun setBackendUrl(url: String) {
        ctx.dataStore.edit { it[KEY_BACKEND] = url.trim().trimEnd('/') }
    }

    // PIN is stored as a SHA-256 hash. Not "encrypted" in the strong sense,
    // but the plaintext is never persisted -- good enough to keep a kid out.
    suspend fun isPinSet(): Boolean =
        ctx.dataStore.data.first()[KEY_PIN_HASH]?.isNotEmpty() == true

    suspend fun setPin(pin: String) {
        ctx.dataStore.edit { it[KEY_PIN_HASH] = sha256(pin) }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = ctx.dataStore.data.first()[KEY_PIN_HASH] ?: return false
        return stored == sha256(pin)
    }

    suspend fun cachedFeed(): Pair<String, Long>? {
        val p = ctx.dataStore.data.first()
        val json = p[KEY_CACHED_FEED] ?: return null
        val at = p[KEY_CACHED_AT]?.toLongOrNull() ?: 0L
        return json to at
    }

    suspend fun saveFeedCache(json: String) {
        ctx.dataStore.edit {
            it[KEY_CACHED_FEED] = json
            it[KEY_CACHED_AT] = System.currentTimeMillis().toString()
        }
    }

    suspend fun clearFeedCache() {
        ctx.dataStore.edit { it.remove(KEY_CACHED_FEED); it.remove(KEY_CACHED_AT) }
    }

    suspend fun watchHistory(): List<String> {
        val raw = ctx.dataStore.data.first()[KEY_HISTORY] ?: return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    suspend fun pushHistory(videoId: String) {
        ctx.dataStore.edit { p ->
            val current = (p[KEY_HISTORY] ?: "").split(",").filter { it.isNotBlank() && it != videoId }
            val next = (listOf(videoId) + current).take(50)
            p[KEY_HISTORY] = next.joinToString(",")
        }
    }

    private fun sha256(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
