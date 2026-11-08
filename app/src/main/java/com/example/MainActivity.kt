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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
import com.example.ui.screens.AddTaskModalSheet
import com.example.ui.screens.AppDrawerScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardHomePage
import com.example.ui.screens.DiscoverFeedPage
import com.example.ui.screens.SystemOptimizerAndSettingsPage
import com.example.ui.screens.launchPhone
import com.example.ui.screens.launchMessages
import com.example.ui.screens.launchCamera
import com.example.ui.screens.signOut
import com.example.ui.theme.AeroGlassTheme
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.viewmodel.GlassDashboardViewModel
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

    var showAddTaskSheet by remember { mutableStateOf(false) }

    // Page 0 = Discover & Weather, Page 1 = Home 3D Clock & Tasks, Page 2 = Apps Drawer, Page 3 = Control & RAM
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 4 })

    BackHandler(enabled = pagerState.currentPage != 1) {
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
                alpha = if (tokens.isDark) 0.62f else 0.32f,
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
                        onSelectPage = { page ->
                            scope.launch { pagerState.animateScrollToPage(page) }
                        },
                        onOpenNewTask = { showAddTaskSheet = true }
                    )

                    Box(modifier = Modifier.weight(1.15f)) {
                        DashboardHomePage(
                            tasksUiState = tasksState,
                            selectedCategory = selectedCategory,
                            weather = weather,
                            telemetry = telemetry,
                            parallaxEnabled = preferences.parallaxEnabled,
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
                            onOpenDiscoverFeed = { scope.launch { pagerState.animateScrollToPage(0) } },
                            onOpenOptimizerPage = { scope.launch { pagerState.animateScrollToPage(3) } },
                            onOpenAddTaskSheet = { showAddTaskSheet = true },
                            onOpenAppDrawer = { scope.launch { pagerState.animateScrollToPage(2) } }
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
                            2 -> AppDrawerScreen(
                                onNavigateBack = { scope.launch { pagerState.animateScrollToPage(1) } }
                            )
                            else -> SystemOptimizerAndSettingsPage(
                                telemetry = telemetry,
                                preferences = preferences,
                                quickSettings = quickSettings,
                                onRunRamCleanBoost = { viewModel.performRamCleanBoost(context) },
                                onCyclePowerProfile = { viewModel.cyclePowerProfile(context) },
                                onToggleQuickSetting = viewModel::toggleQuickSetting,
                                onBlurIntensityChanged = viewModel::updateBlurIntensity,
                                onSelectThemeMode = viewModel::updateThemeMode,
                                onSelectIconPack = viewModel::updateIconPack,
                                onToggleParallax = viewModel::toggleParallax,
                                isGuestSession = isGuestSession,
                                onSignOut = {
                                    if (isGuestSession) {
                                        onExitGuestMode()
                                    } else {
                                        signOut(credentialManager, scope)
                                    }
                                }
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
                    TopGlassStatusHeader(
                        currentPage = pagerState.currentPage,
                        blurPercent = preferences.blurIntensity.toInt(),
                        focusDndActive = quickSettings.focusDndEnabled,
                        isGuestSession = isGuestSession,
                        onSelectPage = { page ->
                            scope.launch { pagerState.animateScrollToPage(page) }
                        }
                    )

                    AnimatedVisibility(
                        visible = statusBanner != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            cornerRadius = 16.dp
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
                                tasksUiState = tasksState,
                                selectedCategory = selectedCategory,
                                weather = weather,
                                telemetry = telemetry,
                                parallaxEnabled = preferences.parallaxEnabled,
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
                                onOpenDiscoverFeed = { scope.launch { pagerState.animateScrollToPage(0) } },
                                onOpenOptimizerPage = { scope.launch { pagerState.animateScrollToPage(3) } },
                                onOpenAddTaskSheet = { showAddTaskSheet = true },
                                onOpenAppDrawer = { scope.launch { pagerState.animateScrollToPage(2) } }
                            )
                            2 -> AppDrawerScreen(
                                onNavigateBack = { scope.launch { pagerState.animateScrollToPage(1) } }
                            )
                            else -> SystemOptimizerAndSettingsPage(
                                telemetry = telemetry,
                                preferences = preferences,
                                quickSettings = quickSettings,
                                onRunRamCleanBoost = { viewModel.performRamCleanBoost(context) },
                                onCyclePowerProfile = { viewModel.cyclePowerProfile(context) },
                                onToggleQuickSetting = viewModel::toggleQuickSetting,
                                onBlurIntensityChanged = viewModel::updateBlurIntensity,
                                onSelectThemeMode = viewModel::updateThemeMode,
                                onSelectIconPack = viewModel::updateIconPack,
                                onToggleParallax = viewModel::toggleParallax,
                                isGuestSession = isGuestSession,
                                onSignOut = {
                                    if (isGuestSession) {
                                        onExitGuestMode()
                                    } else {
                                        signOut(credentialManager, scope)
                                    }
                                }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    FloatingLiquidGlassDock(
                        currentPage = pagerState.currentPage,
                        onSelectPage = { page ->
                            scope.launch { pagerState.animateScrollToPage(page) }
                        }
                    )
                }
            }

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
                    }
                )
            }
        }
    }
}

