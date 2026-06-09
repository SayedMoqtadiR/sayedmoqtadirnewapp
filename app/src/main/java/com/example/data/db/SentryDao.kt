package com.example.data.db

import androidx.room.*
import com.example.data.model.AppUsage
import com.example.data.model.DailyUsage
import com.example.data.model.SentrySetting
import com.example.data.model.TodoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SentryDao {

    // Settings Queries
    @Query("SELECT * FROM sentry_settings WHERE `key` = :key")
    fun getSetting(key: String): Flow<SentrySetting?>

    @Query("SELECT * FROM sentry_settings WHERE `key` = :key")
    fun getSettingSync(key: String): SentrySetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SentrySetting)

    @Query("SELECT * FROM sentry_settings")
    fun getAllSettings(): Flow<List<SentrySetting>>

    // Todo Queries
    @Query("SELECT * FROM todo_items ORDER BY id ASC")
    fun getAllTodosFlow(): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(item: TodoItem)

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteTodoById(id: Int)

    @Query("UPDATE todo_items SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTodoStatus(id: Int, isCompleted: Boolean)

    // App Usage Queries
    @Query("SELECT * FROM app_usages ORDER BY usageMinutes DESC")
    fun getAppUsagesFlow(): Flow<List<AppUsage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsage(usage: AppUsage)

    @Query("DELETE FROM app_usages")
    suspend fun clearAppUsages()

    // Daily Usage Queries
    @Query("SELECT * FROM daily_usages ORDER BY dateString ASC")
    fun getDailyUsagesFlow(): Flow<List<DailyUsage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyUsage(usage: DailyUsage)
}
