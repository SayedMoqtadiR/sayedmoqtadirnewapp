package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.repository.SentryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class SocialSentryAccessibilityService : AccessibilityService() {

    private var isMasterBlockingEnabled = true
    private var isBlockYoutube = true
    private var isBlockInstagram = true
    private var isBlockTiktok = true
    private var isBlockFacebook = true
    private var isPornBlocking = true
    private var isScrollLimit = true

    private var database: AppDatabase? = null
    private var repository: SentryRepository? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var lastBlockTime = 0L
    private val blockCooldown = 3000L // 3 seconds cooldown to avoid looping redirects

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = SentryRepository(database!!.sentryDao())

        // Reactively listen to settings updates from the database
        serviceScope.launch {
            repository!!.allSettings.collect { settings ->
                val map = settings.associateBy { it.key }
                isMasterBlockingEnabled = map["master_blocking"]?.value?.toBoolean() ?: true
                isBlockYoutube = map["block_youtube"]?.value?.toBoolean() ?: true
                isBlockInstagram = map["block_instagram"]?.value?.toBoolean() ?: true
                isBlockTiktok = map["block_tiktok"]?.value?.toBoolean() ?: true
                isBlockFacebook = map["block_facebook"]?.value?.toBoolean() ?: true
                isPornBlocking = map["porn_blocking"]?.value?.toBoolean() ?: true
                isScrollLimit = map["scroll_limit"]?.value?.toBoolean() ?: true
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!isMasterBlockingEnabled) return

        val packageName = event.packageName?.toString() ?: return
        val rootNode = rootInActiveWindow ?: return

        try {
            var shouldBlock = false
            var blockReason = ""

            // 1. YouTube Shorts Blocker
            if (packageName == "com.google.android.youtube" && isBlockYoutube) {
                if (detectYoutubeShorts(rootNode)) {
                    shouldBlock = true
                    blockReason = "YouTube Shorts"
                }
            }
            // 2. Instagram Reels Blocker
            else if (packageName == "com.instagram.android" && isBlockInstagram) {
                if (detectInstagramReels(rootNode)) {
                    shouldBlock = true
                    blockReason = "Instagram Reels"
                }
            }
            // 3. TikTok Blocker (Blocks whole TikTok app for maximum focus!)
            else if ((packageName == "com.zhiliaoapp.musically" || packageName == "com.zhiliaoapp.musically.go") && isBlockTiktok) {
                shouldBlock = true
                blockReason = "TikTok"
            }
            // 4. Facebook Reels Blocker
            else if ((packageName == "com.facebook.katana" || packageName == "com.facebook.lite") && isBlockFacebook) {
                if (detectFacebookReels(rootNode)) {
                    shouldBlock = true
                    blockReason = "Facebook Reels"
                }
            }
            // 5. Porn / NSFW Blocker (Supports Chrome, Firefox, Edge, Opera and generic search/sites)
            else if (isPornBlocking && isBrowserOrSearch(packageName)) {
                if (detectAdultContent(rootNode)) {
                    shouldBlock = true
                    blockReason = "NSFW Content"
                }
            }

            if (shouldBlock) {
                triggerBlockAction(blockReason)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            rootNode.recycle()
        }
    }

    private fun isBrowserOrSearch(packageName: String): Boolean {
        val browsers = listOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.sec.android.app.sbrowser",
            "com.google.android.googlequicksearchbox"
        )
        return browsers.contains(packageName)
    }

    private fun detectYoutubeShorts(rootNode: AccessibilityNodeInfo): Boolean {
        // Detects keywords/resource structures of YouTube Shorts page
        val keywords = listOf("shorts", "short video")
        val ids = listOf("shorts_player", "shorts_container", "shorts_layout", "shorts-scene")
        return findNodeBySignatures(rootNode, keywords, ids)
    }

    private fun detectInstagramReels(rootNode: AccessibilityNodeInfo): Boolean {
        // Detects keywords or resource ids for Reels
        val keywords = listOf("reels_viewer", "reel_profile", "reels_tab_container")
        val ids = listOf("reels_viewer_container", "reel_content", "reels_video_view")
        
        // Also check if content description or textual text contains Reels or Swipe Up to watch
        if (findNodeBySignatures(rootNode, keywords, ids)) {
            return true
        }
        
        // A direct text check for "Reels" in the Instagram package is often indicative of the Reels player
        return checkSimpleTextMatch(rootNode, "reels")
    }

    private fun detectFacebookReels(rootNode: AccessibilityNodeInfo): Boolean {
        val keywords = listOf("reels_video", "fb reels", "reels tab")
        val ids = listOf("reels_viewer", "fb_reels")
        return findNodeBySignatures(rootNode, keywords, ids)
    }

    private fun detectAdultContent(rootNode: AccessibilityNodeInfo): Boolean {
        // Keywords checking for NSFW content / Porn search in browser URL or page titles
        val adultKeywords = listOf(
            "porn", "pornhub", "nsfw", "xxx", "redtube", "xvideos", "hentai", 
            "sex site", "adult video", "porno", "xhamster", "brazzers"
        )
        return scanNodeTextForAdultKeywords(rootNode, adultKeywords)
    }

    private fun findNodeBySignatures(
        node: AccessibilityNodeInfo,
        keywords: List<String>,
        resourceIds: List<String>
    ): Boolean {
        // Check current node text or desc for keywords
        val text = node.text?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val desc = node.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val resId = node.viewIdResourceName?.lowercase(Locale.getDefault()) ?: ""

        for (kw in keywords) {
            if (text.contains(kw) || desc.contains(kw)) {
                return true
            }
        }

        for (id in resourceIds) {
            if (resId.contains(id)) {
                return true
            }
        }

        // Search children
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeBySignatures(child, keywords, resourceIds)
            child.recycle()
            if (found) return true
        }

        return false
    }

    private fun checkSimpleTextMatch(node: AccessibilityNodeInfo, textToFind: String): Boolean {
        val text = node.text?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val desc = node.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""
        if (text == textToFind || desc == textToFind) {
            return true
        }
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val found = checkSimpleTextMatch(child, textToFind)
            child.recycle()
            if (found) return true
        }
        return false
    }

    private fun scanNodeTextForAdultKeywords(node: AccessibilityNodeInfo, keywords: List<String>): Boolean {
        val text = node.text?.toString()?.lowercase(Locale.getDefault()) ?: ""
        val desc = node.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""

        for (kw in keywords) {
            // URL might be something like "pornhub.com" or google search "xxx clips"
            if (text.contains(kw) || desc.contains(kw)) {
                return true
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val found = scanNodeTextForAdultKeywords(child, keywords)
            child.recycle()
            if (found) return true
        }
        return false
    }

    private fun triggerBlockAction(reason: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBlockTime < blockCooldown) {
            // Do not double-redirect in quick succession
            return
        }
        lastBlockTime = currentTime

        // 1. Kick user out of the distracting screen immediately by going back!
        performGlobalAction(GLOBAL_ACTION_BACK)

        // 2. record the Block Event in Room DB (increments distracted count for UI)
        serviceScope.launch {
            repository?.recordBlockEvent()
        }

        // 3. Show Toast Notification
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                applicationContext,
                "🚫 Social Sentry blocked $reason! Stay focused! 🛡️",
                Toast.LENGTH_LONG
            ).show()

            // 4. Launch Social Sentry focus screen to keep user disciplined!
            val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("SHOW_BLOCKED_ALERT", true)
                putExtra("BLOCKED_REASON", reason)
            }
            startActivity(launchIntent)
        }
    }

    override fun onInterrupt() {
        // Handle interrupt
    }
}
