package com.example.slk_verniy.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.slk_verniy.ui.calendar.CalendarScreen
import com.example.slk_verniy.ui.components.AnimatedBackground
import com.example.slk_verniy.ui.profile.ProfileScreen
import com.example.slk_verniy.ui.settings.SettingsScreen
import com.example.slk_verniy.worker.AppUpdateChecker
import com.example.slk_verniy.worker.ReleaseInfo

sealed class BottomTab(val title: String, val icon: ImageVector) {
    object Profile : BottomTab("Профиль", Icons.Rounded.Person)
    object Shifts : BottomTab("Смены", Icons.Rounded.DateRange)
    object Settings : BottomTab("Настройки", Icons.Rounded.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tabs = listOf(
        BottomTab.Shifts,
        BottomTab.Profile,
        BottomTab.Settings
    )
    // Default to "Смены" (Shifts) tab at index 0 by default upon opening the app
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var releaseInfo by remember { mutableStateOf<ReleaseInfo?>(null) }

    LaunchedEffect(Unit) {
        val currentVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
        val info = AppUpdateChecker.checkForUpdate(currentVersion)
        if (info != null) {
            releaseInfo = info
        }
    }

    releaseInfo?.let { info ->
        UpdateDialog(
            releaseInfo = info,
            onDismiss = { releaseInfo = null }
        )
    }

    AnimatedBackground(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
                ) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index }
                        )
                    }
                }
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width / 4 } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width / 4 } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width / 4 } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width / 4 } + fadeOut()
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                label = "tab_transition"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> CalendarScreen(modifier = Modifier.fillMaxSize())
                    1 -> ProfileScreen(modifier = Modifier.fillMaxSize())
                    2 -> SettingsScreen(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MaterialTheme {
        MainScreen()
    }
}
