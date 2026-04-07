package io.github.dawciobiel

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx

class AutoslalomGame : Game() {
    val audioManager = AudioManager()

    override fun create() {
        Gdx.app.log("AutoslalomGame", "Gra uruchomiona.")
        setScreen(MenuScreen(this))
    }

    override fun dispose() {
        super.dispose()
        audioManager.dispose()
    }
}

