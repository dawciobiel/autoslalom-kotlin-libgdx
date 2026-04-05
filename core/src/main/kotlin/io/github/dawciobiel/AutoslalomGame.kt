package io.github.dawciobiel

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.viewport.FitViewport

class GameConfig(
    var numLanes: Int = 5,
    val initialGameSpeed: Float = 600f,
    var gameSpeed: Float = 600f, // Prędkość drogi (jednostki na sekundę)
    var animationSpeed: Float = 0.2f, // Współczynnik lerp dla animacji zmiany pasa
    var obstacleSpawnInterval: Float = 1.0f, // Jak często pojawia się przeszkoda (sekundy)
    val maxGameSpeed: Float = 1800f,
    val virtualWidth: Float = 1000f,
    val virtualHeight: Float = 1500f // "Dłuższa" trasa w pionie
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

enum class ObstacleType {
    STANDARD, LONG, WIDE, MOVING
}

class Obstacle(
    var lane: Int,
    var y: Float,
    val width: Float,
    val height: Float,
    val type: ObstacleType = ObstacleType.STANDARD,
    val extraSpeed: Float = 0f
) {
    val bounds = Rectangle()

    fun update(delta: Float, gameSpeed: Float, laneManager: LaneManager, numLanes: Int) {
        y -= (gameSpeed + extraSpeed) * delta
        val x = if (type == ObstacleType.WIDE) {
            val safeLane = if (lane >= numLanes - 1) numLanes - 2 else lane
            laneManager.getLaneCenterX(safeLane) + laneManager.laneWidth / 2f
        } else {
            laneManager.getLaneCenterX(lane)
        }
        bounds.set(x - width / 2, y, width, height)
    }
}

class LaneManager(private val config: GameConfig) {
    var laneWidth: Float = 0f

    init {
        updateLaneWidth()
    }

    fun updateLaneWidth() {
        laneWidth = config.virtualWidth / config.numLanes
    }

    fun getLaneCenterX(laneIndex: Int): Float {
        return (laneIndex * laneWidth) + (laneWidth / 2f)
    }
}

class AutoslalomGame : ApplicationAdapter() {

    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var config: GameConfig
    private lateinit var laneManager: LaneManager
    private lateinit var player: Player
    
    private val obstacles = mutableListOf<Obstacle>()
    private var spawnTimer: Float = 0f
    private var roadOffset: Float = 0f
    private var isGameOver: Boolean = false
    private var gameTime: Float = 0f
    private var lastSpeedIncreaseTime: Float = 0f

    override fun create() {
        shapeRenderer = ShapeRenderer()
        config = GameConfig(numLanes = 5)
        
        camera = OrthographicCamera()
        viewport = FitViewport(config.virtualWidth, config.virtualHeight, camera)
        camera.position.set(config.virtualWidth / 2f, config.virtualHeight / 2f, 0f)
        
        laneManager = LaneManager(config)
        
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
        gameTime = 0f
        lastSpeedIncreaseTime = 0f
        config.gameSpeed = config.initialGameSpeed
        config.obstacleSpawnInterval = 1.0f
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
        laneManager.updateLaneWidth()
        player.reset(newCount / 2, newCount, laneManager)
        obstacles.clear() // Czyścimy przeszkody przy zmianie układu pasów
        Gdx.app.log("AutoslalomGame", "Zmiana liczby pasów na: $newCount")
    }

    private fun update(delta: Float) {
        player.update(delta, laneManager, config)
        gameTime += delta

        // Progresja trudności co 5 sekund
        if (gameTime - lastSpeedIncreaseTime >= 5f && config.gameSpeed < config.maxGameSpeed) {
            config.gameSpeed = Math.min(config.gameSpeed * 1.1f, config.maxGameSpeed)
            // Skracamy interwał spawnu proporcjonalnie do prędkości, ale nie mniej niż 0.3s
            config.obstacleSpawnInterval = Math.max(1.0f * (config.initialGameSpeed / config.gameSpeed), 0.3f)
            lastSpeedIncreaseTime = gameTime
            Gdx.app.log("AutoslalomGame", "Trudność wzrasta! Prędkość: ${config.gameSpeed.toInt()}")
        }
        
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
            obstacle.update(delta, config.gameSpeed, laneManager, config.numLanes)
            
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
        val type = ObstacleType.values()[MathUtils.random(0, ObstacleType.values().size - 1)]
        val lane = MathUtils.random(0, config.numLanes - 1)
        val baseWidth = laneManager.laneWidth * 0.45f
        val baseHeight = baseWidth * 1.5f

        when (type) {
            ObstacleType.STANDARD -> {
                obstacles.add(Obstacle(lane, config.virtualHeight, baseWidth, baseHeight, type))
            }
            ObstacleType.LONG -> {
                obstacles.add(Obstacle(lane, config.virtualHeight, baseWidth, baseHeight * 2.5f, type))
            }
            ObstacleType.WIDE -> {
                // Zajmuje ok. 1.5 pasa, co wymaga precyzyjnego omijania
                obstacles.add(Obstacle(lane, config.virtualHeight, laneManager.laneWidth * 1.2f, baseHeight, type))
            }
            ObstacleType.MOVING -> {
                // Porusza się nieco szybciej (wolniejsze auto na drodze)
                obstacles.add(Obstacle(lane, config.virtualHeight, baseWidth, baseHeight, type, 150f))
            }
        }
    }

    private fun draw() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Rysowanie pasów i przerywanych linii
        for (i in 0..config.numLanes) {
            val x = i * laneManager.laneWidth
            
            if (i == 0 || i == config.numLanes) {
                shapeRenderer.color = Color.WHITE
                shapeRenderer.rect(x - 4, 0f, 8f, config.virtualHeight)
            } else {
                shapeRenderer.color = Color.GRAY
                var y = roadOffset
                while (y < config.virtualHeight) {
                    if (y + 50f > 0) {
                        shapeRenderer.rect(x - 2, y, 4f, 50f)
                    }
                    y += 100f
                }
            }
        }

        // Rysowanie przeszkód
        for (obstacle in obstacles) {
            shapeRenderer.color = when (obstacle.type) {
                ObstacleType.STANDARD -> Color.ORANGE
                ObstacleType.LONG -> Color.BROWN
                ObstacleType.WIDE -> Color.PURPLE
                ObstacleType.MOVING -> Color.CYAN
            }
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
        viewport.update(width, height)
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }
}
