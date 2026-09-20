/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rahul.vibetube1.data.local.DownloadDao
import com.rahul.vibetube1.data.local.FavoriteDao
import com.rahul.vibetube1.data.local.HistoryDao
import com.rahul.vibetube1.data.local.PlaylistFavoriteDao
import com.rahul.vibetube1.data.local.SearchHistoryDao
import com.rahul.vibetube1.data.local.SubscriptionDao
import com.rahul.vibetube1.data.local.UserInterestDao
import com.rahul.vibetube1.data.local.VibeTubeDatabase
import com.rahul.vibetube1.data.local.BlacklistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VibeTubeDatabase {
        fun createAllTablesIfNotExists(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `downloads` (`videoId` TEXT NOT NULL, `title` TEXT NOT NULL, `thumbnailUrl` TEXT NOT NULL, `uploaderName` TEXT NOT NULL, `filePath` TEXT NOT NULL, `totalSize` INTEGER NOT NULL, `downloadedSize` INTEGER NOT NULL, `status` TEXT NOT NULL, `quality` TEXT, `format` TEXT, `videoUrl` TEXT, `audioUrl` TEXT, `playlistId` TEXT, `playlistTitle` TEXT, `isAudioOnly` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`videoId`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `history` (`videoId` TEXT NOT NULL, `title` TEXT NOT NULL, `thumbnailUrl` TEXT NOT NULL, `uploaderName` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `progressMs` INTEGER NOT NULL, `durationMs` INTEGER NOT NULL, PRIMARY KEY(`videoId`))")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_history_timestamp` ON `history` (`timestamp`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`videoId` TEXT NOT NULL, `title` TEXT NOT NULL, `thumbnailUrl` TEXT NOT NULL, `uploaderName` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`videoId`))")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorites_timestamp` ON `favorites` (`timestamp`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `subscriptions` (`channelId` TEXT NOT NULL, `name` TEXT NOT NULL, `thumbnailUrl` TEXT, `subscriberCount` INTEGER, PRIMARY KEY(`channelId`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `search_history` (`query` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`query`))")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_search_history_timestamp` ON `search_history` (`timestamp`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_favorites` (`playlistId` TEXT NOT NULL, `title` TEXT NOT NULL, `thumbnailUrl` TEXT NOT NULL, `uploaderName` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`playlistId`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `user_interests` (`keyword` TEXT NOT NULL, `weight` REAL NOT NULL, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`keyword`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `blacklist` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `local_playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `thumbnailUrl` TEXT, `createdAt` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `local_playlist_videos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` INTEGER NOT NULL, `videoId` TEXT NOT NULL, `title` TEXT NOT NULL, `thumbnailUrl` TEXT NOT NULL, `uploaderName` TEXT NOT NULL, `duration` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, FOREIGN KEY(`playlistId`) REFERENCES `local_playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_local_playlist_videos_playlistId` ON `local_playlist_videos` (`playlistId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_local_playlist_videos_playlistId_videoId` ON `local_playlist_videos` (`playlistId`, `videoId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `feed_cache` (`feedKey` TEXT NOT NULL, `videos` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`feedKey`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `download_missions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `videoId` TEXT NOT NULL, `title` TEXT NOT NULL, `quality` TEXT NOT NULL, `totalBytes` INTEGER NOT NULL, `downloadedBytes` INTEGER NOT NULL, `status` TEXT NOT NULL, `outputFilePath` TEXT, `creationTime` INTEGER NOT NULL, `videoUrl` TEXT, `audioUrl` TEXT, `format` TEXT, `isAudioOnly` INTEGER NOT NULL DEFAULT 0)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `download_chunks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `missionId` INTEGER NOT NULL, `chunkIndex` INTEGER NOT NULL, `startByte` INTEGER NOT NULL, `endByte` INTEGER NOT NULL, `bytesDownloaded` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `type` TEXT NOT NULL, FOREIGN KEY(`missionId`) REFERENCES `download_missions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_chunks_missionId` ON `download_chunks` (`missionId`)")
        }

        val migrations = (1..16).map { startVersion ->
            object : Migration(startVersion, 17) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    createAllTablesIfNotExists(db)
                    
                    // Specific column additions for existing tables
                    val cursor = db.query("PRAGMA table_info(`downloads`)")
                    var hasIsAudioOnly = false
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                        if (name == "isAudioOnly") {
                            hasIsAudioOnly = true
                            break
                        }
                    }
                    cursor.close()
                    
                    if (!hasIsAudioOnly) {
                        db.execSQL("ALTER TABLE `downloads` ADD COLUMN `isAudioOnly` INTEGER NOT NULL DEFAULT 0")
                    }

                    val missionCursor = db.query("PRAGMA table_info(`download_missions`)")
                    var missionHasIsAudioOnly = false
                    while (missionCursor.moveToNext()) {
                        val name = missionCursor.getString(missionCursor.getColumnIndexOrThrow("name"))
                        if (name == "isAudioOnly") {
                            missionHasIsAudioOnly = true
                            break
                        }
                    }
                    missionCursor.close()

                    if (!missionHasIsAudioOnly) {
                        db.execSQL("ALTER TABLE `download_missions` ADD COLUMN `isAudioOnly` INTEGER NOT NULL DEFAULT 0")
                    }
                }
            }
        }.toTypedArray()

        return Room.databaseBuilder(
            context,
            VibeTubeDatabase::class.java,
            "playtube_db"
        )
        .addMigrations(*migrations)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideDownloadDao(database: VibeTubeDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    fun provideHistoryDao(database: VibeTubeDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    fun provideFavoriteDao(database: VibeTubeDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun providePlaylistFavoriteDao(database: VibeTubeDatabase): PlaylistFavoriteDao {
        return database.playlistFavoriteDao()
    }

    @Provides
    fun provideSubscriptionDao(database: VibeTubeDatabase): SubscriptionDao {
        return database.subscriptionDao()
    }

    @Provides
    fun provideSearchHistoryDao(database: VibeTubeDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Provides
    fun provideUserInterestDao(database: VibeTubeDatabase): UserInterestDao {
        return database.userInterestDao()
    }

    @Provides
    fun provideBlacklistDao(database: VibeTubeDatabase): BlacklistDao {
        return database.blacklistDao()
    }

    @Provides
    fun provideLocalPlaylistDao(database: VibeTubeDatabase): com.rahul.vibetube1.data.local.LocalPlaylistDao {
        return database.localPlaylistDao()
    }

    @Provides
    fun provideFeedCacheDao(database: VibeTubeDatabase): com.rahul.vibetube1.data.local.FeedCacheDao {
        return database.feedCacheDao()
    }

    @Provides
    fun provideMissionDao(database: VibeTubeDatabase): com.rahul.vibetube1.data.local.MissionDao {
        return database.missionDao()
    }
}
