package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.CustomIconPack
import com.example.data.model.GlassThemeMode
import com.example.data.repository.GlassDashboardRepository
import com.example.ui.components.AeroIconGlyph
import com.example.ui.components.CustomGlassIcon
import com.example.ui.components.GlassCard
import com.example.ui.components.glassDockShelf
import com.example.ui.components.glassPill
import com.example.ui.components.glassmorphism
import com.example.ui.screens.AddTaskModalSheet
import com.example.ui.screens.AppDrawerScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardHomePage
import com.example.ui.screens.DiscoverFeedPage
import com.example.ui.screens.LauncherSettingsModalSheet
import com.example.ui.screens.LauncherShortcutsAndWidgetsPage
import com.example.ui.screens.launchBrowser
import com.example.ui.screens.launchCamera
import com.example.ui.screens.launchMessages
import com.example.ui.screens.launchPhone
import com.example.ui.screens.signOut
import com.example.ui.theme.AeroGlassTheme
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.viewmodel.GlassDashboardViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AeroGlassRootApp()
        }
    }
}

@Composable
fun AeroGlassRootApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var isGuestMode by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                isGuestMode = false
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null && !isGuestMode) {
        AeroGlassTheme {
            AuthScreen(
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    isGuestMode = false
                },
                onContinueAsGuest = {
                    isGuestMode = true
                }
            )
        }
    } else {
        val uid = currentUser?.uid ?: "guest_local"
        val isGuestSession = currentUser == null
        key(uid, isGuestSession) {
            AuthenticatedGlassWorkspace(
                currentUserId = uid,
                isGuestSession = isGuestSession,
                onExitGuestMode = { isGuestMode = false }
            )
        }
    }
}

