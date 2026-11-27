package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val name: String,
    @field:Json(name = "full_name") val fullName: String,
    val owner: GitHubOwner,
    val description: String?,
    @field:Json(name = "stargazers_count") val stars: Int,
    @field:Json(name = "forks_count") val forks: Int,
    @field:Json(name = "open_issues_count") val openIssues: Int?,
    val language: String?,
    @field:Json(name = "default_branch") val defaultBranch: String?
)

@JsonClass(generateAdapter = true)
data class GitHubOwner(
    val login: String,
    @field:Json(name = "avatar_url") val avatarUrl: String?
)

@JsonClass(generateAdapter = true)
data class GitHubContent(
    val name: String,
    val path: String,
    val type: String, // "file" or "dir"
    val size: Long,
    @field:Json(name = "download_url") val downloadUrl: String?
)

@JsonClass(generateAdapter = true)
data class GitHubCommitContainer(
    val sha: String,
    val commit: GitCommitDetails,
    val author: GitHubOwner?
)

@JsonClass(generateAdapter = true)
data class GitCommitDetails(
    val message: String,
    val author: GitCommitAuthor
)

@JsonClass(generateAdapter = true)
data class GitCommitAuthor(
    val name: String,
    val email: String,
    val date: String
)

@JsonClass(generateAdapter = true)
data class GitHubIssue(
    val number: Int,
    val title: String,
    val body: String?,
    val state: String,
    @field:Json(name = "created_at") val createdAt: String,
    val user: GitHubOwner
)
