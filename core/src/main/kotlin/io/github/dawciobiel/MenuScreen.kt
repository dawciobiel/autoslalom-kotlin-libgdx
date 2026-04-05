package io.github.dawciobiel

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.viewport.FitViewport

class MenuScreen(private val game: AutoslalomGame) : ScreenAdapter() {

    private val stage = Stage(FitViewport(1000f, 1500f))
    private val skin = createBasicSkin()

    override fun show() {
        Gdx.input.inputProcessor = stage

        val table = Table()
        table.setFillParent(true)
        stage.addActor(table)

        val title = Label("AUTOSLALOM", skin)
        val startButton = TextButton("START GAME", skin)
        val exitButton = TextButton("EXIT", skin)

        startButton.addListener(object : com.badlogic.gdx.scenes.scene2d.InputListener() {
            override fun touchDown(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                game.setScreen(GameScreen(game))
                return true
            }
        })

        exitButton.addListener(object : com.badlogic.gdx.scenes.scene2d.InputListener() {
            override fun touchDown(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                Gdx.app.exit()
                return true
            }
        })

        table.add(title).padBottom(100f).row()
        table.add(startButton).width(400f).height(100f).padBottom(40f).row()
        table.add(exitButton).width(400f).height(100f)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
        skin.dispose()
    }
}
