package com.zhuomo.flowlume.config

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "flowlume_config")

/**
 * 双形态配置存储：桌面壁纸 / App 全屏 各一套参数，隔离保存，一键互拷。
 * 另保存计时器配置（仅全屏形态使用）。
 */
object ConfigStore {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val KEY_WALLPAPER = preferencesKey<String>("wallpaper")
    private val KEY_FULLSCREEN = preferencesKey<String>("fullscreen")
    private val KEY_TIMER = preferencesKey<String>("timer")
    private val KEY_SEPARATE = preferencesKey<Boolean>("separate_configs")

    // 渲染线程读取的内存快照（由 App 启动时收集 DataStore 更新）
    @Volatile var wallpaperSnapshot: RenderConfig = RenderConfig()
    @Volatile var fullscreenSnapshot: RenderConfig = RenderConfig()

    fun current(mode: Mode): RenderConfig =
        if (mode == Mode.WALLPAPER) wallpaperSnapshot else fullscreenSnapshot

    fun configFlow(context: Context, mode: Mode): Flow<RenderConfig> =
        context.dataStore.data.map { prefs ->
            val key = if (mode == Mode.WALLPAPER) KEY_WALLPAPER else KEY_FULLSCREEN
            prefs[key]?.let { json.decodeFromString<RenderConfig>(it) } ?: RenderConfig()
        }

    suspend fun save(context: Context, mode: Mode, config: RenderConfig) {
        val key = if (mode == Mode.WALLPAPER) KEY_WALLPAPER else KEY_FULLSCREEN
        context.dataStore.edit { prefs -> prefs[key] = json.encodeToString(RenderConfig.serializer(), config) }
        if (mode == Mode.WALLPAPER) wallpaperSnapshot = config else fullscreenSnapshot = config
    }

    /** 一键复制：把 source 形态的配置覆盖写入 target 形态 */
    suspend fun copy(context: Context, source: Mode, target: Mode) {
        val cfg = current(source)
        save(context, target, cfg)
        ReloadBus.emit(target)
    }

    fun timerFlow(context: Context): Flow<TimerConfig> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_TIMER]?.let { json.decodeFromString<TimerConfig>(it) } ?: TimerConfig()
        }

    suspend fun saveTimer(context: Context, config: TimerConfig) {
        context.dataStore.edit { prefs -> prefs[KEY_TIMER] = json.encodeToString(TimerConfig.serializer(), config) }
    }

    suspend fun separateConfigs(context: Context): Boolean =
        context.dataStore.data.first()[KEY_SEPARATE] ?: true

    suspend fun setSeparateConfigs(context: Context, separate: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_SEPARATE] = separate }
    }

    /** 启动时预热内存快照 */
    suspend fun prime(context: Context) {
        wallpaperSnapshot = configFlow(context, Mode.WALLPAPER).first()
        fullscreenSnapshot = configFlow(context, Mode.FULLSCREEN).first()
    }
}
