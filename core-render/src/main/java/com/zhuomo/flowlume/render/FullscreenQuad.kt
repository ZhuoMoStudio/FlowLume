package com.zhuomo.flowlume.render

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable

/** 全屏四边形（三角形带），用于多 Pass 渲染 */
class FullscreenQuad : Disposable {

    private val mesh = Mesh(
        true, 4, 6,
        VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
        VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0")
    )

    init {
        mesh.setVertices(
            floatArrayOf(
                -1f, -1f, 0f, 0f,
                 1f, -1f, 1f, 0f,
                 1f,  1f, 1f, 1f,
                -1f,  1f, 0f, 1f
            )
        )
        mesh.setIndices(shortArrayOf(0, 1, 2, 0, 2, 3))
    }

    fun draw(shader: ShaderProgram) {
        mesh.render(shader, GL20.GL_TRIANGLES)
    }

    override fun dispose() {
        mesh.dispose()
    }
}
