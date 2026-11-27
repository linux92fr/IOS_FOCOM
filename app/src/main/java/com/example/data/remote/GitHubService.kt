package com.example.data.remote

import com.example.data.model.GitHubCommitContainer
import com.example.data.model.GitHubContent
import com.example.data.model.GitHubIssue
import com.example.data.model.GitHubRepo
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface GitHubService {

    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("User-Agent") userAgent: String = "GitResume-App",
        @Header("Authorization") token: String? = null
    ): GitHubRepo

    @GET("repos/{owner}/{repo}/contents")
    suspend fun getRootContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("User-Agent") userAgent: String = "GitResume-App",
        @Header("Authorization") token: String? = null
    ): List<GitHubContent>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getPathContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Header("User-Agent") userAgent: String = "GitResume-App",
        @Header("Authorization") token: String? = null
    ): List<GitHubContent>

    @GET("repos/{owner}/{repo}/commits")
    suspend fun getCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30,
        @Header("User-Agent") userAgent: String = "GitResume-App",
        @Header("Authorization") token: String? = null
    ): List<GitHubCommitContainer>

    @GET("repos/{owner}/{repo}/issues")
    suspend fun getIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "all",
        @Query("per_page") perPage: Int = 30,
        @Header("User-Agent") userAgent: String = "GitResume-App",
        @Header("Authorization") token: String? = null
    ): List<GitHubIssue>

    @GET
    suspend fun getRawText(
        @Url url: String,
        @Header("User-Agent") userAgent: String = "GitResume-App",
        @Header("Authorization") token: String? = null
    ): Response<ResponseBody>
}
