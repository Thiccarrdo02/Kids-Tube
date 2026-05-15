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
            // Network failed -- fall back to whatever's cached, even if stale.
            cached?.let { adapter.fromJson(it.first) }?.let {
                return Feed(sortCats(it.categories), it.videos, fromCache = true)
            }
            throw t
        }
    }

    private fun sortCats(cats: List<CategoryDto>) = cats.sortedBy { it.sortOrder }

    companion object {
        private const val TTL_MS = 60L * 60 * 1000 // 1 hour
    }
}
