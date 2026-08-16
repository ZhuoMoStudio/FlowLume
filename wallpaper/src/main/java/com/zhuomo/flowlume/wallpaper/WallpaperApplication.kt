package com.zhuomo.flowlume.wallpaper

import android.util.Log
import com.badlogic.gdx.Application
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.ApplicationLogger
import com.badlogic.gdx.Audio
import com.badlogic.gdx.Files
import com.badlogic.gdx.Graphics
import com.badlogic.gdx.Input
import com.badlogic.gdx.LifecycleListener
import com.badlogic.gdx.Net
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.backends.android.AndroidApplicationLogger
import com.badlogic.gdx.utils.Clipboard

/**
 * 壁纸形态的极简 Gdx.app 实现。
 * 壁纸无法使用 AndroidApplication（依赖 Activity/GLSurfaceView），
 * 仅需满足 ShaderProgram 等对 Gdx.app 日志调用的需求。
 */
class WallpaperApplication : Application {

    private var logLevel = Application.LOG_INFO
    private var logger: ApplicationLogger = AndroidApplicationLogger()

    override fun getApplicationListener(): ApplicationListener? = null
    override fun getGraphics(): Graphics? = null
    override fun getAudio(): Audio? = null
    override fun getInput(): Input? = null
    override fun getFiles(): Files? = null
    override fun getNet(): Net? = null

    override fun log(tag: String, message: String) { Log.i(tag, message) }
    override fun log(tag: String, message: String, exception: Throwable) { Log.i(tag, message, exception) }
    override fun error(tag: String, message: String) { Log.e(tag, message) }
    override fun error(tag: String, message: String, exception: Throwable) { Log.e(tag, message, exception) }
    override fun debug(tag: String, message: String) { Log.d(tag, message) }
    override fun debug(tag: String, message: String, exception: Throwable) { Log.d(tag, message, exception) }

    override fun setLogLevel(logLevel: Int) { this.logLevel = logLevel }
    override fun getLogLevel(): Int = logLevel
    override fun setApplicationLogger(logger: ApplicationLogger) { this.logger = logger }
    override fun getApplicationLogger(): ApplicationLogger = logger

    override fun getType(): Application.ApplicationType = Application.ApplicationType.Android
    override fun getVersion(): Int = android.os.Build.VERSION.SDK_INT

    override fun getJavaHeap(): Long =
        Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    override fun getNativeHeap(): Long = getJavaHeap()

    override fun getPreferences(name: String): Preferences =
        throw UnsupportedOperationException("WallpaperApplication: preferences not available")

    override fun getClipboard(): Clipboard? = null
    override fun postRunnable(runnable: Runnable) { runnable.run() }
    override fun exit() { /* no-op */ }
    override fun addLifecycleListener(listener: LifecycleListener) { /* no-op */ }
    override fun removeLifecycleListener(listener: LifecycleListener) { /* no-op */ }
}
