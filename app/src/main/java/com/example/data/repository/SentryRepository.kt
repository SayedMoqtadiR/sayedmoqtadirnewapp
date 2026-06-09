package com.example.data.repository

import com.example.data.db.SentryDao
import com.example.data.model.AppUsage
import com.example.data.model.DailyUsage
import com.example.data.model.SentrySetting
import com.example.data.model.TodoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SentryRepository(private val sentryDao: SentryDao) {

    val allTodos: Flow<List<TodoItem>> = sentryDao.getAllTodosFlow()
    val allSettings: Flow<List<SentrySetting>> = sentryDao.getAllSettings()
    val appUsages: Flow<List<AppUsage>> = sentryDao.getAppUsagesFlow()
    val dailyUsages: Flow<List<DailyUsage>> = sentryDao.getDailyUsagesFlow()

    fun getSetting(key: String): Flow<String?> {
        return sentryDao.getSetting(key).map { it?.value }
    }

    suspend fun getSettingSync(key: String): String? {
        return sentryDao.getSettingSync(key)?.value
    }

    suspend fun saveSetting(key: String, value: String) {
        sentryDao.insertSetting(SentrySetting(key, value))
    }

    suspend fun insertTodo(item: TodoItem) {
        sentryDao.insertTodo(item)
    }

    suspend fun deleteTodoById(id: Int) {
        sentryDao.deleteTodoById(id)
    }

    suspend fun updateTodoStatus(id: Int, isCompleted: Boolean) {
        sentryDao.updateTodoStatus(id, isCompleted)
    }

    suspend fun insertAppUsage(appUsage: AppUsage) {
        sentryDao.insertAppUsage(appUsage)
    }

    suspend fun clearAppUsages() {
        sentryDao.clearAppUsages()
    }

    suspend fun insertDailyUsage(dailyUsage: DailyUsage) {
        sentryDao.insertDailyUsage(dailyUsage)
    }

    // Call this when blocking occurs! Increases "distracted_minutes" by 1!
    suspend fun recordBlockEvent() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        // Update distracted minutes in daily usage
        val allDaily = sentryDao.getDailyUsagesFlow()
        // Wait, the easiest way is to insert a default or update today.
        // Let's check sync settings or write a quick update:
        // We can check if today already exists, retrieve, increment and insert.
        // Let's implement that logic safely in Repository or Dao.
        // Alternatively, we can store it as a setting "distracted_minutes_today" and daily_usages is just historic.
        // Let's do both: increment a local key setting "distracted_time_today"
        val currDistractedStr = getSettingSync("distracted_time_today") ?: "17"
        val nextDistracted = (currDistractedStr.toIntOrNull() ?: 17) + 1
        saveSetting("distracted_time_today", nextDistracted.toString())
    }
}
