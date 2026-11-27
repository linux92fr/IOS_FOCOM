package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_repos")
data class SavedRepo(
    @PrimaryKey val fullName: String, // e.g. "owner/name"
    val owner: String,
    val name: String,
    val description: String?,
    val stars: Int,
    val forks: Int,
    val language: String?,
    val lastAccessed: Long,
    val avatarUrl: String?
)
