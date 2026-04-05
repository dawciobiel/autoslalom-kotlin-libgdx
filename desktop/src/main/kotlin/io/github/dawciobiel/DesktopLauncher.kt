package io.github.dawciobiel

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Autoslalom")
        setWindowedMode(1280, 720)
        setForegroundFPS(60)
        useVsync(true)
        setResizable(true)
        // Ikona (opcjonalnie, dodaj plik do assets)
        // setWindowIcon("icon128.png", "icon64.png", "icon32.png")
    }

    Lwjgl3Application(AutoslalomGame(), config)
}