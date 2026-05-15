package com.family.kidstube.data.api

import com.family.kidstube.data.model.AddRequest
import com.family.kidstube.data.model.AddResponse
import com.family.kidstube.data.model.FeedResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface FeedApi {
    @GET("api/public/feed")
    suspend fun getFeed(): FeedResponse

    @POST("api/app/add")
    suspend fun addVideo(@Body req: AddRequest): AddResponse
}
