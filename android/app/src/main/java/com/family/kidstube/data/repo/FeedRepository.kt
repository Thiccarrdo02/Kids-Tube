package com.family.kidstube.data.repo

import android.content.Context
import com.family.kidstube.data.api.Network
import com.family.kidstube.data.model.CategoryDto
import com.family.kidstube.data.model.FeedResponse
import com.family.kidstube.data.model.VideoDto
import com.family.kidstube.data.prefs.AppPrefs
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first

class FeedRepository(private val ctx: Context, private val prefs: AppPrefs) {

    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(FeedResponse::class.java)

    data class Feed(
        val categories: List<CategoryDto>,
        val videos: List<VideoDto>,
        val fromCache: Boolean,
    )

    suspend fun loadFeed(forceRefresh: Boolean = false): Feed {
        val cached = prefs.cachedFeed()
        val cacheAgeMs = cached?.let { System.currentTimeMillis() - it.second }
        val cacheFresh = cacheAgeMs != null && cacheAgeMs < TTL_MS

        if (!forceRefresh && cached != null && cacheFresh) {
            adapter.fromJson(cached.first)?.let {
                return Feed(sortCats(it.categories), it.videos, fromCache = true)
            }
        }

        return try {
            val baseUrl = prefs.backendUrl.first()
            val api = Network.feedApi(ctx, baseUrl)
            val resp = api.getFeed()
            prefs.saveFeedCache(adapter.toJson(resp))
            Feed(sortCats(resp.categories), resp.videos, fromCache = false)
        } catch (t: Throwable) {
            // Network or parse failed. If we have any cached payload (even
            // stale), surface it -- better than an angry error screen for
            // a kid. Otherwise rethrow with a friendly message.
            cached?.let { adapter.fromJson(it.first) }?.let {
                return Feed(sortCats(it.categories), it.videos, fromCache = true)
            }
            throw RuntimeException(friendlyMessage(t), t)
        }
    }

    private fun sortCats(cats: List<CategoryDto>) = cats.sortedBy { it.sortOrder }

    companion object {
        private const val TTL_MS = 60L * 60 * 1000 // 1 hour

        // Maps low-level network/parse errors to a single short sentence
        // the parent can act on. Anything unrecognized falls through to
        // the original message so we don't hide useful info.
        fun friendlyMessage(t: Throwable): String {
            val msg = t.message.orEmpty()
            return when {
                msg.contains("malformed JSON", ignoreCase = true) ||
                    msg.contains("Expected BEGIN_OBJECT", ignoreCase = true) ||
                    msg.contains("setLenient", ignoreCase = true) ->
                    "Backend URL isn't returning a KidsTube feed. Open parental settings and check the URL."
                msg.contains("Unable to resolve host", ignoreCase = true) ||
                    msg.contains("UnknownHost", ignoreCase = true) ->
                    "Can't reach the backend. Check the URL and your internet."
                msg.contains("timeout", ignoreCase = true) ->
                    "Backend timed out. Try Force refresh."
                msg.contains("HTTP 401") -> "Wrong admin password."
                msg.contains("HTTP 4") || msg.contains("HTTP 5") ->
                    "Backend error: $msg"
                else -> if (msg.isBlank()) "Network error" else msg
            }
        }
    }
}
