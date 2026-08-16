package com.zhuomo.flowlume.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/** 预设仓库：官方预设（assets，只读）+ 自定义预设（filesDir/presets，可增删改） */
object PresetStore {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    fun officialPresets(context: Context): List<Preset> =
        (context.assets.list("presets") ?: emptyArray())
            .mapNotNull { file ->
                runCatching {
                    json.decodeFromString<Preset>(context.assets.open("presets/$file").bufferedReader().use { it.readText() })
                }.getOrNull()
            }

    suspend fun customPresets(context: Context): List<Preset> = withContext(Dispatchers.IO) {
        (dir(context).listFiles() ?: emptyArray())
            .mapNotNull { f -> runCatching { json.decodeFromString<Preset>(f.readText()) }.getOrNull() }
            .sortedBy { it.name }
    }

    suspend fun saveCustom(context: Context, preset: Preset): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(dir(context), sanitize(preset.name) + ".json")
            file.writeText(json.encodeToString(Preset.serializer(), preset))
            true
        }.getOrDefault(false)
    }

    suspend fun deleteCustom(context: Context, name: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(dir(context), sanitize(name) + ".json")
            file.delete()
        }.getOrDefault(false)
    }

    /** 导入外部 JSON 预设（SAF 读取后调用） */
    suspend fun importJson(context: Context, text: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val preset = json.decodeFromString<Preset>(text)
            saveCustom(context, preset)
            true
        }.getOrDefault(false)
    }

    fun exportJson(preset: Preset): String = json.encodeToString(Preset.serializer(), preset)

    private fun dir(context: Context): File {
        val d = File(context.filesDir, "presets")
        if (!d.exists()) d.mkdirs()
        return d
    }

    private fun sanitize(name: String): String = name.replace(Regex("[^\\w\\u4e00-\\u9fa5-]"), "_")
}
