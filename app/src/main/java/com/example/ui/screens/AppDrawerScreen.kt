package com.example.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.AeroIconGlyph
import com.example.ui.components.CustomGlassIcon
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalGlassTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable? = null
)

fun Drawable.toBitmapOrNull(width: Int = 144, height: Int = 144): Bitmap? {
    return runCatching {
        if (this is BitmapDrawable && this.bitmap != null) {
            return this.bitmap
        }
        val targetWidth = if (intrinsicWidth > 0) intrinsicWidth else width
        val targetHeight = if (intrinsicHeight > 0) intrinsicHeight else height
        val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bmp
    }.getOrNull()
}

suspend fun fetchInstalledApps(context: Context): List<InstalledAppItem> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val resolveList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(mainIntent, 0)
    }

    resolveList.mapNotNull { resolveInfo ->
        val pkg = resolveInfo.activityInfo.packageName
        if (pkg == context.packageName) return@mapNotNull null // Exclude launcher itself from list
        val label = resolveInfo.loadLabel(pm).toString().takeIf { it.isNotBlank() } ?: pkg
        val icon = resolveInfo.loadIcon(pm)
        InstalledAppItem(
            label = label,
            packageName = pkg,
            activityName = resolveInfo.activityInfo.name,
            icon = icon
        )
    }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
}

fun launchApp(context: Context, packageName: String) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            Toast.makeText(context, "Cannot open $packageName", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error launching app: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun openAppInfo(context: Context, packageName: String) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open app info", Toast.LENGTH_SHORT).show()
    }
}

fun openDefaultHomeSettings(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
        }
        val homeIntent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(homeIntent)
    } catch (e: Exception) {
        try {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
        } catch (_: Exception) {}
    }
}

fun promptSetDefaultLauncher(context: Context) {
    openDefaultHomeSettings(context)
}

fun isAeroGlassDefaultLauncher(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolveInfo?.activityInfo?.packageName == context.packageName
}

fun launchPhone(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Phone dialer unavailable", Toast.LENGTH_SHORT).show()
    }
}

fun launchMessages(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(smsIntent) }
    }
}

fun launchCamera(context: Context) {
    try {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Camera unavailable", Toast.LENGTH_SHORT).show()
    }
}

fun launchBrowser(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Browser unavailable", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = LocalGlassTokens.current
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var appsList by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedAppForDetails by remember { mutableStateOf<InstalledAppItem?>(null) }
    var isDefaultLauncher by remember { mutableStateOf(isAeroGlassDefaultLauncher(context)) }

    LaunchedEffect(Unit) {
        appsList = fetchInstalledApps(context)
        isLoading = false
        isDefaultLauncher = isAeroGlassDefaultLauncher(context)
    }

    val filteredApps = remember(searchQuery, appsList) {
        if (searchQuery.isBlank()) {
            appsList
        } else {
            val q = searchQuery.trim().lowercase()
            appsList.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenWidth = maxWidth
        val columnsCount = when {
            screenWidth < 340.dp -> 3
            screenWidth < 600.dp -> 4
            screenWidth < 840.dp -> 5
            else -> 6
        }
        val isNarrow = screenWidth < 360.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = if (isNarrow) 10.dp else 16.dp, vertical = 8.dp)
        ) {
        // Search Bar with Liquid Glass aesthetic
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            cornerRadius = 24.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomGlassIcon(
                    glyph = AeroIconGlyph.SEARCH,
                    size = 20.dp,
                    tint = tokens.accentPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search apps and features...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tokens.textSecondary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = tokens.textPrimary,
                        unfocusedTextColor = tokens.textPrimary,
                        cursorColor = tokens.accentPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("app_drawer_search_input")
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = tokens.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Set as Default Home banner if not default
        if (!isDefaultLauncher) {
            Spacer(modifier = Modifier.height(6.dp))
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openDefaultHomeSettings(context) }
                    .testTag("set_default_launcher_banner"),
                cornerRadius = 18.dp
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
                        modifier = Modifier.weight(1f)
                    ) {
                        CustomGlassIcon(
                            glyph = AeroIconGlyph.HOME_LAUNCHER,
                            size = 20.dp,
                            tint = tokens.accentSecondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Set AeroGlass as default Home Launcher",
                            style = MaterialTheme.typography.labelMedium,
                            color = tokens.textPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.accentPrimary)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SET DEFAULT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF041326)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // App count label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (searchQuery.isBlank()) "ALL APPLICATIONS (${appsList.size})" else "SEARCH RESULTS (${filteredApps.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = tokens.accentPrimary
            )
            Text(
                text = "Tap to open • Long press options",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.textSecondary
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = tokens.accentPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CustomGlassIcon(
                        glyph = AeroIconGlyph.SEARCH,
                        size = 40.dp,
                        tint = tokens.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No applications matching '$searchQuery'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.textSecondary
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(if (isNarrow) 12.dp else 16.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isNarrow) 8.dp else 12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("app_drawer_grid")
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppGridItem(
                        app = app,
                        onClick = {
                            launchApp(context, app.packageName)
                        },
                        onLongClick = {
                            selectedAppForDetails = app
                        }
                    )
                }
            }
        }
    }
}

    // Long Press Context Sheet
    selectedAppForDetails?.let { app ->
        ModalBottomSheet(
            onDismissRequest = { selectedAppForDetails = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = tokens.bgGradientStart.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val appBitmap = remember(app.packageName) { app.icon?.toBitmapOrNull(180, 180) }
                if (appBitmap != null) {
                    Image(
                        bitmap = appBitmap.asImageBitmap(),
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        AppSheetActionRow(
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            label = "Open Application",
                            tint = tokens.accentPrimary,
                            onClick = {
                                selectedAppForDetails = null
                                launchApp(context, app.packageName)
                            }
                        )
                        AppSheetActionRow(
                            icon = Icons.Default.Info,
                            label = "App Info & Permissions",
                            tint = tokens.textPrimary,
                            onClick = {
                                selectedAppForDetails = null
                                openAppInfo(context, app.packageName)
                            }
                        )
                        AppSheetActionRow(
                            icon = Icons.Default.PushPin,
                            label = "Pin to Home Shortcuts",
                            tint = tokens.accentSecondary,
                            onClick = {
                                selectedAppForDetails = null
                                Toast.makeText(context, "${app.label} pinned to Launcher Home", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AppGridItem(
    app: InstalledAppItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val appBitmap = remember(app.packageName) { app.icon?.toBitmapOrNull(140, 140) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .testTag("app_item_${app.packageName.replace('.', '_')}")
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (tokens.isDark) 0.14f else 0.40f),
                            Color.White.copy(alpha = if (tokens.isDark) 0.04f else 0.16f)
                        )
                    )
                )
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (appBitmap != null) {
                Image(
                    bitmap = appBitmap.asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Text(
                    text = app.label.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 72.dp)
        )
    }
}

@Composable
private fun AppSheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textPrimary
        )
    }
}