@Composable
fun AuthenticatedGlassWorkspace(
    currentUserId: String,
    isGuestSession: Boolean = false,
    onExitGuestMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: GlassDashboardViewModel = viewModel(
        key = "${currentUserId}_${isGuestSession}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                GlassDashboardViewModel(
                    repository = GlassDashboardRepository(db),
                    currentUserId = currentUserId,
                    isGuestSession = isGuestSession,
                    appContext = app
                )
            }
        }
    )

    val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferencesState.collectAsStateWithLifecycle()
    val weather by viewModel.weatherState.collectAsStateWithLifecycle()
    val news by viewModel.newsState.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetryState.collectAsStateWithLifecycle()
    val quickSettings by viewModel.quickSettings.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    val rawTasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val activeRemindersCount = remember(rawTasks) {
        rawTasks.count { it.reminderEnabled && !it.completed }
    }
    val pendingTasksCount = remember(rawTasks) {
        rawTasks.count { !it.completed }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshTelemetry(context)
            delay(5000L)
        }
    }

    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(3500L)
            viewModel.clearStatusBanner()
        }
    }

    val themeMode = remember(preferences.themeMode) {
        runCatching { GlassThemeMode.valueOf(preferences.themeMode) }
            .getOrDefault(GlassThemeMode.DARK_GLASS)
    }
    val iconPack = remember(preferences.iconPack) {
        runCatching { CustomIconPack.valueOf(preferences.iconPack) }
            .getOrDefault(CustomIconPack.LIQUID_PRISM)
    }

    var isAppDrawerOpen by rememberSaveable { mutableStateOf(false) }
    var isSettingsSheetOpen by rememberSaveable { mutableStateOf(false) }
    var showAddTaskSheet by remember { mutableStateOf(false) }

    // Page 0 = Google Discover (-1 screen), Page 1 = Stock Android Home, Page 2 = Glass Task Cards & Widgets
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

    BackHandler(enabled = isAppDrawerOpen) {
        isAppDrawerOpen = false
    }
    BackHandler(enabled = !isAppDrawerOpen && pagerState.currentPage != 1) {
        scope.launch {
            pagerState.animateScrollToPage(1)
        }
    }

    AeroGlassTheme(
        themeMode = themeMode,
        blurIntensityPercent = preferences.blurIntensity.toInt(),
        iconPack = iconPack,
        parallaxEnabled = preferences.parallaxEnabled
    ) {
        val tokens = LocalGlassTokens.current

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(tokens.bgGradientStart, tokens.bgGradientEnd)
                    )
                )
        ) {
            val isExpandedScreen = maxWidth >= 780.dp

            Image(
                painter = painterResource(id = R.drawable.img_aesthetic_glass_wallpaper_1794159551333),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = if (tokens.isDark) 0.65f else 0.35f,
                modifier = Modifier.fillMaxSize()
            )

            if (isExpandedScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlassSideNavigationRail(
                        currentPage = pagerState.currentPage,
                        isDrawerOpen = isAppDrawerOpen,
                        onSelectPage = { page ->
                            isAppDrawerOpen = false
                            scope.launch { pagerState.animateScrollToPage(page) }
                        },
                        onToggleAppDrawer = { isAppDrawerOpen = !isAppDrawerOpen },
                        onOpenSettings = { isSettingsSheetOpen = true },
                        onOpenNewTask = { showAddTaskSheet = true }
                    )

                    Box(modifier = Modifier.weight(1.15f)) {
                        DashboardHomePage(
                            weather = weather,
                            telemetry = telemetry,
                            parallaxEnabled = preferences.parallaxEnabled,
                            activeRemindersCount = activeRemindersCount,
                            pendingTasksCount = pendingTasksCount,
                            onOpenAppDrawer = { isAppDrawerOpen = true },
                            onOpenDiscoverFeed = { scope.launch { pagerState.animateScrollToPage(0) } },
                            onOpenWidgetsPage = { scope.launch { pagerState.animateScrollToPage(2) } },
                            onOpenSettingsSheet = { isSettingsSheetOpen = true },
                            onQuickRamClean = { viewModel.performRamCleanBoost(context) }
                        )
                    }

                    Box(modifier = Modifier.weight(0.95f)) {
                        when (pagerState.currentPage) {
                            0 -> DiscoverFeedPage(
                                weather = weather,
                                newsArticles = news,
                                selectedCity = preferences.city,
                                onSelectCity = viewModel::updateWeatherCity,
                                onRefreshFeed = { viewModel.refreshWeatherAndNews(preferences.city) }
                            )
                            else -> LauncherShortcutsAndWidgetsPage(
                                tasksUiState = tasksState,
                                selectedCategory = selectedCategory,
                                telemetry = telemetry,
                                lastRamBoostSummary = quickSettings.lastRamBoostSummary,
                                onSelectCategory = viewModel::setCategoryFilter,
                                onToggleTaskComplete = viewModel::toggleTaskCompleted,
                                onToggleTaskReminder = { task ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    viewModel.toggleTaskReminder(context, task)
                                },
                                onDeleteTask = viewModel::deleteTask,
                                onCommitReorderedTasks = viewModel::commitReorderedTasks,
                                onQuickRamClean = { viewModel.performRamCleanBoost(context) },
                                onOpenAddTaskSheet = { showAddTaskSheet = true },
                                onOpenSettingsSheet = { isSettingsSheetOpen = true }
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    // Transient Status Toast Banner (e.g., after RAM boost or Smart Reminder)
                    AnimatedVisibility(
                        visible = statusBanner != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                                .glassmorphism(cornerRadius = 16.dp, elevated = true)
                        ) {
                            Text(
                                text = statusBanner.orEmpty(),
                                style = MaterialTheme.typography.labelLarge,
                                color = tokens.accentPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        when (page) {
                            0 -> DiscoverFeedPage(
                                weather = weather,
                                newsArticles = news,
                                selectedCity = preferences.city,
                                onSelectCity = viewModel::updateWeatherCity,
                                onRefreshFeed = { viewModel.refreshWeatherAndNews(preferences.city) }
                            )
                            1 -> DashboardHomePage(
                                weather = weather,
                                telemetry = telemetry,
                                parallaxEnabled = preferences.parallaxEnabled,
                                activeRemindersCount = activeRemindersCount,
                                pendingTasksCount = pendingTasksCount,
                                onOpenAppDrawer = { isAppDrawerOpen = true },
                                onOpenDiscoverFeed = { scope.launch { pagerState.animateScrollToPage(0) } },
                                onOpenWidgetsPage = { scope.launch { pagerState.animateScrollToPage(2) } },
                                onOpenSettingsSheet = { isSettingsSheetOpen = true },
                                onQuickRamClean = { viewModel.performRamCleanBoost(context) }
                            )
                            else -> LauncherShortcutsAndWidgetsPage(
                                tasksUiState = tasksState,
                                selectedCategory = selectedCategory,
                                telemetry = telemetry,
                                lastRamBoostSummary = quickSettings.lastRamBoostSummary,
                                onSelectCategory = viewModel::setCategoryFilter,
                                onToggleTaskComplete = viewModel::toggleTaskCompleted,
                                onToggleTaskReminder = { task ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    viewModel.toggleTaskReminder(context, task)
                                },
                                onDeleteTask = viewModel::deleteTask,
                                onCommitReorderedTasks = viewModel::commitReorderedTasks,
                                onQuickRamClean = { viewModel.performRamCleanBoost(context) },
                                onOpenAddTaskSheet = { showAddTaskSheet = true },
                                onOpenSettingsSheet = { isSettingsSheetOpen = true }
                            )
                        }
                    }
                }

                // Authentic Stock Android / Pixel Launcher Pinned Bottom Zone:
                // Page Dots + 5-Icon Dock + Bottom Google Search Bar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    StockAndroidLauncherBottomZone(
                        currentPage = pagerState.currentPage,
                        isDrawerOpen = isAppDrawerOpen,
                        onSelectPage = { page ->
                            isAppDrawerOpen = false
                            scope.launch { pagerState.animateScrollToPage(page) }
                        },
                        onToggleAppDrawer = { isAppDrawerOpen = !isAppDrawerOpen },
                        onOpenSettings = { isSettingsSheetOpen = true }
                    )
                }
            }

            // Slide-Up App Drawer Overlay with Frosted Glass Blur Backdrop
            AnimatedVisibility(
                visible = isAppDrawerOpen,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 240)
                ) + fadeOut(),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(50f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (tokens.isDark) Color(0xD80A0C12)
                            else Color(0xD8EEF2F6)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // Stock Android App Drawer Pull-Down Pill Handle
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .glassPill()
                                    .clickable { isAppDrawerOpen = false }
                                    .padding(horizontal = 18.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Close App Drawer",
                                        tint = tokens.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "All Apps",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tokens.textPrimary
                                    )
                                }
                            }
                        }

                        AppDrawerScreen(
                            onNavigateBack = { isAppDrawerOpen = false }
                        )
                    }
                }
            }

            // Launcher Settings Modal Bottom Sheet
            if (isSettingsSheetOpen) {
                LauncherSettingsModalSheet(
                    preferences = preferences,
                    telemetry = telemetry,
                    isGuestSession = isGuestSession,
                    onDismiss = { isSettingsSheetOpen = false },
                    onRunRamCleanBoost = { viewModel.performRamCleanBoost(context) },
                    onCyclePowerProfile = { viewModel.cyclePowerProfile(context) },
                    onBlurIntensityChanged = { viewModel.updateBlurIntensity(it.toInt()) },
                    onSelectThemeMode = viewModel::updateThemeMode,
                    onSelectIconPack = viewModel::updateIconPack,
                    onToggleParallax = viewModel::toggleParallax,
                    onSignOut = {
                        isSettingsSheetOpen = false
                        if (isGuestSession) {
                            onExitGuestMode()
                        } else {
                            signOut(credentialManager, scope)
                        }
                    }
                )
            }

            // Add Task Sheet
            if (showAddTaskSheet) {
                AddTaskModalSheet(
                    onDismiss = { showAddTaskSheet = false },
                    onCreateTask = { title, notes, category, priority, reminderLabel, reminderEnabled ->
                        if (reminderEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.addTask(
                            context = context,
                            title = title,
                            notes = notes,
                            category = category,
                            priority = priority,
                            reminderLabel = reminderLabel,
                            reminderEnabled = reminderEnabled
                        )
                        showAddTaskSheet = false
                    }
                )
            }
        }
    }
}

