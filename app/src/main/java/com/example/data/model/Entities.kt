package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sentry_settings")
data class SentrySetting(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "todo_items")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subtitle: String,
    val timeString: String, // e.g. "08:00"
    val durationMinutes: Int, // e.g. 60
    val isCompleted: Boolean = false,
    val dateString: String, // e.g. "2026-06-07"
    val category: String, // "All", "Personal", "Work", "Health", "Shopping"
    val iconName: String // "bell", "school", "work", "fitness", "pencil"
)

@Entity(tableName = "app_usages")
data class AppUsage(
    @PrimaryKey val packageName: String,
    val appName: String,
    val usageMinutes: Int,
    val isDistracting: Boolean
)

@Entity(tableName = "daily_usages")
data class DailyUsage(
    @PrimaryKey val dateString: String, // e.g. "2026-06-07"
    val totalMinutes: Int,
    val distractedMinutes: Int
)
