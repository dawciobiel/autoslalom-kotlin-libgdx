package io.github.dawciobiel

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle

class GameConfig(
    var numLanes: Int = 5,
    var gameSpeed: Float = 500f, // Prędkość drogi (piksele na sekundę)
    var animationSpeed: Float = 0.2f, // Współczynnik lerp dla animacji zmiany pasa
    var obstacleSpawnInterval: Float = 1.0f // Jak często pojawia się przeszkoda (sekundy)
)

class Player(var currentLane: Int, var numLanes: Int) {
    var targetLane: Int = currentLane
    var visualX: Float = 0f
    var targetX: Float = 0f
    val bounds = Rectangle()

    fun update(delta: Float, laneManager: LaneManager, config: GameConfig) {
        targetX = laneManager.getLaneCenterX(targetLane)
        
        // Płynny ruch między pasami
        if (Math.abs(visualX - targetX) > 0.1f) {
            visualX = MathUtils.lerp(visualX, targetX, config.animationSpeed)
        } else {
            visualX = targetX
            currentLane = targetLane
        }

        val width = laneManager.laneWidth * 0.5f
        val height = width * 1.6f
        bounds.set(visualX - width / 2, 80f, width, height)
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

class Obstacle(var lane: Int, var y: Float, val width: Float, val height: Float) {
    val bounds = Rectangle()

    fun update(delta: Float, gameSpeed: Float, laneManager: LaneManager) {
        y -= gameSpeed * delta
        val x = laneManager.getLaneCenterX(lane)
        bounds.set(x - width / 2, y, width, height)
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
    
    private val obstacles = mutableListOf<Obstacle>()
    private var spawnTimer: Float = 0f
    private var roadOffset: Float = 0f
    private var isGameOver: Boolean = false

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
        if (!isGameOver) {
            handleInput()
            update(Gdx.graphics.deltaTime)
        } else {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                resetGame()
            }
        }
        draw()
    }

    private fun resetGame() {
        isGameOver = false
        obstacles.clear()
        spawnTimer = 0f
        player.reset(config.numLanes / 2, config.numLanes, laneManager)
        Gdx.app.log("AutoslalomGame", "Gra zresetowana.")
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
        obstacles.clear() // Czyścimy przeszkody przy zmianie układu pasów
        Gdx.app.log("AutoslalomGame", "Zmiana liczby pasów na: $newCount")
    }

    private fun update(delta: Float) {
        player.update(delta, laneManager, config)
        
        // Animacja drogi
        roadOffset -= config.gameSpeed * delta
        if (roadOffset < -100f) {
            roadOffset += 100f
        }

        // Zarządzanie przeszkodami
        spawnTimer += delta
        if (spawnTimer >= config.obstacleSpawnInterval) {
            spawnTimer = 0f
            spawnObstacle()
        }

        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            obstacle.update(delta, config.gameSpeed, laneManager)
            
            // Kolizja
            if (player.bounds.overlaps(obstacle.bounds)) {
                isGameOver = true
                Gdx.app.log("AutoslalomGame", "KOLIZJA! Naciśnij SPACJĘ, aby spróbować ponownie.")
            }

            // Usuwanie przeszkód poza ekranem
            if (obstacle.y + obstacle.height < 0) {
                iterator.remove()
            }
        }
    }

    private fun spawnObstacle() {
        val lane = MathUtils.random(0, config.numLanes - 1)
        val width = laneManager.laneWidth * 0.4f
        val height = width * 1.2f
        obstacles.add(Obstacle(lane, laneManager.screenHeight, width, height))
    }

    private fun draw() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Rysowanie pasów i przerywanych linii
        for (i in 0..config.numLanes) {
            val x = i * laneManager.laneWidth
            
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

        // Rysowanie przeszkód
        shapeRenderer.color = Color.ORANGE
        for (obstacle in obstacles) {
            shapeRenderer.rect(
                obstacle.bounds.x,
                obstacle.bounds.y,
                obstacle.bounds.width,
                obstacle.bounds.height
            )
        }

        // Rysowanie gracza
        if (isGameOver) shapeRenderer.color = Color.GRAY else shapeRenderer.color = Color.RED
        shapeRenderer.rect(
            player.bounds.x,
            player.bounds.y,
            player.bounds.width,
            player.bounds.height
        )
        
        // Światła
        if (!isGameOver) {
            shapeRenderer.color = Color.YELLOW
            shapeRenderer.rect(player.bounds.x + 5, player.bounds.y + player.bounds.height - 10, 10f, 10f)
            shapeRenderer.rect(player.bounds.x + player.bounds.width - 15, player.bounds.y + player.bounds.height - 10, 10f, 10f)
        }

        shapeRenderer.end()
    }

    override fun resize(width: Int, height: Int) {
        laneManager.updateSize(width, height)
        player.visualX = laneManager.getLaneCenterX(player.targetLane)
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }
}
