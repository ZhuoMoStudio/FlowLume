package com.zhuomo.flowlume.wallpaper

import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLExt
import android.view.Surface
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidApplicationLogger
import com.badlogic.gdx.backends.android.AndroidFiles
import com.badlogic.gdx.backends.android.AndroidGL20
import com.badlogic.gdx.backends.android.AndroidGL30
import com.zhuomo.flowlume.render.RenderCore

/**
 * 壁纸形态的 GL 宿主：基于 Surface + EGL14 手工创建上下文与渲染线程。
 * LibGDX 无壁纸后端，这是社区标准做法（AndroidApplication 不可用于壁纸）。
 */
class GdxWallpaper(
    private val context: Context,
    private val surface: Surface,
    private val renderCore: RenderCore
) {

    private var thread: Thread? = null
    @Volatile private var running = false
    @Volatile private var paused = false
    @Volatile private var width = 1
    @Volatile private var height = 1
    private var lastTime = 0L

    fun start() {
        if (thread?.isAlive == true) return
        running = true
        thread = Thread({ runLoop() }, "FlowLumeWallpaperGL").apply { start() }
    }

    fun stop() {
        running = false
        thread?.join(1500)
        thread = null
    }

    fun pause() { paused = true }
    fun resume() { paused = false }

    fun setSize(w: Int, h: Int) {
        width = w.coerceAtLeast(1)
        height = h.coerceAtLeast(1)
    }

    private fun runLoop() {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) return
        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) return

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        val attribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 0,
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )
        if (!EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] <= 0) {
            return
        }
        val config = configs[0]
        val contextAttrs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        val eglContext = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, contextAttrs, 0)
        val eglSurface = EGL14.eglCreateWindowSurface(display, config, surface, intArrayOf(EGL14.EGL_NONE), 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT || eglSurface == EGL14.EGL_NO_SURFACE) {
            EGL14.eglTerminate(display)
            return
        }
        if (!EGL14.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)) {
            EGL14.eglDestroySurface(display, eglSurface)
            EGL14.eglDestroyContext(display, eglContext)
            EGL14.eglTerminate(display)
            return
        }

        initGdx()
        renderCore.create()
        renderCore.resize(width, height)
        lastTime = System.nanoTime()

        while (running) {
            if (paused) {
                Thread.sleep(80)
                continue
            }
            val now = System.nanoTime()
            val delta = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastTime = now
            renderCore.render(delta)
            EGL14.eglSwapBuffers(display, eglSurface)
        }

        renderCore.dispose()
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(display, eglSurface)
        EGL14.eglDestroyContext(display, eglContext)
        EGL14.eglTerminate(display)
    }

    /** 手工初始化 LibGDX 静态环境（壁纸无 AndroidApplication） */
    private fun initGdx() {
        Gdx.gl20 = AndroidGL20()
        Gdx.gl = Gdx.gl20
        Gdx.gl30 = AndroidGL30()
        Gdx.app = AndroidApplicationLogger()
        Gdx.files = AndroidFiles(context)
    }
}
