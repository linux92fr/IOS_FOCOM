package com.example.data.remote

import com.squareup.moshi.Json
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

data class SupabaseContactMessage(
    val name: String,
    val email: String,
    val subject: String,
    val message: String,
    val category: String = "Application Android"
)

data class SupabaseArticle(
    val id: String,
    val title: String,
    val content: String? = "",
    val excerpt: String? = null,
    val category: String? = null,
    val slug: String? = null,
    @param:Json(name = "image_url") val imageUrl: String? = null,
    @param:Json(name = "published_at") val publishedAt: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null
)

data class SupabaseFlashBanner(
    val id: String,
    val message: String,
    val type: String,
    val link: String? = null,
    @param:Json(name = "start_date") val startDate: String,
    @param:Json(name = "end_date") val endDate: String,
    @param:Json(name = "is_active") val isActive: Boolean? = true
)

interface SupabaseService {
    @POST("rest/v1/contact_messages")
    suspend fun insertContactMessage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String? = null,
        @Header("Prefer") prefer: String = "return=minimal",
        @Body message: SupabaseContactMessage
    ): Response<Unit>

    @GET("rest/v1/articles")
    suspend fun getPublishedArticles(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String? = null,
        @Query("select") select: String = "id,title,content,excerpt,category,slug,image_url,published_at,created_at",
        @Query("is_published") isPublished: String = "eq.true",
        @Query("order") order: String = "published_at.desc.nullslast,created_at.desc",
        @Query("limit") limit: Int = 100
    ): Response<List<SupabaseArticle>>

    @GET("rest/v1/flash_banners")
    suspend fun getActiveFlashBanners(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String? = null,
        @Query("select") select: String = "id,message,type,link,start_date,end_date,is_active",
        @Query("is_active") isActive: String = "eq.true",
        @Query("start_date") startsBeforeOrAt: String,
        @Query("end_date") endsAfterOrAt: String,
        @Query("order") order: String = "created_at.desc"
    ): List<SupabaseFlashBanner>
}
