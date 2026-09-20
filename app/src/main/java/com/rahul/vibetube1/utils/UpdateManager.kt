/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rahul.vibetube1.BuildConfig
import com.rahul.vibetube1.data.network.VersionInfo
import com.rahul.vibetube1.workers.AppUpdateCheckWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.updateManagerDataStore: DataStore<Preferences> by preferencesDataStore(name = "update_manager_settings")

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: HttpClient
) {
    private val dataStore = context.updateManagerDataStore

    companion object {
        private val KEY_LATEST_VERSION_CODE = intPreferencesKey("latest_version_code")
        private const val REMOTE_VERSION_JSON_URL = "https://raw.githubusercontent.com/Rahulhaldar/ZyvoTube/main/version.json"
        private const val WORK_NAME = "periodic_update_check_work"
    }

    /**
     * Cache the latest known version code from the remote JSON in DataStore
     */
    suspend fun cacheLatestVersionCode(versionCode: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_LATEST_VERSION_CODE] = versionCode
        }
    }

    /**
     * Retrieve the cached version code from DataStore
     */
    fun getCachedLatestVersionCode(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[KEY_LATEST_VERSION_CODE] ?: BuildConfig.VERSION_CODE
        }
    }

    /**
     * Schedules a periodic background update check via WorkManager
     */
    fun schedulePeriodicUpdateCheck(intervalHours: Long = 24) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<AppUpdateCheckWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }

    /**
     * Checks the remote version JSON and compares it with the local versionCode.
     * Returns true if there is a newer update available.
     */
    suspend fun checkRemoteUpdate(): UpdateCheckResult {
        return try {
            val response = client.get(REMOTE_VERSION_JSON_URL)
            if (response.status.value != 200) {
                throw java.io.IOException("Server returned HTTP status ${response.status.value}")
            }
            val jsonText = response.bodyAsText()
            val versionInfo = Json { ignoreUnknownKeys = true }.decodeFromString<VersionInfo>(jsonText)
            val remoteVersionCode = versionInfo.resolvedVersionCode ?: 0
            val localVersionCode = BuildConfig.VERSION_CODE
            val remoteVersion = versionInfo.resolvedVersionName

            if (remoteVersionCode > 0) {
                cacheLatestVersionCode(remoteVersionCode)
            }

            val hasUpdate = remoteVersionCode > localVersionCode
            
            // Log exactly as requested
            PTLog.d("UpdateCheck", "installedVersionCode=$localVersionCode")
            PTLog.d("UpdateCheck", "remoteVersionCode=$remoteVersionCode")
            PTLog.d("UpdateCheck", "remoteVersionName=$remoteVersion")
            PTLog.d("UpdateCheck", "updateAvailable=$hasUpdate")

            UpdateCheckResult.Success(
                hasUpdate = hasUpdate,
                isMandatory = hasUpdate, // Every newer version is mandatory/blocking
                remoteVersionCode = remoteVersionCode,
                versionInfo = versionInfo
            )
        } catch (e: Exception) {
            PTLog.d("UpdateCheck", "Network/Parsing failed: ${e.message}")
            UpdateCheckResult.Failure(e)
        }
    }
}

sealed interface UpdateCheckResult {
    data class Success(
        val hasUpdate: Boolean,
        val isMandatory: Boolean,
        val remoteVersionCode: Int,
        val versionInfo: VersionInfo
    ) : UpdateCheckResult

    data class Failure(val throwable: Throwable) : UpdateCheckResult
}
