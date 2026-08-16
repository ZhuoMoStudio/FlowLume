package com.zhuomo.flowlume.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zhuomo.flowlume.app.R
import com.zhuomo.flowlume.app.di.AppContainer
import com.zhuomo.flowlume.app.ui.useRenderConfig
import com.zhuomo.flowlume.config.EffectState
import com.zhuomo.flowlume.config.Mode
import com.zhuomo.flowlume.config.Preset
import com.zhuomo.flowlume.config.PresetRender
import com.zhuomo.flowlume.config.PresetStore
import com.zhuomo.flowlume.effects.PerformancePolicy
import com.zhuomo.flowlume.render.Effect
import com.zhuomo.flowlume.render.EffectGroup
import com.zhuomo.flowlume.render.EffectRegistry
import com.zhuomo.flowlume.ui.CheckRow
import com.zhuomo.flowlume.ui.ConfirmDialog
import com.zhuomo.flowlume.ui.EmptyState
import com.zhuomo.flowlume.ui.FlowColors
import com.zhuomo.flowlume.ui.FxCard
import com.zhuomo.flowlume.ui.SecondaryButton
import com.zhuomo.flowlume.ui.SliderRow
import kotlinx.coroutines.launch

/** 页面3 · 动效控制面板（核心模块） */
@Composable
fun FxScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mode by AppContainer.uiMode.collectAsState()
    val (config, update) = useRenderConfig(mode)
    val performance = config.performanceMode
    val forcedOff = if (performance) PerformancePolicy.disabledEffectIds() else emptySet()

    var official by remember { mutableStateOf<List<Preset>>(emptyList()) }
    var custom by remember { mutableStateOf<List<Preset>>(emptyList()) }
    var showSaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        official = PresetStore.officialPresets(context)
        custom = PresetStore.customPresets(context)
    }

    fun updateEffect(id: String, transform: (EffectState) -> EffectState) {
        update(config.copy(effects = config.effects.map { if (it.id == id) transform(it) else it }))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlowColors.BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (performance) {
            Text(
                text = stringResource(R.string.perf_on_warning),
                color = FlowColors.Warning,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // 预设快捷条
        FxCard(title = stringResource(R.string.presets)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(official.size + custom.size) { i ->
                    val preset = if (i < official.size) official[i] else custom[i - official.size]
                    Text(
                        text = preset.name.uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(FlowColors.Accent.copy(alpha = 0.25f))
                            .clickable {
                                update(
                                    config.copy(
                                        effects = preset.effects,
                                        renderMode = preset.render.renderMode,
                                        fluidScale = preset.render.fluidScale,
                                        flowSpeed = preset.render.flowSpeed,
                                        turbulence = preset.render.turbulence,
                                        brightness = preset.render.brightness,
                                        saturation = preset.render.saturation
                                    )
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(text = stringResource(R.string.save_preset)) { showSaveDialog = true }
                SecondaryButton(text = stringResource(R.string.import_preset)) { /* SAF 导入：预留 */ }
            }
        }

        EffectGroupCard(
            group = EffectGroup.BASE_FLUID,
            effects = EffectRegistry.byGroup(EffectGroup.BASE_FLUID),
            config = config,
            forcedOff = forcedOff,
            onUpdateEffect = ::updateEffect
        )
        EffectGroupCard(
            group = EffectGroup.AUDIO_SYNC,
            effects = EffectRegistry.byGroup(EffectGroup.AUDIO_SYNC),
            config = config,
            forcedOff = forcedOff,
            onUpdateEffect = ::updateEffect
        )
        EffectGroupCard(
            group = EffectGroup.PARTICLE_DECOR,
            effects = EffectRegistry.byGroup(EffectGroup.PARTICLE_DECOR),
            config = config,
            forcedOff = forcedOff,
            onUpdateEffect = ::updateEffect
        )
    }

    if (showSaveDialog) {
        ConfirmDialog(
            title = stringResource(R.string.save_preset_title),
            message = stringResource(R.string.save_preset_msg),
            confirmText = stringResource(R.string.ok),
            onConfirm = {
                showSaveDialog = false
                val name = "CUSTOM ${System.currentTimeMillis() % 10000}"
                val preset = Preset(
                    name = name,
                    official = false,
                    effects = config.effects,
                    render = PresetRender(
                        renderMode = config.renderMode,
                        fluidScale = config.fluidScale,
                        flowSpeed = config.flowSpeed,
                        turbulence = config.turbulence,
                        brightness = config.brightness,
                        saturation = config.saturation
                    )
                )
                scope.launch {
                    PresetStore.saveCustom(context, preset)
                    custom = PresetStore.customPresets(context)
                }
            },
            onDismiss = { showSaveDialog = false }
        )
    }
}

@Composable
private fun EffectGroupCard(
    group: EffectGroup,
    effects: List<Effect>,
    config: com.zhuomo.flowlume.config.RenderConfig,
    forcedOff: Set<String>,
    onUpdateEffect: (String, (EffectState) -> EffectState) -> Unit
) {
    val title = when (group) {
        EffectGroup.BASE_FLUID -> stringResource(R.string.group_base)
        EffectGroup.AUDIO_SYNC -> stringResource(R.string.group_audio)
        EffectGroup.PARTICLE_DECOR -> stringResource(R.string.group_particle)
    }
    FxCard(title = title) {
        if (effects.isEmpty()) {
            EmptyState(stringResource(R.string.no_effects))
            return@FxCard
        }
        effects.forEach { fx ->
            val state = config.effects.firstOrNull { it.id == fx.id }
            if (state == null) return@forEach
            val disabled = state.id in forcedOff
            CheckRow(
                title = fx.meta.title,
                checked = state.enabled && !disabled,
                enabled = !disabled,
                onCheckedChange = { en ->
                    onUpdateEffect(fx.id) { it.copy(enabled = en) }
                }
            )
            if (state.enabled && !disabled) {
                if (fx.meta.hasIntensity) {
                    SliderRow(stringResource(R.string.intensity), state.intensity) { v ->
                        onUpdateEffect(fx.id) { it.copy(intensity = v) }
                    }
                }
                if (fx.meta.hasSpeed) {
                    SliderRow(stringResource(R.string.speed), state.speed) { v ->
                        onUpdateEffect(fx.id) { it.copy(speed = v) }
                    }
                }
                if (fx.meta.hasSize) {
                    SliderRow(stringResource(R.string.size), state.size) { v ->
                        onUpdateEffect(fx.id) { it.copy(size = v) }
                    }
                }
            }
        }
    }
}
