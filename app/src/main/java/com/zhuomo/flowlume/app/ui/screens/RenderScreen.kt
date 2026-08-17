package com.zhuomo.flowlume.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.zhuomo.flowlume.app.R
import com.zhuomo.flowlume.app.di.AppContainer
import com.zhuomo.flowlume.app.ui.useRenderConfig
import com.zhuomo.flowlume.config.ConfigStore
import com.zhuomo.flowlume.config.Mode
import com.zhuomo.flowlume.config.RenderMode
import com.zhuomo.flowlume.media.ArtBus
import com.zhuomo.flowlume.media.ArtEvent
import com.zhuomo.flowlume.media.ListenerStatus
import com.zhuomo.flowlume.media.NotificationCenter
import com.zhuomo.flowlume.ui.CheckRow
import com.zhuomo.flowlume.ui.ConfirmDialog
import com.zhuomo.flowlume.ui.FlowColors
import com.zhuomo.flowlume.ui.FxCard
import com.zhuomo.flowlume.ui.PrimaryButton
import com.zhuomo.flowlume.ui.RadioRow
import com.zhuomo.flowlume.ui.SectionLabel
import com.zhuomo.flowlume.ui.SliderRow
import com.zhuomo.flowlume.ui.StatusDot
import kotlinx.coroutines.launch

private val TONES = listOf(
    0xFF14142C, 0xFF1A1A2E, 0xFF2D1B4E, 0xFF0F2A43,
    0xFF3E1F47, 0xFF1B3A2F, 0xFF4A2C2C, 0xFF232323
)

/** 页面2 · 通用渲染参数面板 */
@Composable
fun RenderScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mode by AppContainer.uiMode.collectAsState()
    val (config, update) = useRenderConfig(mode)
    var showCopyDialog by remember { mutableStateOf(false) }

    // 实时获取状态（封面/歌曲信息/监听状态）
    var art by remember { mutableStateOf<ArtEvent?>(null) }
    var listenerEnabled by remember { mutableStateOf(true) }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlowColors.BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(
                if (mode == Mode.WALLPAPER) R.string.editing_wallpaper else R.string.editing_fullscreen
            ),
            color = FlowColors.Accent,
            style = MaterialTheme.typography.labelMedium
        )

        // 实时获取卡片：当前捕获的专辑封面与信息
        FxCard(title = stringResource(R.string.live_capture)) {
            if (art?.title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    art?.artwork?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column {
                        Text("《${art?.title}》", color = FlowColors.TextPrimary)
                        if (!art?.artist.isNullOrBlank()) {
                            Text(art?.artist.orEmpty(), style = MaterialTheme.typography.bodySmall, color = FlowColors.TextSecondary)
                        }
                        if (!art?.album.isNullOrBlank()) {
                            Text(
                                stringResource(R.string.album_label, art?.album.orEmpty()),
                                style = MaterialTheme.typography.bodySmall,
                                color = FlowColors.TextTertiary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(if (art?.isPlaying == true) FlowColors.Success else FlowColors.Warning)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (art?.isPlaying == true) {
                                    stringResource(R.string.status_playing)
                                } else {
                                    stringResource(R.string.status_paused)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = FlowColors.TextTertiary
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.live_capture_empty),
                    color = FlowColors.TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(if (listenerEnabled) FlowColors.Success else FlowColors.Warning)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (listenerEnabled) R.string.listener_on else R.string.listener_off
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = FlowColors.TextTertiary
                )
            }
        }

        // 流体设定
        FxCard(title = stringResource(R.string.fluid_card)) {
            SliderRow(stringResource(R.string.fluid_scale), config.fluidScale, valueRange = 0.5f..3f, steps = 24) {
                update(config.copy(fluidScale = it))
            }
            SliderRow(stringResource(R.string.flow_speed), config.flowSpeed, valueRange = 0f..2f, steps = 19) {
                update(config.copy(flowSpeed = it))
            }
            SliderRow(stringResource(R.string.turbulence), config.turbulence, valueRange = 0f..2f, steps = 19) {
                update(config.copy(turbulence = it))
            }
        }

        // 图形设定
        FxCard(title = stringResource(R.string.graphics_card)) {
            RadioRow(stringResource(R.string.mode_blur), config.renderMode == RenderMode.BLUR) {
                update(config.copy(renderMode = RenderMode.BLUR))
            }
            RadioRow(stringResource(R.string.mode_glass), config.renderMode == RenderMode.FLUTED_GLASS) {
                update(config.copy(renderMode = RenderMode.FLUTED_GLASS))
            }
        }

        // 专辑封面配置
        FxCard(title = stringResource(R.string.album_card)) {
            CheckRow(stringResource(R.string.restore_art), checked = config.restoreArtOnReboot) {
                update(config.copy(restoreArtOnReboot = it))
            }
            CheckRow(stringResource(R.string.keep_art_pause), checked = config.keepArtOnPause) {
                update(config.copy(keepArtOnPause = it))
            }
            Spacer(Modifier.height(8.dp))
            SectionLabel(stringResource(R.string.default_tone))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TONES.forEach { tone ->
                    val selected = config.defaultTone == tone
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(tone))
                            .clickable { update(config.copy(defaultTone = tone)) }
                            .padding(2.dp)
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(FlowColors.Accent.copy(alpha = 0.35f))
                            )
                        }
                    }
                }
            }
        }

        // 全局画面调节
        FxCard(title = stringResource(R.string.global_adjust)) {
            SliderRow(stringResource(R.string.brightness), config.brightness, valueRange = 0.5f..1.5f, steps = 9) {
                update(config.copy(brightness = it))
            }
            SliderRow(stringResource(R.string.saturation), config.saturation, valueRange = 0f..2f, steps = 19) {
                update(config.copy(saturation = it))
            }
        }

        // 配置互拷
        FxCard(title = stringResource(R.string.sync_card)) {
            Text(
                text = stringResource(R.string.copy_to_target),
                style = MaterialTheme.typography.bodyMedium,
                color = FlowColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            PrimaryButton(text = stringResource(R.string.copy_config)) {
                showCopyDialog = true
            }
        }
    }

    if (showCopyDialog) {
        val target = if (mode == Mode.WALLPAPER) Mode.FULLSCREEN else Mode.WALLPAPER
        ConfirmDialog(
            title = stringResource(R.string.overwrite_title),
            message = stringResource(R.string.overwrite_msg),
            confirmText = stringResource(R.string.overwrite),
            onConfirm = {
                showCopyDialog = false
                scope.launch { ConfigStore.copy(context, mode, target) }
            },
            onDismiss = { showCopyDialog = false }
        )
    }
}
