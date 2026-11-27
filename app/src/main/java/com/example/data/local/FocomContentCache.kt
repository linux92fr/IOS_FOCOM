package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "focom_content_cache")
data class CachedFocomContent(
    @PrimaryKey val id: String = DEFAULT_ID,
    val rawJson: String,
    val sourceUrl: String,
    val cachedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_ID = "default"
    }
}

@Dao
interface FocomContentCacheDao {
    @Query("SELECT * FROM focom_content_cache WHERE id = :id LIMIT 1")
    fun observeById(id: String = CachedFocomContent.DEFAULT_ID): Flow<CachedFocomContent?>

    @Query("SELECT * FROM focom_content_cache WHERE id = :id LIMIT 1")
    suspend fun getById(id: String = CachedFocomContent.DEFAULT_ID): CachedFocomContent?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: CachedFocomContent)
}
