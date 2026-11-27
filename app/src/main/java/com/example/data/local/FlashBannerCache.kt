package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.data.model.FocomFlashBanner

@Entity(tableName = "flash_banner_cache")
data class CachedFlashBanner(
    @PrimaryKey val id: String,
    val message: String,
    val type: String,
    val link: String?,
    val startDate: String,
    val endDate: String,
    val position: Int,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toBanner() = FocomFlashBanner(
        id = id,
        message = message,
        type = type,
        link = link,
        startDate = startDate,
        endDate = endDate
    )

    companion object {
        fun from(banner: FocomFlashBanner, position: Int) = CachedFlashBanner(
            id = banner.id,
            message = banner.message,
            type = banner.type,
            link = banner.link,
            startDate = banner.startDate,
            endDate = banner.endDate,
            position = position
        )
    }
}

@Dao
interface FlashBannerCacheDao {
    @Query("SELECT * FROM flash_banner_cache ORDER BY position ASC")
    suspend fun getAll(): List<CachedFlashBanner>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceAll(banners: List<CachedFlashBanner>)

    @Query("DELETE FROM flash_banner_cache")
    suspend fun clear()
}
