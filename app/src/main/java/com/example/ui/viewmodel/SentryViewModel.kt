package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AppUsage
import com.example.data.model.DailyUsage
import com.example.data.model.TodoItem
import com.example.data.repository.SentryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SentryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = SentryRepository(db.sentryDao())

    // Expose flows directly from DB
    val allTodos: StateFlow<List<TodoItem>> = repository.allTodos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settingsMap: StateFlow<Map<String, String>> = repository.allSettings
        .map { list -> list.associate { it.key to it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val appUsages: StateFlow<List<AppUsage>> = repository.appUsages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyUsages: StateFlow<List<DailyUsage>> = repository.dailyUsages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active block dialog states
    private val _showBlockedAlert = MutableStateFlow(false)
    val showBlockedAlert = _showBlockedAlert.asStateFlow()

    private val _blockedReason = MutableStateFlow("")
    val blockedReason = _blockedReason.asStateFlow()

    // Interactive selections in UI
    private val _selectedDateStr = MutableStateFlow("")
    val selectedDateStr = _selectedDateStr.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    // Live porn blocker countdown ticked states
    private val _pornStreakTicker = MutableStateFlow(StreakTimeLeft(0, 0, 0, 0))
    val pornStreakTicker = _pornStreakTicker.asStateFlow()

    init {
        // Set default selected date string as today's date
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        _selectedDateStr.value = todayStr

        // Start Porn Streak live seconds updating ticker
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                updatePornStreakTicker()
                delay(1000)
            }
        }
    }

    private fun updatePornStreakTicker() {
        val startMillisStr = settingsMap.value["porn_streak_start"] ?: return
        val startMillis = startMillisStr.toLongOrNull() ?: System.currentTimeMillis()
        val diffSeconds = (System.currentTimeMillis() - startMillis) / 1000

        if (diffSeconds > 0) {
            val days = diffSeconds / (24 * 3600)
            var remainder = diffSeconds % (24 * 3600)
            val hours = remainder / 3600
            remainder %= 3600
            val minutes = remainder / 60
            val seconds = remainder % 60
            _pornStreakTicker.value = StreakTimeLeft(days, hours, minutes, seconds)
        } else {
            _pornStreakTicker.value = StreakTimeLeft(0, 0, 0, 0)
        }
    }

    // Helper to safety fetch values from settings map
    fun getSettingValue(key: String, default: String): String {
        return settingsMap.value[key] ?: default
    }

    fun saveSetting(key: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSetting(key, value)
        }
    }

    // Individual toggle helpers
    fun toggleMasterBlocking() {
        val current = getSettingValue("master_blocking", "true").toBoolean()
        saveSetting("master_blocking", (!current).toString())
    }

    fun toggleBlockYoutube() {
        val current = getSettingValue("block_youtube", "true").toBoolean()
        saveSetting("block_youtube", (!current).toString())
    }

    fun toggleBlockInstagram() {
        val current = getSettingValue("block_instagram", "true").toBoolean()
        saveSetting("block_instagram", (!current).toString())
    }

    fun toggleBlockTiktok() {
        val current = getSettingValue("block_tiktok", "true").toBoolean()
        saveSetting("block_tiktok", (!current).toString())
    }

    fun toggleBlockFacebook() {
        val current = getSettingValue("block_facebook", "true").toBoolean()
        saveSetting("block_facebook", (!current).toString())
    }

    fun togglePornBlocking() {
        val current = getSettingValue("porn_blocking", "true").toBoolean()
        saveSetting("porn_blocking", (!current).toString())
    }

    fun toggleScrollLimit() {
        val current = getSettingValue("scroll_limit", "true").toBoolean()
        saveSetting("scroll_limit", (!current).toString())
    }

    fun toggleSafetyMode() {
        val current = getSettingValue("safety_mode", "false").toBoolean()
        saveSetting("safety_mode", (!current).toString())
    }

    // Streak Reset Function (User can reset when relapsed)
    fun resetPornStreak() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSetting("porn_streak_start", System.currentTimeMillis().toString())
            // Increment distraction level or add a penalty
            val currentKarma = getSettingValue("user_karma", "853").toIntOrNull() ?: 853
            val nextKarma = (currentKarma - 50).coerceAtLeast(0)
            repository.saveSetting("user_karma", nextKarma.toString())
        }
    }

    // Streak Increase manually or daily check benefits
    fun checkDailyStreakReward() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentKarma = getSettingValue("user_karma", "853").toIntOrNull() ?: 853
            val nextKarma = currentKarma + 15
            repository.saveSetting("user_karma", nextKarma.toString())
        }
    }

    // Todo Action Handlers
    fun addTodo(
        title: String,
        subtitle: String,
        timeString: String,
        durationMinutes: Int,
        category: String,
        iconName: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = selectedDateStr.value
            val item = TodoItem(
                title = title,
                subtitle = subtitle,
                timeString = timeString,
                durationMinutes = durationMinutes,
                isCompleted = false,
                dateString = dateStr,
                category = category,
                iconName = iconName
            )
            repository.insertTodo(item)
            // Reward some Karma for planning!
            val currentKarma = getSettingValue("user_karma", "853").toIntOrNull() ?: 853
            repository.saveSetting("user_karma", (currentKarma + 10).toString())
        }
    }

    fun deleteTodo(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTodoById(id)
        }
    }

    fun toggleTodoCompletion(id: Int, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTodoStatus(id, isCompleted)
            // Reward or deduct karma
            val currentKarma = getSettingValue("user_karma", "853").toIntOrNull() ?: 853
            val bonus = if (isCompleted) 20 else -20
            repository.saveSetting("user_karma", (currentKarma + bonus).coerceAtLeast(0).toString())
        }
    }

    fun selectDate(dateStr: String) {
        _selectedDateStr.value = dateStr
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setBlockedAlert(show: Boolean, reason: String = "") {
        _showBlockedAlert.value = show
        _blockedReason.value = reason
    }
}

// Data holder for live countdown
data class StreakTimeLeft(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long
)
