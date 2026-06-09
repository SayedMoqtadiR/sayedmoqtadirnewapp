package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppUsage
import com.example.data.model.DailyUsage
import com.example.data.model.SentrySetting
import com.example.data.model.TodoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@Database(
    entities = [
        SentrySetting::class,
        TodoItem::class,
        AppUsage::class,
        DailyUsage::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sentryDao(): SentryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "social_sentry_db"
                )
                    .addCallback(DatabasePrepopulateCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabasePrepopulateCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val dao = getInstance(context).sentryDao()
                populateDefaultData(dao)
            }
        }

        private suspend fun populateDefaultData(dao: SentryDao) {
            // Populate Settings
            val defaultSettings = listOf(
                SentrySetting("master_blocking", "true"),
                SentrySetting("block_youtube", "true"),
                SentrySetting("block_instagram", "true"),
                SentrySetting("block_tiktok", "true"),
                SentrySetting("block_facebook", "true"),
                SentrySetting("porn_blocking", "true"),
                SentrySetting("scroll_limit", "true"),
                SentrySetting("scroll_limit_min", "15"),
                SentrySetting("safety_mode", "false"),
                SentrySetting("user_name", "MK Shaon"),
                SentrySetting("user_handle", "@mkshaon7"),
                SentrySetting("user_karma", "853"),
                // Set start date 14 days ago for porn blocker streak (adds up to 14 days)
                SentrySetting("porn_streak_start", (System.currentTimeMillis() - 14L * 24 * 3600 * 1000).toString()),
                SentrySetting("porn_streak_longest", "30")
            )
            for (setting in defaultSettings) {
                dao.insertSetting(setting)
            }

            // Generate Date strings for the past few days including today
            val calendar = Calendar.getInstance()
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStrings = (0..6).map { offset ->
                val cal = calendar.clone() as Calendar
                cal.add(Calendar.DAY_OF_YEAR, -offset)
                format.format(cal.time)
            }.reversed()

            // Populate Daily Usages matching Screen 6 ("Last 7 Days" graph):
            // Sun: 4h 2m (242m), Mon: 5h 23m (323m), Tue: 2h 15m (135m), Wed: 1h 37m (97m), Thu: 3h 37m (217m), Fri: 5h 8m (308m), Sat: 1h 38m (98m)
            val usageMinutes = listOf(242, 323, 135, 97, 217, 308, 98)
            val distractedMinutes = listOf(17, 25, 10, 8, 18, 22, 17)
            for (i in 0 until dateStrings.size.coerceAtMost(7)) {
                dao.insertDailyUsage(
                    DailyUsage(
                        dateString = dateStrings[i],
                        totalMinutes = usageMinutes[i],
                        distractedMinutes = distractedMinutes[i]
                    )
                )
            }

            // Populate App Usages (Screen 6: "Most Used Apps")
            val defaultApps = listOf(
                AppUsage("org.telegram.messenger", "Telegram", 33, false),
                AppUsage("com.tencent.ig", "Delta Force", 18, false),
                AppUsage("com.instagram.android", "Instagram", 45, true),
                AppUsage("com.google.android.youtube", "YouTube", 65, true),
                AppUsage("com.zhiliaoapp.musically", "TikTok", 24, true)
            )
            for (app in defaultApps) {
                dao.insertAppUsage(app)
            }

            // Populate To-do items (Screen 2: Wake up!, Study, Work out, Wind Down for today and yesterday)
            val todayStr = format.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = format.format(calendar.time)

            val todos = listOf(
                TodoItem(
                    title = "Wake up!",
                    subtitle = "A well-deserved break.",
                    timeString = "05:30",
                    durationMinutes = 30,
                    isCompleted = false,
                    dateString = todayStr,
                    category = "Health",
                    iconName = "bell"
                ),
                TodoItem(
                    title = "Study",
                    subtitle = "08:00 - 09:00 (1h)",
                    timeString = "08:00",
                    durationMinutes = 60,
                    isCompleted = false,
                    dateString = todayStr,
                    category = "Work",
                    iconName = "school"
                ),
                TodoItem(
                    title = "Work out",
                    subtitle = "09:10 - 09:40 (30m)",
                    timeString = "09:10",
                    durationMinutes = 30,
                    isCompleted = false,
                    dateString = todayStr,
                    category = "Health",
                    iconName = "fitness"
                ),
                TodoItem(
                    title = "Wind Down",
                    subtitle = "Time to recharge.",
                    timeString = "23:00",
                    durationMinutes = 60,
                    isCompleted = false,
                    dateString = todayStr,
                    category = "Work",
                    iconName = "pencil"
                ),
                // For yesterday, make some completed
                TodoItem(
                    title = "Wake up!",
                    subtitle = "Morning routine.",
                    timeString = "05:30",
                    durationMinutes = 30,
                    isCompleted = true,
                    dateString = yesterdayStr,
                    category = "Health",
                    iconName = "bell"
                ),
                TodoItem(
                    title = "Work out",
                    subtitle = "Daily exercise.",
                    timeString = "07:00",
                    durationMinutes = 45,
                    isCompleted = true,
                    dateString = yesterdayStr,
                    category = "Health",
                    iconName = "fitness"
                )
            )
            for (todo in todos) {
                dao.insertTodo(todo)
            }
        }
    }
}
