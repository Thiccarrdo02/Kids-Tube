package com.family.kidstube.data.api

import com.family.kidstube.data.model.FeedResponse
import retrofit2.http.GET

interface FeedApi {
    @GET("api/public/feed")
    suspend fun getFeed(): FeedResponse
}
