package com.zhuomo.flowlume.render

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable

/**
 * GPU 粒子批：所有粒子类动效共享一个动态 VBO，一次 draw 完成。
 * 顶点布局：position(x, y, size, alpha) + color(r, g, b, a)
 */
class ParticleBatch(private val capacity: Int = 4096) : Disposable {

    class Particle(
        var x: Float, var y: Float, var size: Float,
        var vx: Float, var vy: Float,
        var life: Float, var maxLife: Float,
        var r: Float, var g: Float, var b: Float, var a: Float
    )

    private val particles = ArrayList<Particle>()
    private val mesh = Mesh(
        false, capacity, 0,
        VertexAttribute(VertexAttributes.Usage.Position, 4, "a_position"),
        VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color")
    )
    private val vertices = FloatArray(capacity * 8)

    fun spawn(
        x: Float, y: Float, size: Float,
        vx: Float, vy: Float, life: Float,
        r: Float, g: Float, b: Float, a: Float
    ) {
        if (particles.size >= capacity) return
        particles.add(Particle(x, y, size, vx, vy, life, life, r, g, b, a))
    }

    fun update(delta: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= delta
            if (p.life <= 0f) { it.remove(); continue }
            p.x += p.vx * delta
            p.y += p.vy * delta
        }
    }

    fun flush(shader: ShaderProgram) {
        if (particles.isEmpty()) return
        var v = 0
        for (p in particles) {
            val fade = (p.life / p.maxLife).coerceIn(0f, 1f)
            vertices[v++] = p.x
            vertices[v++] = p.y
            vertices[v++] = p.size
            vertices[v++] = p.a * fade
            vertices[v++] = p.r
            vertices[v++] = p.g
            vertices[v++] = p.b
            vertices[v++] = 1f
        }
        mesh.setVertices(vertices, 0, v)
        mesh.render(shader, GL20.GL_POINTS, 0, particles.size)
    }

    fun clear() = particles.clear()

    override fun dispose() = mesh.dispose()
}
