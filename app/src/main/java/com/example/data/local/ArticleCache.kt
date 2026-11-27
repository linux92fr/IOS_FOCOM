package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.FocomPublication
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "article_cache")
data class CachedArticle(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val date: String,
    val thumbnailUrl: String?,
    val pdfUrl: String,
    val excerpt: String,
    val content: String,
    val position: Int,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toPublication() = FocomPublication(
        id = id,
        title = title,
        type = type,
        date = date,
        thumbnailUrl = thumbnailUrl,
        pdfUrl = pdfUrl,
        excerpt = excerpt,
        content = content
    )

    companion object {
        fun from(publication: FocomPublication, position: Int) = CachedArticle(
            id = publication.id,
            title = publication.title,
            type = publication.type,
            date = publication.date,
            thumbnailUrl = publication.thumbnailUrl,
            pdfUrl = publication.pdfUrl,
            excerpt = publication.excerpt,
            content = publication.content,
            position = position
        )
    }
}

@Dao
interface ArticleCacheDao {
    @Query("SELECT * FROM article_cache ORDER BY position ASC")
    fun observeAll(): Flow<List<CachedArticle>>

    @Query("SELECT MAX(cachedAt) FROM article_cache")
    fun observeLastCachedAt(): Flow<Long?>

    @Query("SELECT * FROM article_cache ORDER BY position ASC")
    suspend fun getAll(): List<CachedArticle>

    @Query("SELECT * FROM article_cache WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CachedArticle?

    @Query("SELECT MAX(cachedAt) FROM article_cache")
    suspend fun getLastCachedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceAll(articles: List<CachedArticle>)

    @Query("DELETE FROM article_cache")
    suspend fun clear()

    @Transaction
    suspend fun replaceCache(articles: List<CachedArticle>) {
        clear()
        if (articles.isNotEmpty()) {
            replaceAll(articles)
        }
    }
}
