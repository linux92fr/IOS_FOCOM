package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.RoomDatabase
import com.example.data.model.ContactMessage
import com.example.data.model.SavedRepo
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRepoDao {
    @Query("SELECT * FROM saved_repos ORDER BY lastAccessed DESC")
    fun getAllSavedRepos(): Flow<List<SavedRepo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepo(repo: SavedRepo)

    @Query("DELETE FROM saved_repos WHERE fullName = :fullName")
    suspend fun deleteRepo(fullName: String)

    @Query("SELECT * FROM saved_repos WHERE fullName = :fullName LIMIT 1")
    suspend fun getRepo(fullName: String): SavedRepo?
}

@Dao
interface ContactMessageDao {
    @Query("SELECT * FROM contact_messages ORDER BY createdAt DESC")
    fun getAllMessages(): Flow<List<ContactMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ContactMessage): Long

    @Query("UPDATE contact_messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: Long, status: String)

    @Query("SELECT * FROM contact_messages WHERE status != 'Envoyé' ORDER BY createdAt ASC")
    suspend fun getPendingMessages(): List<ContactMessage>
}

@Database(
    entities = [
        SavedRepo::class,
        ContactMessage::class,
        CachedArticle::class,
        CachedFlashBanner::class,
        CachedFocomContent::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedRepoDao(): SavedRepoDao
    abstract fun contactMessageDao(): ContactMessageDao
    abstract fun articleCacheDao(): ArticleCacheDao
    abstract fun flashBannerCacheDao(): FlashBannerCacheDao
    abstract fun focomContentCacheDao(): FocomContentCacheDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS article_cache (id TEXT NOT NULL, title TEXT NOT NULL, type TEXT NOT NULL, date TEXT NOT NULL, thumbnailUrl TEXT, pdfUrl TEXT NOT NULL, excerpt TEXT NOT NULL, content TEXT NOT NULL, position INTEGER NOT NULL, cachedAt INTEGER NOT NULL, PRIMARY KEY(id))")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS flash_banner_cache (id TEXT NOT NULL, message TEXT NOT NULL, type TEXT NOT NULL, link TEXT, startDate TEXT NOT NULL, endDate TEXT NOT NULL, position INTEGER NOT NULL, cachedAt INTEGER NOT NULL, PRIMARY KEY(id))")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS focom_content_cache (id TEXT NOT NULL, rawJson TEXT NOT NULL, sourceUrl TEXT NOT NULL, cachedAt INTEGER NOT NULL, PRIMARY KEY(id))")
            }
        }
    }
}
