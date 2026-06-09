package com.example

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.SocialSentryAccessibilityService
import com.example.ui.screens.MainSentryContainer
import com.example.ui.screens.SubScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.SentryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SentryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle initial launches with blocking details
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Check if service is active and show guide banner if disabled
                    AccessibilityPermissionBanner()

                    // Main Container
                    MainSentryContainer(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val showBlock = intent.getBooleanExtra("SHOW_BLOCKED_ALERT", false)
        if (showBlock) {
            val reason = intent.getStringExtra("BLOCKED_REASON") ?: "Content"
            viewModel.setBlockedAlert(true, reason)
        }
    }
}

@Composable
fun AccessibilityPermissionBanner() {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(true) }

    // Periodically recheck check status when MainActivity gets resumed
    LaunchedEffect(Unit) {
        while (true) {
            isEnabled = isAccessibilityServiceEnabled(context, SocialSentryAccessibilityService::class.java)
            kotlinx.coroutines.delay(2000)
        }
    }

    AnimatedVisibility(
        visible = !isEnabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CoralRed),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .clickable {
                    try {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        Toast.makeText(context, "Locate 'Social Sentry' and enable it! 🛡️", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open settings. Please enable manually.", Toast.LENGTH_LONG).show()
                    }
                }
                .testTag("perm_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Permission Alert",
                        tint = SlateBlack,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Social Sentry Service is Disabled. Tap to enable blocking in settings!",
                        color = SlateBlack,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp
                    )
                }

                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = "Setup",
                    tint = SlateBlack,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<out AccessibilityService>): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    for (service in enabledServices) {
        val resolvedServiceInfo = service.resolveInfo.serviceInfo
        if (resolvedServiceInfo.packageName == context.packageName && resolvedServiceInfo.name == serviceClass.name) {
            return true
        }
    }
    return false
}