/**
 * Authentic Stock Android / Pixel Launcher Bottom Zone:
 * - Swipe-up chevron + 3 page indicator dots
 * - 5-Icon Pinned Favorites Dock inside a `.glassDockShelf()`
 * - Bottom Google Search Bar Pill (`.glassPill()`) with G logo, Voice Mic, and Launcher Settings trigger
 */
@Composable
private fun StockAndroidLauncherBottomZone(
    currentPage: Int,
    isDrawerOpen: Boolean,
    onSelectPage: (Int) -> Unit,
    onToggleAppDrawer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current
    var dockSwipeAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dockSwipeAccumulator += dragAmount
                        if (dockSwipeAccumulator < -28f) {
                            dockSwipeAccumulator = 0f
                            if (!isDrawerOpen) onToggleAppDrawer()
                        }
                    },
                    onDragEnd = { dockSwipeAccumulator = 0f },
                    onDragCancel = { dockSwipeAccumulator = 0f }
                )
            }
            .testTag("floating_glass_dock")
    ) {
        // Swipe-up Chevron & Page Indicator Dots (Discover = 0, Home = 1, Tasks/Widgets = 2)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = onToggleAppDrawer)
                .padding(bottom = 6.dp)
                .testTag("swipe_up_apps_trigger")
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Swipe up for All Apps",
                tint = tokens.textSecondary.copy(alpha = 0.75f),
                modifier = Modifier.size(16.dp)
            )
            repeat(3) { idx ->
                val active = currentPage == idx
                Box(
                    modifier = Modifier
                        .width(if (active) 16.dp else 6.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) tokens.accentPrimary
                            else tokens.textSecondary.copy(alpha = 0.40f)
                        )
                        .clickable { onSelectPage(idx) }
                )
            }
        }

        // Fixed 5-Icon Stock Android Dock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassDockShelf(cornerRadius = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Phone Dialer
                StockDockAppIcon(
                    glyph = AeroIconGlyph.PHONE,
                    label = "Phone",
                    containerBrush = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                    glyphTint = Color.White,
                    onClick = { launchPhone(context) },
                    testTag = "dock_app_phone"
                )

                // 2. Messages (SMS)
                StockDockAppIcon(
                    glyph = AeroIconGlyph.MESSAGES,
                    label = "Messages",
                    containerBrush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))),
                    glyphTint = Color.White,
                    onClick = { launchMessages(context) },
                    testTag = "dock_app_messages"
                )

                // 3. Center App Drawer Trigger
                StockDockAppIcon(
                    glyph = if (isDrawerOpen) AeroIconGlyph.DASHBOARD else AeroIconGlyph.APPS_DRAWER,
                    label = if (isDrawerOpen) "Home" else "Apps",
                    containerBrush = Brush.linearGradient(
                        listOf(
                            tokens.accentPrimary.copy(alpha = 0.38f),
                            tokens.accentSecondary.copy(alpha = 0.28f)
                        )
                    ),
                    glyphTint = tokens.accentPrimary,
                    isCenterKey = true,
                    onClick = onToggleAppDrawer,
                    testTag = "dock_app_drawer"
                )

                // 4. Camera
                StockDockAppIcon(
                    glyph = AeroIconGlyph.CAMERA,
                    label = "Camera",
                    containerBrush = Brush.linearGradient(listOf(Color(0xFF475569), Color(0xFF1E293B))),
                    glyphTint = Color(0xFFF8FAFC),
                    onClick = { launchCamera(context) },
                    testTag = "dock_app_camera"
                )

                // 5. Launcher Settings / Glass Customization
                StockDockAppIcon(
                    glyph = AeroIconGlyph.QUICK_SETTINGS,
                    label = "Settings",
                    containerBrush = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))),
                    glyphTint = Color.White,
                    onClick = onOpenSettings,
                    testTag = "dock_app_settings"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Iconic Stock Android / Pixel Bottom Google Search Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassPill()
                .clickable { launchBrowser(context) }
                .testTag("google_search_bar_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Google "G" Badge
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF4285F4),
                                        Color(0xFFEA4335),
                                        Color(0xFFFBBC05),
                                        Color(0xFF34A853)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Search apps & web...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.textSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Voice Search Mic
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tokens.accentPrimary.copy(alpha = 0.16f))
                            .clickable { launchBrowser(context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search",
                            tint = tokens.accentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Quick Glass Blur / Launcher Settings Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tokens.accentSecondary.copy(alpha = 0.16f))
                            .clickable(onClick = onOpenSettings),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Launcher Glass Settings",
                            tint = tokens.textPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockDockAppIcon(
    glyph: AeroIconGlyph,
    label: String,
    containerBrush: Brush,
    glyphTint: Color,
    isCenterKey: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    val iconSize = if (isCenterKey) 52.dp else 48.dp
    val glyphSize = if (isCenterKey) 26.dp else 22.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .defaultMinSize(minWidth = 52.dp, minHeight = 52.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(2.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .background(containerBrush)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.15f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            CustomGlassIcon(
                glyph = glyph,
                size = glyphSize,
                tint = glyphTint,
                secondaryTint = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun GlassSideNavigationRail(
    currentPage: Int,
    isDrawerOpen: Boolean,
    onSelectPage: (Int) -> Unit,
    onToggleAppDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNewTask: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    GlassCard(
        modifier = Modifier
            .fillMaxHeight()
            .width(104.dp),
        cornerRadius = 28.dp,
        elevated = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 20.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                CustomGlassIcon(
                    glyph = AeroIconGlyph.CLOCK_3D,
                    size = 32.dp,
                    tint = tokens.accentPrimary
                )
                RailItem(
                    glyph = AeroIconGlyph.DASHBOARD,
                    label = "Home",
                    selected = currentPage == 1 && !isDrawerOpen,
                    onClick = { onSelectPage(1) }
                )
                RailItem(
                    glyph = AeroIconGlyph.DISCOVER_NEWS,
                    label = "Discover",
                    selected = currentPage == 0 && !isDrawerOpen,
                    onClick = { onSelectPage(0) }
                )
                RailItem(
                    glyph = AeroIconGlyph.APPS_DRAWER,
                    label = "Apps",
                    selected = isDrawerOpen,
                    onClick = onToggleAppDrawer
                )
                RailItem(
                    glyph = AeroIconGlyph.QUICK_SETTINGS,
                    label = "Settings",
                    selected = false,
                    onClick = onOpenSettings
                )
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(tokens.accentPrimary)
                    .clickable(onClick = onOpenNewTask),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF041326)
                )
            }
        }
    }
}

@Composable
private fun RailItem(
    glyph: AeroIconGlyph,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) tokens.accentPrimary.copy(alpha = 0.22f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        CustomGlassIcon(
            glyph = glyph,
            size = 24.dp,
            tint = if (selected) tokens.accentPrimary else tokens.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) tokens.textPrimary else tokens.textSecondary
        )
    }
}
