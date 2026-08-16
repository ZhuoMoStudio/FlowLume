package com.zhuomo.flowlume.app.util

import android.content.Context
import android.content.res.Configuration
import com.zhuomo.flowlume.app.di.AppContainer
import java.util.Locale

/** 应用内语言切换（默认跟随系统） */
object LocaleHelper {
    const val LANG_SYSTEM = "system"
    const val LANG_ZH = "zh"
    const val LANG_EN = "en"

    /** 在 Activity.attachBaseContext 中调用 */
    fun apply(base: Context): Context {
        val lang = AppContainer.appLang
        if (lang == LANG_SYSTEM || lang.isBlank()) return base
        val locale = runCatching { Locale(lang) }.getOrNull() ?: return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    /** 用于 Compose 预览/资源获取（可选） */
    fun isZh(): Boolean {
        val lang = AppContainer.appLang
        return if (lang == LANG_SYSTEM) Locale.getDefault().language == "zh" else lang == LANG_ZH
    }
}
