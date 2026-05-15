package com.family.kidstube.data.api

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object Network {

    @Volatile private var client: OkHttpClient? = null
    @Volatile private var moshi: Moshi? = null

    private fun moshi(): Moshi =
        moshi ?: synchronized(this) {
            moshi ?: Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .also { moshi = it }
        }

    private fun client(context: Context): OkHttpClient =
        client ?: synchronized(this) {
            client ?: run {
                val cacheDir = File(context.applicationContext.cacheDir, "http").apply { mkdirs() }
                // 25 MB on-disk cache, shared by feed JSON and Coil thumbnails.
                OkHttpClient.Builder()
                    .cache(Cache(cacheDir, 25L * 1024 * 1024))
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build()
            }.also { client = it }
        }

    fun feedApi(context: Context, baseUrl: String): FeedApi {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client(context))
            .addConverterFactory(MoshiConverterFactory.create(moshi()))
            .build()
            .create(FeedApi::class.java)
    }
}
