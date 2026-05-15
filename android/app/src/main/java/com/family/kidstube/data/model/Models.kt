package com.family.kidstube.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class FeedResponse(
    val version: Int,
    val generatedAt: String?,
    val categories: List<CategoryDto>,
    val videos: List<VideoDto>,
)

@JsonClass(generateAdapter = false)
data class CategoryDto(
    val id: String,
    val name: String,
    val sortOrder: Int = 0,
)

@JsonClass(generateAdapter = false)
data class AddRequest(
    val password: String,
    val url: String,
    val categoryId: String? = null,
    val categoryName: String? = null,
)

@JsonClass(generateAdapter = false)
data class AddResponse(
    val ok: Boolean? = null,
    val saved: Int? = null,
    val error: String? = null,
)

@JsonClass(generateAdapter = false)
data class VideoDto(
    val id: String,
    val title: String,
    val channelTitle: String?,
    val channelId: String?,
    val thumbnailUrl: String?,
    val durationSeconds: Int = 0,
    val publishedAt: String?,
    val categoryId: String?,
    val addedAt: String?,
)
