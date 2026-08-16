package com.zhuomo.flowlume.app.fullscreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.zhuomo.flowlume.app.di.AppContainer
import com.zhuomo.flowlume.config.TimeAnchorConfig
import com.zhuomo.flowlume.timer.TimerEngine
import com.zhuomo.flowlume.timer.TimeTextStyle
import kotlin.math.roundToInt

/** 全屏渲染屏幕：流体渲染 + 时间文字（gdx-freetype 位图字体） */
class FlowScreen(private val game: FullscreenGame) : ScreenAdapter() {

    private var width = 1
    private var height = 1
    private val margin = 32

    override fun render(delta: Float) {
        game.renderCore.render(delta)
        drawTimerText()
    }

    override fun resize(width: Int, height: Int) {
        this.width = width.coerceAtLeast(1)
        this.height = height.coerceAtLeast(1)
        game.renderCore.resize(this.width, this.height)
    }

    private fun drawTimerText() {
        val state = AppContainer.timerEngine.state.value
        if (!state.running && state.elapsedMs <= 0L) return

        val style = TimeTextStyle.fromConfig(AppContainer.timerConfig)
        val density = Gdx.graphics.density.coerceAtLeast(1f)
        val fontSizePx = (style.fontSizeSp * density).roundToInt()
        val font = game.fontCache.get(fontSizePx, style.strokeWidthDp)

        val color = Color(style.color.toInt())
        color.a = style.alpha
        font.color = color

        val text = TimerEngine.format(state.displayMs)
        val layout = GlyphLayout(font, text)
        val (x, y) = positionFor(layout, style)

        game.batch.begin()
        font.draw(game.batch, layout, x, y)
        game.batch.end()
    }

    private fun positionFor(layout: GlyphLayout, style: TimeTextStyle): Pair<Float, Float> {
        if (style.anchor == TimeAnchorConfig.CUSTOM) {
            return style.customX.toFloat() to style.customY.toFloat()
        }
        val w = layout.width
        val h = layout.height
        return when (style.anchor) {
            TimeAnchorConfig.CENTER -> (width - w) / 2f to (height + h) / 2f
            TimeAnchorConfig.TOP_LEFT -> margin.toFloat() to (height - margin).toFloat()
            TimeAnchorConfig.TOP_CENTER -> (width - w) / 2f to (height - margin).toFloat()
            TimeAnchorConfig.TOP_RIGHT -> (width - w - margin).toFloat() to (height - margin).toFloat()
            TimeAnchorConfig.MID_LEFT -> margin.toFloat() to (height + h) / 2f
            TimeAnchorConfig.MID_RIGHT -> (width - w - margin).toFloat() to (height + h) / 2f
            TimeAnchorConfig.BOTTOM_LEFT -> margin.toFloat() to margin.toFloat()
            TimeAnchorConfig.BOTTOM_CENTER -> (width - w) / 2f to margin.toFloat()
            TimeAnchorConfig.BOTTOM_RIGHT -> (width - w - margin).toFloat() to margin.toFloat()
            TimeAnchorConfig.CUSTOM -> style.customX.toFloat() to style.customY.toFloat()
        }
    }
}
