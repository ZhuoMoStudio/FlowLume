package com.zhuomo.flowlume.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zhuomo.flowlume.app.di.AppContainer
import com.zhuomo.flowlume.app.ui.screens.AboutScreen
import com.zhuomo.flowlume.app.ui.screens.ExperimentalScreen
import com.zhuomo.flowlume.app.ui.screens.FxScreen
import com.zhuomo.flowlume.app.ui.screens.HelpScreen
import com.zhuomo.flowlume.app.ui.screens.HomeScreen
import com.zhuomo.flowlume.app.ui.screens.RenderScreen
import com.zhuomo.flowlume.app.ui.screens.SettingsScreen
import com.zhuomo.flowlume.app.ui.screens.TimerScreen
import com.zhuomo.flowlume.config.Mode
import com.zhuomo.flowlume.ui.FlowColors

private data class Tab(val route: String, val label: String, val icon: ImageVector)

/** 底部导航（计时器 Tab 仅 App 全屏形态显示） */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val fullscreenMode = AppContainer.uiMode.collectAsState().value == Mode.FULLSCREEN

    val tabs = buildList {
        add(Tab("home", "HOME", Icons.Outlined.Home))
        add(Tab("render", "RENDER", Icons.Outlined.Tune))
        add(Tab("fx", "FX", Icons.Outlined.AutoAwesome))
        if (fullscreenMode) add(Tab("timer", "TIMER", Icons.Outlined.Timer))
        add(Tab("settings", "SETTINGS", Icons.Outlined.Settings))
    }
    val showBottomBar = currentRoute in tabs.map { it.route }

    Scaffold(
        containerColor = FlowColors.BgPrimary,
        bottomBar = {
            if (showBottomBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FlowColors.BgSecondary)
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (selected) FlowColors.Accent else FlowColors.TextTertiary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                color = if (selected) FlowColors.Accent else FlowColors.TextTertiary
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") { HomeScreen(navController) }
            composable("render") { RenderScreen() }
            composable("fx") { FxScreen() }
            composable("timer") { TimerScreen(navController) }
            composable("settings") { SettingsScreen(navController) }
            composable("experimental") { ExperimentalScreen() }
            composable("about") { AboutScreen() }
            composable("help") { HelpScreen() }
        }
    }
}

/** 次级页面顶部返回栏 */
@Composable
fun SubPageScaffold(
    title: String,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FlowColors.BgPrimary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = FlowColors.TextPrimary,
                fontSize = 22.sp,
                modifier = Modifier
                    .width(40.dp)
                    .clickable { navController.popBackStack() }
            )
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = FlowColors.TextPrimary
            )
        }
        content()
    }
}
