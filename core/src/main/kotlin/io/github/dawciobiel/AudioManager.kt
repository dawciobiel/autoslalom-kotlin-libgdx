package io.github.dawciobiel

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Disposable

class AudioManager : Disposable {
    private val sounds = mutableMapOf<String, Sound>()
    private var currentMusic: Music? = null
    
    var soundEnabled: Boolean = true
    var musicEnabled: Boolean = true
    var soundVolume: Float = 0.5f
    var musicVolume: Float = 0.3f

    fun loadSound(name: String, path: String) {
        try {
            sounds[name] = Gdx.audio.newSound(Gdx.files.internal(path))
        } catch (e: Exception) {
            Gdx.app.error("AudioManager", "Could not load sound: $path")
            System.err.println(e.message);
        }
    }

    fun playSound(name: String, pitch: Float = 1f) {
        if (soundEnabled) {
            sounds[name]?.play(soundVolume, pitch, 0f)
        }
    }

    fun playMusic(path: String, loop: Boolean = true) {
        if (!musicEnabled) return
        
        stopMusic()
        try {
            currentMusic = Gdx.audio.newMusic(Gdx.files.internal(path)).apply {
                isLooping = loop
                volume = musicVolume
                play()
            }
        } catch (e: Exception) {
            Gdx.app.error("AudioManager", "Could not load music: $path")
        }
    }

    fun stopMusic() {
        currentMusic?.stop()
        currentMusic?.dispose()
        currentMusic = null
    }

    override fun dispose() {
        sounds.values.forEach { it.dispose() }
        sounds.clear()
        stopMusic()
    }
}
