package io.github.dawciobiel

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils

class GameConfig(
    var numLanes: Int = 5,
    var gameSpeed: Float = 500f, // Prędkość drogi (piksele na sekundę)
    var animationSpeed: Float = 0.2f // Współczynnik lerp dla animacji zmiany pasa
)

class Player(var currentLane: Int, var numLanes: Int) {
    var targetLane: Int = currentLane
    var visualX: Float = 0f
    var targetX: Float = 0f

    fun update(delta: Float, laneManager: LaneManager, config: GameConfig) {
        targetX = laneManager.getLaneCenterX(targetLane)
        
        // Płynny ruch między pasami
        if (Math.abs(visualX - targetX) > 0.1f) {
            visualX = MathUtils.lerp(visualX, targetX, config.animationSpeed)
        } else {
            visualX = targetX
            currentLane = targetLane
        }
    }

    fun moveLeft() {
        if (targetLane > 0) targetLane--
    }

    fun moveRight() {
        if (targetLane < numLanes - 1) targetLane++
    }

    fun reset(newLane: Int, newNumLanes: Int, laneManager: LaneManager) {
        currentLane = newLane
        targetLane = newLane
        numLanes = newNumLanes
        visualX = laneManager.getLaneCenterX(currentLane)
    }
}

class LaneManager(private val config: GameConfig) {
    var screenWidth: Float = 0f
    var screenHeight: Float = 0f
    var laneWidth: Float = 0f

    fun updateSize(width: Int, height: Int) {
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
        laneWidth = screenWidth / config.numLanes
    }

    fun getLaneCenterX(laneIndex: Int): Float {
        return (laneIndex * laneWidth) + (laneWidth / 2f)
    }
}

class AutoslalomGame : ApplicationAdapter() {

    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var config: GameConfig
    private lateinit var laneManager: LaneManager
    private lateinit var player: Player
    
    private var roadOffset: Float = 0f

    override fun create() {
        shapeRenderer = ShapeRenderer()
        config = GameConfig(numLanes = 5)
        laneManager = LaneManager(config)
        
        laneManager.updateSize(Gdx.graphics.width, Gdx.graphics.height)
        
        player = Player(config.numLanes / 2, config.numLanes)
        player.visualX = laneManager.getLaneCenterX(player.currentLane)
        
        Gdx.app.log("AutoslalomGame", "Gra uruchomiona. Strzałki: ruch. Klawisze 3-9: zmiana liczby pasów.")
    }

    override fun render() {
        handleInput()
        update(Gdx.graphics.deltaTime)
        draw()
    }

    private fun handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            player.moveLeft()
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            player.moveRight()
        }

        // Zmiana liczby pasów (3-9)
        for (i in 3..9) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0 + i)) {
                changeLaneCount(i)
            }
        }
    }

    private fun changeLaneCount(newCount: Int) {
        config.numLanes = newCount
        laneManager.updateSize(Gdx.graphics.width, Gdx.graphics.height)
        player.reset(newCount / 2, newCount, laneManager)
        Gdx.app.log("AutoslalomGame", "Zmiana liczby pasów na: $newCount")
    }

    private fun update(delta: Float) {
        player.update(delta, laneManager, config)
        
        // Animacja drogi
        roadOffset -= config.gameSpeed * delta
        if (roadOffset < -100f) {
            roadOffset += 100f
        }
    }

    private fun draw() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Rysowanie pasów i przerywanych linii
        for (i in 0..config.numLanes) {
            val x = i * laneManager.laneWidth
            
            // Linie krawędziowe (ciągłe) lub oddzielające pasy (przerywane)
            if (i == 0 || i == config.numLanes) {
                shapeRenderer.color = Color.WHITE
                shapeRenderer.rect(x - 4, 0f, 8f, laneManager.screenHeight)
            } else {
                shapeRenderer.color = Color.GRAY
                var y = roadOffset
                while (y < laneManager.screenHeight) {
                    if (y + 50f > 0) {
                        shapeRenderer.rect(x - 2, y, 4f, 50f)
                    }
                    y += 100f
                }
            }
        }

        // Rysowanie gracza
        shapeRenderer.color = Color.RED
        val playerWidth = laneManager.laneWidth * 0.5f
        val playerHeight = playerWidth * 1.6f
        shapeRenderer.rect(
            player.visualX - playerWidth / 2,
            80f,
            playerWidth,
            playerHeight
        )
        
        // "Światła" gracza dla lepszego efektu
        shapeRenderer.color = Color.YELLOW
        shapeRenderer.rect(player.visualX - playerWidth / 2 + 5, 80f + playerHeight - 10, 10f, 10f)
        shapeRenderer.rect(player.visualX + playerWidth / 2 - 15, 80f + playerHeight - 10, 10f, 10f)

        shapeRenderer.end()
    }

    override fun resize(width: Int, height: Int) {
        laneManager.updateSize(width, height)
        // Po resize upewniamy się, że gracz jest na właściwej pozycji wizualnej
        player.visualX = laneManager.getLaneCenterX(player.targetLane)
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }
}
