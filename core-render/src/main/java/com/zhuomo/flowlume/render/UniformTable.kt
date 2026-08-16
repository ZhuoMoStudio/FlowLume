package com.zhuomo.flowlume.render

import com.badlogic.gdx.graphics.glutils.ShaderProgram

/** 每帧 uniform 打包表：动效只写表，绘制前统一应用到 shader */
class UniformTable {

    private val floats = HashMap<String, Float>()
    private val arrays = HashMap<String, FloatArray>()
    private val vec2s = HashMap<String, FloatArray>()
    private val samplers = HashMap<String, Int>()

    fun set(name: String, value: Float) { floats[name] = value }
    fun set(name: String, value: Int) { samplers[name] = value }
    fun set(name: String, value: FloatArray) { if (value.size == 2) vec2s[name] = value else arrays[name] = value }

    fun get(name: String, default: Float = 0f): Float = floats[name] ?: default

    fun reset() {
        floats.clear()
        arrays.clear()
        vec2s.clear()
        samplers.clear()
    }

    fun applyTo(shader: ShaderProgram) {
        for ((k, v) in floats) if (shader.hasUniform(k)) shader.setUniformf(k, v)
        for ((k, v) in vec2s) if (shader.hasUniform(k)) shader.setUniformf(k, v[0], v[1])
        for ((k, v) in samplers) if (shader.hasUniform(k)) shader.setUniformi(k, v)
        for ((k, v) in arrays) if (shader.hasUniform(k)) shader.setUniform1fv(k, v, 0, v.size)
    }
}
