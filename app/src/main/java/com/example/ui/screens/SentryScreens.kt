package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppUsage
import com.example.data.model.TodoItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.SentryViewModel
import com.example.ui.viewmodel.StreakTimeLeft
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

enum class ScreenTab {
    Usage,
    Todo,
    Home,
    Community,
    Insights
}

enum class SubScreen {
    ReelsBlockerSettings,
    PornBlockerDetails,
    AppLimitSettings,
    SafetyModeDetails,
    ScrollLimitDetails,
    AppTutorialDetails,
    AccountSettingsDetails
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSentryContainer(
    viewModel: SentryViewModel,
    initialSubScreen: SubScreen? = null,
    initialBlockedReason: String? = null
) {
    var currentTab by remember { mutableStateOf(ScreenTab.Home) }
    var activeSubScreen by remember { mutableStateOf<SubScreen?>(initialSubScreen) }

    val showBlockedAlert by viewModel.showBlockedAlert.collectAsState()
    val blockedReason by viewModel.blockedReason.collectAsState()

    // Handle intents passing blocking details
    LaunchedEffect(initialSubScreen, initialBlockedReason) {
        if (initialSubScreen != null) {
            activeSubScreen = initialSubScreen
        }
        if (initialBlockedReason != null) {
            viewModel.setBlockedAlert(true, initialBlockedReason)
        }
    }

    Scaffold(
        bottomBar = {
            if (activeSubScreen == null) {
                SocialSentryBottomNav(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        currentTab = tab
                    }
                )
            }
        },
        containerColor = SlateBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = activeSubScreen,
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                },
                label = "subscreen_navigation"
            ) { subScreen ->
                if (subScreen != null) {
                    RenderSubScreen(
                        subScreen = subScreen,
                        viewModel = viewModel,
                        onBack = { activeSubScreen = null }
                    )
                } else {
                    // Render Active Tab
                    Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                        when (tab) {
                            ScreenTab.Home -> DashboardHomeScreen(
                                viewModel = viewModel,
                                onNavigateToSub = { activeSubScreen = it }
                            )
                            ScreenTab.Todo -> TodoListScreen(viewModel = viewModel)
                            ScreenTab.Usage -> ScreenTimeUsageScreen(viewModel = viewModel)
                            ScreenTab.Insights -> ProfileInsightsScreen(viewModel = viewModel)
                            ScreenTab.Community -> CommunityRankingScreen(viewModel = viewModel)
                        }
                    }
                }
            }

            // Blocker Overlay Dialog
            if (showBlockedAlert) {
                BlockedFocusOverlay(
                    reason = blockedReason,
                    onDismiss = { viewModel.setBlockedAlert(false) },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun SocialSentryBottomNav(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit
) {
    NavigationBar(
        containerColor = CardBg,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(72.dp)
            .border(1.dp, BorderColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        val items = listOf(
            NavigationItem(ScreenTab.Usage, Icons.Outlined.Analytics, Icons.Filled.Analytics, "Usage"),
            NavigationItem(ScreenTab.Todo, Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle, "To-do"),
            NavigationItem(ScreenTab.Home, Icons.Outlined.Home, Icons.Filled.Home, "Home"),
            NavigationItem(ScreenTab.Community, Icons.Outlined.Group, Icons.Filled.Group, "Social"),
            NavigationItem(ScreenTab.Insights, Icons.Outlined.Insights, Icons.Filled.Insights, "Insights")
        )

        items.forEach { item ->
            val isActive = currentTab == item.tab
            NavigationBarItem(
                selected = isActive,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = if (isActive) item.activeIcon else item.inactiveIcon,
                        contentDescription = item.label,
                        tint = if (isActive) BrightMint else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = if (isActive) BrightMint else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = DarkMint
                ),
                modifier = Modifier.testTag("nav_tab_${item.label.lowercase()}")
            )
        }
    }
}

data class NavigationItem(
    val tab: ScreenTab,
    val inactiveIcon: ImageVector,
    val activeIcon: ImageVector,
    val label: String
)

// ==========================================
// SCREEN 1: DASHBOARD HOME
// ==========================================
@Composable
fun DashboardHomeScreen(
    viewModel: SentryViewModel,
    onNavigateToSub: (SubScreen) -> Unit
) {
    val settings by viewModel.settingsMap.collectAsState()
    val isMasterBlocking = settings["master_blocking"]?.toBoolean() ?: true
    val distractedTime = settings["distracted_time_today"] ?: "17"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Bar Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { /* Simulated menu drawer */ }) {
                    Icon(Icons.Default.Menu, "Menu", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Social Sentry",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
            IconButton(
                onClick = { onNavigateToSub(SubScreen.AccountSettingsDetails) },
                modifier = Modifier.testTag("profile_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PinkAccent, CoralRed)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, "Profile", tint = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Screen Time Info Cards Header Row (Screen 1 & 6)
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "1h 38m",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total Screen Time",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(BorderColor)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${distractedTime}m",
                        color = CoralRed,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Distracting Time",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Center Pulsing Shield / Power Toggle for Blocker
        InteractivePulsingSentryButton(
            enabled = isMasterBlocking,
            onToggle = { viewModel.toggleMasterBlocking() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: REDUCE USAGE
        Text(
            text = "REDUCE USAGE",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureBlockCard(
                title = "Reels Blocker",
                icon = Icons.Default.Videocam,
                accentColor = EmeraldMint,
                isActive = settings["block_instagram"]?.toBoolean() == true || settings["block_youtube"]?.toBoolean() == true,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToSub(SubScreen.ReelsBlockerSettings) }
            )

            FeatureBlockCard(
                title = "App Limit",
                icon = Icons.Default.HourglassEmpty,
                accentColor = PinkAccent,
                isActive = true,
                hasDot = true,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToSub(SubScreen.AppLimitSettings) }
            )

            FeatureBlockCard(
                title = "Porn Blocker",
                icon = Icons.Default.Warning,
                accentColor = CyanAccent,
                isActive = settings["porn_blocking"]?.toBoolean() == true,
                hasDot = true,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToSub(SubScreen.PornBlockerDetails) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION: MORE FEATURES
        Text(
            text = "MORE FEATURES",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Start
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureBlockCard(
                title = "Safety Mode",
                icon = Icons.Default.Lock,
                accentColor = PinkAccent,
                isActive = settings["safety_mode"]?.toBoolean() == true,
                hasDot = true,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToSub(SubScreen.SafetyModeDetails) }
            )

            FeatureBlockCard(
                title = "Scroll Limit",
                icon = Icons.Default.Timer,
                accentColor = EmeraldMint,
                isActive = settings["scroll_limit"]?.toBoolean() == true,
                hasDot = true,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToSub(SubScreen.ScrollLimitDetails) }
            )

            FeatureBlockCard(
                title = "App Tutorial",
                icon = Icons.Default.PlayCircle,
                accentColor = CyanAccent,
                isActive = false,
                hasDot = true,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToSub(SubScreen.AppTutorialDetails) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun InteractivePulsingSentryButton(
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (enabled) 1.15f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (enabled) 0.6f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(220.dp)
            .testTag("sentry_power_toggle")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
    ) {
        // Outer pulsing ring 2
        Box(
            modifier = Modifier
                .size(200.dp)
                .drawBehind {
                    val multiplier = pulseScale
                    drawCircle(
                        color = if (enabled) EmeraldMint else TextSecondary,
                        radius = size.minDimension / 2 * multiplier,
                        style = Stroke(width = 3.dp.toPx()),
                        alpha = glowAlpha * 0.4f
                    )
                }
        )

        // Outer pulsing ring 1
        Box(
            modifier = Modifier
                .size(160.dp)
                .drawBehind {
                    val multiplier = (pulseScale + 1.0f) / 2
                    drawCircle(
                        color = if (enabled) EmeraldMint else TextSecondary,
                        radius = size.minDimension / 2 * multiplier,
                        style = Stroke(width = 4.dp.toPx()),
                        alpha = glowAlpha * 0.7f
                    )
                }
        )

        // Main Core Circle
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (enabled) {
                            listOf(BrightMint, DarkMint)
                        } else {
                            listOf(TextSecondary, CardBg)
                        }
                    )
                )
                .border(2.dp, if (enabled) TextPrimary else BorderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (enabled) Icons.Default.Close else Icons.Default.PowerSettingsNew,
                    contentDescription = if (enabled) "Blocking ON" else "Blocking OFF",
                    tint = SlateBlack,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

@Composable
fun FeatureBlockCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    isActive: Boolean,
    hasDot: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isActive) accentColor.copy(alpha = 0.4f) else BorderColor),
        modifier = modifier
            .height(115.dp)
            .clickable(onClick = onClick)
            .testTag("feature_card_${title.lowercase().replace(" ", "_")}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // notification red dot
            if (hasDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CoralRed)
                        .align(Alignment.TopEnd)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                // Circular icon frame
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) accentColor.copy(alpha = 0.15f) else BorderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isActive) accentColor else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ==========================================
// SCREEN 2: TO-DO LIST SCREEN (Calendar)
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoListScreen(viewModel: SentryViewModel) {
    val todos by viewModel.allTodos.collectAsState()
    val selectedDateStr by viewModel.selectedDateStr.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }

    // Filter todos
    val filteredTodos = todos.filter {
        it.dateString == selectedDateStr && (selectedCategory == "All" || it.category == selectedCategory)
    }

    val doneCount = filteredTodos.count { it.isCompleted }
    val totalCount = filteredTodos.size

    val calendarDays = remember(selectedDateStr) {
        generateCalendarDays(selectedDateStr)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "To-do List",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.Person, "Profile", tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Month Indicator Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getMonthNameString(selectedDateStr),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Done Ratio Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkMint)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$doneCount/$totalCount done",
                    color = BrightMint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Week Calendar
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(calendarDays) { day ->
                val dateStr = day.dateString
                val isSelected = dateStr == selectedDateStr
                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) EmeraldMint else CardBg)
                        .border(1.dp, if (isSelected) BrightMint else BorderColor, RoundedCornerShape(16.dp))
                        .clickable { viewModel.selectDate(dateStr) }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = day.dayLabel,
                        color = if (isSelected) SlateBlack else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = day.dayNumber,
                        color = if (isSelected) SlateBlack else TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (day.hasItems) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) SlateBlack else CoralRed)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Category pills
        val categories = listOf("All", "Personal", "Work", "Health", "Shopping")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) EmeraldMint else CardBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { viewModel.selectCategory(category) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) SlateBlack else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Output Display
        Box(modifier = Modifier.weight(1f)) {
            if (filteredTodos.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = "No tasks",
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Clean slate! No tasks today.",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Build discipline, add focused schedules.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val lineX = 48.dp.toPx()
                            drawLine(
                                color = CoralRed.copy(alpha = 0.4f),
                                start = Offset(lineX, 0f),
                                end = Offset(lineX, size.height),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                ) {
                    items(filteredTodos) { item ->
                        TodoTimelineItem(
                            item = item,
                            onCompleteToggle = { viewModel.toggleTodoCompletion(item.id, it) },
                            onDelete = { viewModel.deleteTodo(item.id) }
                        )
                    }
                }
            }

            // FLOATING ADD BUTTON
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 8.dp)
            ) {
                FloatingActionButton(
                    onClick = { showAddTaskDialog = true },
                    containerColor = EmeraldMint,
                    contentColor = SlateBlack,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("add_todo_fab")
                ) {
                    Icon(Icons.Default.Add, "Add Schedule", modifier = Modifier.size(28.dp))
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddScheduleDialog(
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, desc, time, dur, cat, icon ->
                viewModel.addTodo(title, desc, time, dur, cat, icon)
                showAddTaskDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoTimelineItem(
    item: TodoItem,
    onCompleteToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("todo_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.timeString,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(42.dp),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .width(44.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PinkAccent.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
                .border(1.dp, PinkAccent.copy(alpha = 0.3f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getTodoIcon(item.iconName),
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onDelete
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = if (item.isCompleted) TextSecondary else TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (item.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    )
                    Text(
                        text = item.subtitle,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(
                    onClick = { onCompleteToggle(!item.isCompleted) },
                    modifier = Modifier.testTag("todo_check_${item.id}")
                ) {
                    Icon(
                        imageVector = if (item.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = "Check",
                        tint = if (item.isCompleted) EmeraldMint else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

data class CalendarDayHolder(
    val dateString: String,
    val dayLabel: String,
    val dayNumber: String,
    val hasItems: Boolean = false
)

private fun generateCalendarDays(selectedDateStr: String): List<CalendarDayHolder> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val selectedDate = try {
        sdf.parse(selectedDateStr) ?: Date()
    } catch (e: Exception) {
        Date()
    }

    val cal = Calendar.getInstance()
    cal.time = selectedDate

    val list = mutableListOf<CalendarDayHolder>()
    val tempCal = Calendar.getInstance()
    tempCal.time = selectedDate
    tempCal.add(Calendar.DAY_OF_YEAR, -3)

    val labelFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val numberFormat = SimpleDateFormat("d", Locale.getDefault())

    for (i in 0 until 7) {
        val dateString = sdf.format(tempCal.time)
        val dayLabel = labelFormat.format(tempCal.time).uppercase()
        val dayNumber = numberFormat.format(tempCal.time)
        list.add(CalendarDayHolder(dateString, dayLabel, dayNumber, true))
        tempCal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return list
}

private fun getMonthNameString(dateString: String): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return try {
        val d = sdf.parse(dateString) ?: Date()
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d)
    } catch (e: Exception) {
        "This Month"
    }
}

private fun getTodoIcon(name: String): ImageVector {
    return when (name.lowercase()) {
        "bell" -> Icons.Default.Notifications
        "school" -> Icons.Default.School
        "fitness" -> Icons.Default.FitnessCenter
        "pencil" -> Icons.Default.Edit
        "work" -> Icons.Default.Work
        else -> Icons.Default.Check
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var timeString by remember { mutableStateOf("08:00") }
    var durationMinutes by remember { mutableStateOf("60") }
    var category by remember { mutableStateOf("Work") }
    var iconName by remember { mutableStateOf("bell") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Focused Schedule",
                    fontSize = 18.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldMint,
                        focusedLabelColor = EmeraldMint,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_todo_title_input")
                )

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Sub description") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldMint,
                        focusedLabelColor = EmeraldMint,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = timeString,
                        onValueChange = { timeString = it },
                        label = { Text("Time (HH:MM)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldMint,
                            focusedLabelColor = EmeraldMint,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = { durationMinutes = it },
                        label = { Text("Mins") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldMint,
                            focusedLabelColor = EmeraldMint,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Category", fontSize = 12.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Personal", "Work", "Health", "Shopping").forEach { cat ->
                            val active = category == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) EmeraldMint else CardBg)
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { category = cat }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    color = if (active) SlateBlack else TextSecondary
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Icon Identifier", fontSize = 12.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("bell", "school", "fitness", "pencil", "work").forEach { icon ->
                            val active = iconName == icon
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) EmeraldMint else CardBg)
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { iconName = icon }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getTodoIcon(icon),
                                    contentDescription = icon,
                                    tint = if (active) SlateBlack else TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (title.isNotEmpty()) {
                                onSave(
                                    title,
                                    subtitle,
                                    timeString,
                                    durationMinutes.toIntOrNull() ?: 60,
                                    category,
                                    iconName
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldMint),
                        modifier = Modifier.testTag("submit_todo_button")
                    ) {
                        Text("Save Task", color = SlateBlack)
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: PROFILE INSIGHTS (Charts/Radar Canvas)
// ==========================================
@Composable
fun ProfileInsightsScreen(viewModel: SentryViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Profile Insights",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Person, "Profile", tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats (Left)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(210.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Stats",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        RadarChartCanvas()
                    }
                }
            }

            // Skill Points (Right)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(210.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Skill Points",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val skills = listOf(
                        SkillData("Writing", 0.72f),
                        SkillData("Financial", 0.65f),
                        SkillData("Learning", 0.80f),
                        SkillData("Coding", 0.45f)
                    )

                    skills.forEach { skill ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(skill.name, fontSize = 11.sp, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { skill.progress },
                                color = EmeraldMint,
                                trackColor = BorderColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Activity (Left)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1.1f)
                    .height(210.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Activity",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val days = listOf("Wed", "Thu", "Fri", "Today")
                    val fractions = listOf(0.35f, 0.7f, 1.0f, 0.36f)
                    val values = listOf("1h37m", "3h37m", "5h8m", "1h38m")

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        days.forEachIndexed { i, label ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(values[i], fontSize = 10.sp, color = TextPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .fillMaxHeight(fractions[i] * 0.7f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(EmeraldMint)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(label, fontSize = 9.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // XP Goal (Right)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(0.9f)
                    .height(210.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "97% XP Goal",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    CircularProgressIndicator(
                        progress = { 0.97f },
                        color = EmeraldMint,
                        trackColor = BorderColor,
                        strokeWidth = 10.dp,
                        modifier = Modifier.size(72.dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "1948 / 2000",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "XP points accumulated",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Full Width card: Contribution Grid
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Focus Grid Tracker",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Dec - Mar",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val shades = listOf(
                        SlateBlack,
                        Color(0xFF042F1A),
                        Color(0xFF0D5E34),
                        Color(0xFF10B981),
                        Color(0xFF00FF9D)
                    )
                    
                    for (row in 0 until 5) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (col in 0 until 24) {
                                val seed = (row * 31 + col * 17) % 63
                                val index = if (seed % 7 == 0) 0 else if (seed % 3 == 0) 1 else if (seed % 5 == 0) 2 else if (seed % 9 == 0) 4 else 3
                                val color = shades[index]
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(color)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Less", fontSize = 9.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    listOf(SlateBlack, Color(0xFF0D5E34), Color(0xFF10B981), Color(0xFF00FF9D)).forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(c)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text("More", fontSize = 9.sp, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RadarChartCanvas() {
    Canvas(modifier = Modifier.size(110.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.width / 2

        for (gridIndex in 1..3) {
            val r = maxRadius * (gridIndex / 3.0f)
            val path = Path()
            for (i in 0 until 5) {
                val angleRad = Math.toRadians((i * 72 - 90).toDouble())
                val x = centerX + r * cos(angleRad).toFloat()
                val y = centerY + r * sin(angleRad).toFloat()
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()
            drawPath(
                path = path,
                color = BorderColor,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        for (i in 0 until 5) {
            val angleRad = Math.toRadians((i * 72 - 90).toDouble())
            val x = centerX + maxRadius * cos(angleRad).toFloat()
            val y = centerY + maxRadius * sin(angleRad).toFloat()
            drawLine(
                color = BorderColor.copy(alpha = 0.5f),
                start = Offset(centerX, centerY),
                end = Offset(x, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val values = listOf(0.85f, 0.65f, 0.90f, 0.45f, 0.75f)
        val dataPath = Path()
        for (i in 0 until 5) {
            val r = maxRadius * values[i]
            val angleRad = Math.toRadians((i * 72 - 90).toDouble())
            val x = centerX + r * cos(angleRad).toFloat()
            val y = centerY + r * sin(angleRad).toFloat()
            if (i == 0) {
                dataPath.moveTo(x, y)
            } else {
                dataPath.lineTo(x, y)
            }
        }
        dataPath.close()

        drawPath(
            path = dataPath,
            color = EmeraldMint.copy(alpha = 0.25f)
        )
        drawPath(
            path = dataPath,
            color = BrightMint,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

data class SkillData(val name: String, val progress: Float)

// ==========================================
// SCREEN 4: PORN BLOCKER DETAILS
// ==========================================
@Composable
fun PornBlockerScreenDetails(
    viewModel: SentryViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settingsMap.collectAsState()
    val pornBlockingEnabled = settings["porn_blocking"]?.toBoolean() ?: true
    val streakTicker by viewModel.pornStreakTicker.collectAsState()

    val currentDayStreak = streakTicker.days

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Porn Blocker",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = {}) {
                Icon(Icons.Filled.Settings, "Config", tint = TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CyanAccent.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                        .border(1.dp, CyanAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Chad Profile",
                        tint = CyanAccent,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DISCIPLINED",
                    color = CyanAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "CURRENT RANK",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(230.dp)
        ) {
            CircularProgressIndicator(
                progress = { ((currentDayStreak % 30) / 30.0f).coerceIn(0f, 1f) },
                color = CyanAccent,
                trackColor = BorderColor,
                strokeWidth = 14.dp,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${streakTicker.days}",
                    color = TextPrimary,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "DAYS",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Text(
                    text = String.format(
                        Locale.getDefault(),
                        "%02d:%02d:%02d",
                        streakTicker.hours,
                        streakTicker.minutes,
                        streakTicker.seconds
                    ),
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Master Porn Blocker", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Blocks adult sites in browsers automatically", color = TextSecondary, fontSize = 11.sp)
                }

                Switch(
                    checked = pornBlockingEnabled,
                    onCheckedChange = { viewModel.togglePornBlocking() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BrightMint,
                        checkedTrackColor = DarkMint
                    ),
                    modifier = Modifier.testTag("porn_block_toggle")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.resetPornStreak() },
            colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("reset_streak_button")
        ) {
            Icon(Icons.Default.Refresh, "Relapse Reset", tint = TextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("I Relapsed (Reset Streak)", color = TextPrimary, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ==========================================
// SCREEN 5: ACCOUNT & SETTINGS SCREEN
// ==========================================
@Composable
fun AccountSettingsScreen(
    viewModel: SentryViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settingsMap.collectAsState()
    val karma = settings["user_karma"] ?: "853"
    val name = settings["user_name"] ?: "MK Shaon"
    val handle = settings["user_handle"] ?: "@mkshaon7"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Account & Settings",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CoralRed, Color(0xFFF97316), Color(0xFFEAB308))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Face, "Chad Avatar", tint = TextPrimary, modifier = Modifier.size(44.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(name, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(handle, color = LightGray, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Disciplined Rank", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    val progressFraction = (karma.toFloatOrNull() ?: 853f) / 1500f
                    LinearProgressIndicator(
                        progress = { progressFraction.coerceIn(0f, 1f) },
                        color = BrightMint,
                        trackColor = SlateBlack.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("User current karma is $karma xp gold", color = LightGray, fontSize = 11.sp)
                        Text("Target: 1500", color = LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("QUICK ACCESS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        SettingsMenuItem("Community Group", Icons.Default.Group, onClick = {})
        SettingsMenuItem("Global Rankings", Icons.Default.EmojiEvents, onClick = {})
        SettingsMenuItem("Report a Relapse Bug", Icons.Default.BugReport, onClick = {})
        SettingsMenuItem("What's New in v2.8", Icons.Default.Info, onClick = {})

        Spacer(modifier = Modifier.height(16.dp))

        Text("APP INFO", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = EmeraldMint)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("App Version", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("v2.8.93 Stable", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingsMenuItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
        }
    }
}

// ==========================================
// SCREEN 6: SCREEN TIME & USAGE LIST
// ==========================================
@Composable
fun ScreenTimeUsageScreen(viewModel: SentryViewModel) {
    val dailyUsages by viewModel.dailyUsages.collectAsState()
    val appUsages by viewModel.appUsages.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Screen Time",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.Person, "Profile", tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("1h 38m", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Total Screen Time", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Box(modifier = Modifier.width(1.dp).height(40.dp).background(BorderColor))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("17m", color = CoralRed, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Distracting Time", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Last 7 Days", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))

                val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                val hoursLabel = listOf("4h 2m", "5h 23m", "2h 15m", "1h 37m", "3h 37m", "5h 8m", "1h 38m")
                val heights = listOf(0.75f, 1.0f, 0.42f, 0.3f, 0.67f, 0.95f, 0.3f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weekdays.forEachIndexed { i, day ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = hoursLabel[i],
                                fontSize = 8.sp,
                                color = if (day == "Sat") BrightMint else TextSecondary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .fillMaxHeight(heights[i] * 0.8f)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (day == "Sat") {
                                            Brush.verticalGradient(listOf(BrightMint, DarkMint))
                                        } else {
                                            Brush.verticalGradient(listOf(EmeraldMint.copy(alpha = 0.8f), EmeraldMint.copy(alpha = 0.3f)))
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(day, fontSize = 10.sp, color = if (day == "Sat") BrightMint else TextSecondary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Most Used Apps - Today", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            appUsages.forEach { app ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (app.isDistracting) CoralRed.copy(alpha = 0.15f) else EmeraldMint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (app.isDistracting) Icons.Filled.Block else Icons.Filled.Send,
                                    contentDescription = app.appName,
                                    tint = if (app.isDistracting) CoralRed else EmeraldMint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.appName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                val fraction = (app.usageMinutes.toFloat() / 65f).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    color = if (app.isDistracting) CoralRed else EmeraldMint,
                                    trackColor = BorderColor,
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                            }
                        }

                        Text("${app.usageMinutes}m", color = if (app.isDistracting) CoralRed else BrightMint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ==========================================
// SUB SCREENS SELECTOR
// ==========================================
@Composable
fun RenderSubScreen(
    subScreen: SubScreen,
    viewModel: SentryViewModel,
    onBack: () -> Unit
) {
    when (subScreen) {
        SubScreen.ReelsBlockerSettings -> ReelsBlockerSettingsScreen(viewModel, onBack)
        SubScreen.PornBlockerDetails -> PornBlockerScreenDetails(viewModel, onBack)
        SubScreen.AppLimitSettings -> AppLimitSettingsScreen(viewModel, onBack)
        SubScreen.SafetyModeDetails -> SafetyModeScreenDetails(viewModel, onBack)
        SubScreen.ScrollLimitDetails -> ScrollLimitScreenDetails(viewModel, onBack)
        SubScreen.AppTutorialDetails -> AppTutorialScreenDetails(onBack)
        SubScreen.AccountSettingsDetails -> AccountSettingsScreen(viewModel, onBack)
    }
}

@Composable
fun ReelsBlockerSettingsScreen(
    viewModel: SentryViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settingsMap.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reels & Shorts Blocker", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkMint.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, EmeraldMint.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Shield, null, tint = EmeraldMint, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Social Sentry monitors the view nodes of the checked social apps below. When you scroll into a Reel or Short video, it automatically registers a back gesture to close it, keeping you focused, 100% locally.",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Text("SELECT APPS TO BLOCK", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        AppBlockConfigItem("Instagram Reels", "com.instagram.android", settings["block_instagram"]?.toBoolean() ?: true, onCheckedChange = { viewModel.toggleBlockInstagram() })
        AppBlockConfigItem("YouTube Shorts", "com.google.android.youtube", settings["block_youtube"]?.toBoolean() ?: true, onCheckedChange = { viewModel.toggleBlockYoutube() })
        AppBlockConfigItem("TikTok App", "com.zhiliaoapp.musically", settings["block_tiktok"]?.toBoolean() ?: true, onCheckedChange = { viewModel.toggleBlockTiktok() })
        AppBlockConfigItem("Facebook Reels", "com.facebook.katana", settings["block_facebook"]?.toBoolean() ?: true, onCheckedChange = { viewModel.toggleBlockFacebook() })

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AppBlockConfigItem(
    label: String,
    pkg: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(label, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(pkg, color = TextSecondary, fontSize = 11.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BrightMint,
                    checkedTrackColor = DarkMint
                ),
                modifier = Modifier.testTag("toggle_switch_${label.lowercase().replace(" ", "_")}")
            )
        }
    }
}

@Composable
fun AppLimitSettingsScreen(viewModel: SentryViewModel, onBack: () -> Unit) {
    var limitMinutes by remember { mutableStateOf("45") }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary) }
            Text("App Usage Limits", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(30.dp))
        Icon(Icons.Default.HourglassBottom, null, tint = PinkAccent, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Daily Application Timeout", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Deter screen addiction by adding custom limit blockers.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = limitMinutes,
            onValueChange = { limitMinutes = it },
            label = { Text("Daily limit for distracting apps (minutes)") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PinkAccent, focusedLabelColor = PinkAccent, unfocusedBorderColor = BorderColor),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { onBack() },
            colors = ButtonDefaults.buttonColors(containerColor = PinkAccent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set Active App Limit", color = SlateBlack, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SafetyModeScreenDetails(viewModel: SentryViewModel, onBack: () -> Unit) {
    val settings by viewModel.settingsMap.collectAsState()
    val active = settings["safety_mode"]?.toBoolean() ?: false
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary) }
            Text("Safety Anti-Relapse Mode", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(30.dp))
        Icon(Icons.Default.Lock, null, tint = PinkAccent, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Prevention Settings Lock", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Prevents disabling the blocker during weak moments of temptation.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(30.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Safety Lock ACTIVE", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Cannot edit blocker configs", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(checked = active, onCheckedChange = { viewModel.toggleSafetyMode() })
            }
        }
    }
}

@Composable
fun ScrollLimitScreenDetails(viewModel: SentryViewModel, onBack: () -> Unit) {
    val settings by viewModel.settingsMap.collectAsState()
    val active = settings["scroll_limit"]?.toBoolean() ?: true
    val mins = settings["scroll_limit_min"] ?: "15"
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary) }
            Text("Scroll Time Limit", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(30.dp))
        Icon(Icons.Default.Timer, null, tint = EmeraldMint, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Dopamine Overload Block", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Configurability to prevent continuous swipes after some minutes of feed scrolls.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Enable Feed Scroll Limit ($mins Mins)", color = TextPrimary, fontWeight = FontWeight.Bold)
                Switch(checked = active, onCheckedChange = { viewModel.toggleScrollLimit() })
            }
        }
    }
}

@Composable
fun AppTutorialScreenDetails(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary) }
            Text("Social Sentry Tutorial", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("How to Setup Social Sentry", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())

        val steps = listOf(
            "1. Grant Accessibility Services Permission" to "Go to System Settings -> Accessibility -> Social Sentry and enable it. This lets us local scan. No personal data ever leaves your device.",
            "2. Toggle Master Blocker ON" to "Tap the big green circle on the home dashboard to initiate real-time active coverage.",
            "3. Choose target apps" to "Click Reels Blocker card and toggle apps (Instagram, YouTube, TikTok, Facebook) to block short videos selectively.",
            "4. Stay clean in browsers" to "Keep Porn Blocker active to automatically stop NSFW urges in Chrome or Firefox browser."
        )

        steps.forEachIndexed { i, step ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(step.first, color = EmeraldMint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(step.second, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

// ==========================================
// SIMULATED RANKING COMMUNITY SOCIALS
// ==========================================
@Composable
fun CommunityRankingScreen(viewModel: SentryViewModel) {
    val settings by viewModel.settingsMap.collectAsState()
    val karma = settings["user_karma"] ?: "853"
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Focus Sentry Group", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Group members sustaining streaks and focus levels", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(20.dp))

        val members = listOf(
            SentryMember("AlphaFocus", "38 Days Clean", "1850 XP", true),
            SentryMember("FocusDojo", "20 Days Clean", "1220 XP", false),
            SentryMember("HabitHero", "15 Days Clean", "940 XP", false),
            SentryMember("MK Shaon", "14 Days Clean", "$karma XP", false, isSelf = true),
            SentryMember("DopamineDetoxer", "8 Days Clean", "420 XP", false)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(members) { mem ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (mem.isSelf) CardBg.copy(alpha = 1.3f) else CardBg),
                    border = BorderStroke(1.dp, if (mem.isSelf) BrightMint.copy(alpha = 0.5f) else BorderColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (mem.isLeader) CoralRed.copy(alpha = 0.15f) else EmeraldMint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (mem.isLeader) Icons.Filled.Star else Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = if (mem.isLeader) CoralRed else EmeraldMint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(mem.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (mem.isSelf) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(DarkMint).padding(horizontal = 4.dp, vertical = 1.dp)) {
                                            Text("YOU", color = BrightMint, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(mem.streak, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        Text(mem.xp, color = if (mem.isSelf) BrightMint else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

data class SentryMember(
    val name: String,
    val streak: String,
    val xp: String,
    val isLeader: Boolean,
    val isSelf: Boolean = false
)

// ==========================================
// BLOCK SCREEN OVERLAY
// ==========================================
@Composable
fun BlockedFocusOverlay(
    reason: String,
    onDismiss: () -> Unit,
    viewModel: SentryViewModel
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateBlack),
            border = BorderStroke(2.dp, BrightMint),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("blocked_dialog_overlay")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CoralRed.copy(alpha = 0.15f))
                        .border(1.dp, CoralRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Blocked Alert",
                        tint = CoralRed,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "🚫 STOP THE SCROLL! 🚫",
                    color = CoralRed,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Social Sentry detected and closed an active session of $reason.",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                val motivationQuotes = listOf(
                    "\"The path of discipline is painful, but temporary, while the pain of regret is permanent.\"",
                    "\"Don't trade your long-term focus goals for 15 seconds of cheap dopamine swipes.\"",
                    "\"You are stronger than a social media recommendation algorithm. Stay disciplined, Chad.\"",
                    "\"Every single urge you overcome strengthens your mind. Keep the streak going.\"",
                    "\"Focus on what matters. Build your coding skill, build your life!\""
                )
                
                val quoteIndex = (System.currentTimeMillis() / 60000 % motivationQuotes.size).toInt()
                val activeQuote = motivationQuotes[quoteIndex]

                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = activeQuote,
                        color = BrightMint,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.addTodo("Urge Defeated Task", "Added after urge blocker", "FocusMins", 30, "Work", "pencil")
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkMint),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Urge Task", color = BrightMint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = BrightMint),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back to Focus", color = SlateBlack, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
