package com.zhuomo.flowlume.render

import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException

/** 着色器编译与缓存（双宿主各自持有编译实例，源码共享） */
object ShaderLibrary {

    private val cache = HashMap<String, ShaderProgram>()

    fun get(name: String, vertex: String, fragment: String): ShaderProgram =
        cache.getOrPut(name) { compile(name, vertex, fragment) }

    private fun compile(name: String, vertex: String, fragment: String): ShaderProgram {
        val program = ShaderProgram(vertex, fragment)
        if (!program.isCompiled) {
            throw GdxRuntimeException("Shader [$name] compile failed: ${program.log}")
        }
        return program
    }

    fun dispose() {
        cache.values.forEach { it.dispose() }
        cache.clear()
    }
}
