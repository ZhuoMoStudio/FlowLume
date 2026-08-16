package com.zhuomo.flowlume.app.ui.screens

import android.Manifest
import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.zhuomo.flowlume.app.di.AppContainer
import com.zhuomo.flowlume.config.Mode
import com.zhuomo.flowlume.media.ArtBus
import com.zhuomo.flowlume.media.ArtEvent
import com.zhuomo.flowlume.media.ListenerStatus
import com.zhuomo.flowlume.media.NotificationCenter
import com.zhuomo.flowlume.media.NotificationManagerCompatCompat
import com.zhuomo.flowlume.media.RestartListenerHelper
import com.zhuomo.flowlume.ui.CheckRow
import com.zhuomo.flowlume.ui.FlowColors
import com.zhuomo.flowlume.ui.FxCard
import com.zhuomo.flowlume.ui.PrimaryButton
import com.zhuomo.flowlume.ui.SegmentedControl
import com.zhuomo.flowlume.ui.SecondaryButton
import com.zhuomo.flowlume.ui.StatusDot
import com.zhuomo.flowlume.wallpaper.FluidWallpaperService
import kotlinx.coroutines.launch

/** 页面1 · 启动权限引导首页 */
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mode by AppContainer.uiMode.collectAsState()

    var art by remember { mutableStateOf<ArtEvent?>(null) }
    var listenerEnabled by remember {
        mutableStateOf(NotificationManagerCompatCompat.isListenerEnabled(context))
    }
    var audioGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(Unit) {
        launch {
            ArtBus.events.collect { art = it }
        }
        launch {
            NotificationCenter.status.collect { s ->
                when (s) {
                    ListenerStatus.CONNECTED -> listenerEnabled = true
                    ListenerStatus.REVOKED -> listenerEnabled = false
                    else -> Unit
                }
            }
        }
    }

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            AppContainer.audioEngine.start(result.resultCode, result.data!!)
        }
    }

    val launchProjection: () -> Unit = {
        val pm = context.getSystemService(MediaProjectionManager::class.java)
        projectionLauncher.launch(pm.createScreenCaptureIntent())
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        audioGranted = granted
        if (granted) launchProjection()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlowColors.BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 品牌区
        Column {
            Text(
                text = "FLOWLUME",
                style = MaterialTheme.typography.titleLarge,
                color = FlowColors.TextPrimary,
                fontSize = 28.sp
            )
            Text(
                text = "Fluid Music Wallpaper · 弥光",
                style = MaterialTheme.typography.bodySmall,
                color = FlowColors.TextTertiary
            )
        }

        // 权限状态卡片
        FxCard(title = "PERMISSIONS") {
            CheckRow(
                title = "允许阅读通知",
                subtitle = if (listenerEnabled) "已授权 · 自动提取专辑封面" else "未授权 · 壁纸无法获取封面（核心权限）",
                checked = listenerEnabled,
                onCheckedChange = {
                    context.startActivity(
                        Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    )
                }
            )
            Spacer(Modifier.height(4.dp))
            CheckRow(
                title = "允许获取音频权限（可选）",
                subtitle = if (audioGranted) "已授权 · 音频联动可用" else "未授权 · 音频联动动效不可用",
                checked = audioGranted,
                onCheckedChange = {
                    if (!audioGranted) {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        launchProjection()
                    }
                }
            )
        }

        // 现在播放状态面板
        FxCard(title = "NOW PLAYING") {
            val playing = art?.title != null
            if (playing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(if (art?.isPlaying == true) FlowColors.Success else FlowColors.Warning)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("《${art?.title}》", color = FlowColors.TextPrimary)
                        Text(art?.artist ?: "", style = MaterialTheme.typography.bodySmall, color = FlowColors.TextSecondary)
                    }
                }
            } else {
                Text(
                    text = "NO MUSIC DETECTED\n未检测到播放中的音乐，播放后自动提取专辑封面",
                    color = FlowColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!listenerEnabled) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton(text = "RESTART LISTENER 一键重启监听服务") {
                    RestartListenerHelper.restart(context)
                    scope.launch {
                        kotlinx.coroutines.delay(500)
                        listenerEnabled = NotificationManagerCompatCompat.isListenerEnabled(context)
                    }
                }
            }
        }

        // 设置壁纸
        PrimaryButton(text = "SET AS WALLPAPER 设置壁纸") {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, FluidWallpaperService::class.java)
                )
            }
            runCatching { context.startActivity(intent) }
        }

        // 形态切换
        FxCard(title = "RENDER MODE 运行形态") {
            SegmentedControl(
                options = listOf("DESKTOP WALLPAPER", "APP FULLSCREEN"),
                selectedIndex = if (mode == Mode.WALLPAPER) 0 else 1
            ) { idx ->
                AppContainer.uiMode.value = if (idx == 0) Mode.WALLPAPER else Mode.FULLSCREEN
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "两种形态参数独立保存，可在渲染页一键互拷",
                style = MaterialTheme.typography.bodySmall,
                color = FlowColors.TextTertiary
            )
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = "OPEN FULLSCREEN 打开全屏模式") {
                context.startActivity(
                    Intent(context, com.zhuomo.flowlume.app.fullscreen.FullscreenActivity::class.java)
                )
            }
        }
    }
}
