/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_interests")
data class UserInterestEntity(
    @PrimaryKey val keyword: String,
    val weight: Float,
    val lastUpdated: Long = System.currentTimeMillis()
)
