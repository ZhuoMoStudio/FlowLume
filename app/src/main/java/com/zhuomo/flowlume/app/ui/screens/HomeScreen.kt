package com.zhuomo.flowlume.app.ui.screens

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.zhuomo.flowlume.app.R
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
    val snackbar = remember { SnackbarHostState() }
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
    val audioDeniedMsg = stringResource(R.string.audio_perm_off)
    val restartingMsg = stringResource(R.string.listener_restarting)
    val openWallpaperFailedMsg = stringResource(R.string.open_wallpaper_failed)
    val openFullscreenFailedMsg = stringResource(R.string.open_fullscreen_failed)

    // 音频权限 → Visualizer（无屏幕捕获弹窗）
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        audioGranted = granted
        if (granted) {
            AppContainer.audioEngine.start()
        } else {
            scope.launch { snackbar.showSnackbar(audioDeniedMsg) }
        }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FlowColors.BgPrimary)
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                text = stringResource(R.string.brand_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = FlowColors.TextTertiary
            )
        }

        // 权限状态卡片
        FxCard(title = stringResource(R.string.permissions)) {
            CheckRow(
                title = stringResource(R.string.notif_perm),
                subtitle = if (listenerEnabled) {
                    stringResource(R.string.notif_perm_on)
                } else {
                    stringResource(R.string.notif_perm_off)
                },
                checked = listenerEnabled,
                onCheckedChange = {
                    context.startActivity(
                        Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    )
                }
            )
            Spacer(Modifier.height(4.dp))
            CheckRow(
                title = stringResource(R.string.audio_perm),
                subtitle = if (audioGranted) {
                    stringResource(R.string.audio_perm_on)
                } else {
                    stringResource(R.string.audio_perm_off)
                },
                checked = audioGranted,
                onCheckedChange = {
                    if (!audioGranted) {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        AppContainer.audioEngine.start()
                    }
                }
            )
        }

        // 现在播放状态面板（含实时专辑封面预览）
        FxCard(title = stringResource(R.string.now_playing)) {
            val playing = art?.title != null
            if (playing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    art?.artwork?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    StatusDot(if (art?.isPlaying == true) FlowColors.Success else FlowColors.Warning)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("《${art?.title}》", color = FlowColors.TextPrimary)
                        Text(
                            art?.artist ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = FlowColors.TextSecondary
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.no_music),
                    color = FlowColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!listenerEnabled) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton(text = stringResource(R.string.restart_listener)) {
                    val needsManual = RestartListenerHelper.restart(context)
                    scope.launch {
                        if (needsManual) {
                            snackbar.showSnackbar(restartingMsg)
                        } else {
                            kotlinx.coroutines.delay(800)
                            listenerEnabled =
                                NotificationManagerCompatCompat.isListenerEnabled(context)
                        }
                    }
                }
            }
        }

        // 设置壁纸（失败时给出明确提示）
        PrimaryButton(text = stringResource(R.string.set_wallpaper)) {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, FluidWallpaperService::class.java)
                )
            }
            val ok = runCatching { context.startActivity(intent) }.isSuccess
            if (!ok) {
                scope.launch { snackbar.showSnackbar(openWallpaperFailedMsg) }
            }
        }

        // 形态切换
        FxCard(title = stringResource(R.string.render_mode)) {
            SegmentedControl(
                options = listOf(
                    stringResource(R.string.mode_wallpaper),
                    stringResource(R.string.mode_fullscreen)
                ),
                selectedIndex = if (mode == Mode.WALLPAPER) 0 else 1
            ) { idx ->
                AppContainer.uiMode.value = if (idx == 0) Mode.WALLPAPER else Mode.FULLSCREEN
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.config_independent),
                style = MaterialTheme.typography.bodySmall,
                color = FlowColors.TextTertiary
            )
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = stringResource(R.string.open_fullscreen)) {
                val ok = runCatching {
                    context.startActivity(
                        Intent(context, com.zhuomo.flowlume.app.fullscreen.FullscreenActivity::class.java)
                    )
                }.isSuccess
                if (!ok) {
                    scope.launch { snackbar.showSnackbar(openFullscreenFailedMsg) }
                }
            }
        }
    }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}
