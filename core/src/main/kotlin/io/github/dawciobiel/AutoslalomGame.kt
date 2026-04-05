package io.github.dawciobiel

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch

class AutoslalomGame : ApplicationAdapter() {

    private lateinit var batch: SpriteBatch

    override fun create() {
        batch = SpriteBatch()
        Gdx.app.log("AutoslalomGame", "Gra uruchomiona — LibGDX ${com.badlogic.gdx.Version.VERSION}")
    }

    override fun render() {
        // Czyszczenie ekranu
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val delta = Gdx.graphics.deltaTime
        update(delta)
        draw()
    }

    private fun update(delta: Float) {
        // Logika gry — tutaj będzie rosło
    }

    private fun draw() {
        batch.begin()
        // Renderowanie — tutaj będzie rosło
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        Gdx.app.log("AutoslalomGame", "Resize: ${width}x${height}")
    }

    override fun dispose() {
        batch.dispose()
    }
}