@Composable
private fun TopGlassStatusHeader(
    currentPage: Int,
    blurPercent: Int,
    focusDndActive: Boolean,
    isGuestSession: Boolean,
    onSelectPage: (Int) -> Unit
) {
    val tokens = LocalGlassTokens.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        val isNarrow = maxWidth < 380.dp

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Clean stock launcher indicator
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(tokens.accentPrimary.copy(alpha = 0.20f))
                    .clickable { onSelectPage(1) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = when (currentPage) {
                        0 -> "Google Discover"
                        2 -> "All Applications"
                        3 -> "Control Center"
                        else -> "AeroGlass Home"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.accentPrimary
                )
            }

            // Right: Quick Status Chips (Guest, DND, Glass Level)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isGuestSession) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(tokens.accentSecondary.copy(alpha = 0.22f))
                            .clickable { onSelectPage(3) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "GUEST",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textPrimary
                        )
                    }
                }
                if (focusDndActive && !isNarrow) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(tokens.accentSecondary.copy(alpha = 0.22f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DND",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.accentSecondary
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tokens.cardFillTop)
                        .clickable { onSelectPage(3) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${blurPercent}% Glass",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingLiquidGlassDock(
    currentPage: Int,
    onSelectPage: (Int) -> Unit
) {
    val tokens = LocalGlassTokens.current
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .testTag("floating_glass_dock")
    ) {
        // Stock Android Page Indicator Dots: Discover (0), Home (1), Apps (2), Control (3)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            repeat(4) { idx ->
                val active = currentPage == idx
                Box(
                    modifier = Modifier
                        .size(if (active) 8.dp else 5.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) tokens.accentPrimary
                            else tokens.textSecondary.copy(alpha = 0.40f)
                        )
                        .clickable { onSelectPage(idx) }
                )
            }
        }

        // Authentic Stock Android 5-Icon Glass Shelf
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 32.dp,
            elevated = true
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Phone App (Dialer)
                StockDockAppIcon(
                    glyph = AeroIconGlyph.PHONE,
                    label = "Phone",
                    containerBrush = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
                    glyphTint = Color.White,
                    onClick = { launchPhone(context) },
                    testTag = "dock_app_phone"
                )

                // 2. Messages App (SMS)
                StockDockAppIcon(
                    glyph = AeroIconGlyph.MESSAGES,
                    label = "Messages",
                    containerBrush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))),
                    glyphTint = Color.White,
                    onClick = { launchMessages(context) },
                    testTag = "dock_app_messages"
                )

                // 3. Central Launcher Button (App Drawer / Home)
                val inDrawer = currentPage == 2
                StockDockAppIcon(
                    glyph = if (inDrawer) AeroIconGlyph.DASHBOARD else AeroIconGlyph.APPS_DRAWER,
                    label = if (inDrawer) "Home" else "Apps",
                    containerBrush = Brush.linearGradient(
                        listOf(
                            tokens.accentPrimary.copy(alpha = 0.35f),
                            tokens.accentSecondary.copy(alpha = 0.25f)
                        )
                    ),
                    glyphTint = tokens.accentPrimary,
                    isCenterKey = true,
                    onClick = {
                        if (inDrawer) onSelectPage(1) else onSelectPage(2)
                    },
                    testTag = "dock_app_drawer"
                )

                // 4. Camera App
                StockDockAppIcon(
                    glyph = AeroIconGlyph.CAMERA,
                    label = "Camera",
                    containerBrush = Brush.linearGradient(listOf(Color(0xFF475569), Color(0xFF1E293B))),
                    glyphTint = Color(0xFFF8FAFC),
                    onClick = { launchCamera(context) },
                    testTag = "dock_app_camera"
                )

                // 5. Settings / Control Center
                StockDockAppIcon(
                    glyph = AeroIconGlyph.QUICK_SETTINGS,
                    label = "Control",
                    containerBrush = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))),
                    glyphTint = Color.White,
                    selected = currentPage == 3,
                    onClick = { onSelectPage(3) },
                    testTag = "dock_app_settings"
                )
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
    selected: Boolean = false,
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
    onSelectPage: (Int) -> Unit,
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
                    selected = currentPage == 1,
                    onClick = { onSelectPage(1) }
                )
                RailItem(
                    glyph = AeroIconGlyph.DISCOVER_NEWS,
                    label = "Discover",
                    selected = currentPage == 0,
                    onClick = { onSelectPage(0) }
                )
                RailItem(
                    glyph = AeroIconGlyph.APPS_DRAWER,
                    label = "Apps",
                    selected = currentPage == 2,
                    onClick = { onSelectPage(2) }
                )
                RailItem(
                    glyph = AeroIconGlyph.QUICK_SETTINGS,
                    label = "Control",
                    selected = currentPage == 3,
                    onClick = { onSelectPage(3) }
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
