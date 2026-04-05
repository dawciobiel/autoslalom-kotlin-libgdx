package io.github.dawciobiel

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx

class AutoslalomGame : Game() {
    override fun create() {
        Gdx.app.log("AutoslalomGame", "Gra uruchomiona.")
        setScreen(MenuScreen(this))
    }
}

