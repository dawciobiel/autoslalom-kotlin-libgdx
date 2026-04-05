package io.github.dawciobiel

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.viewport.FitViewport

class GameScreen(private val game: AutoslalomGame) : ScreenAdapter() {

    private val shapeRenderer = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(3f) }
    private val config = GameConfig(numLanes = 5)
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(config.virtualWidth, config.virtualHeight, camera)
    private val laneManager = LaneManager(config)
    private val player = Player(config.numLanes / 2, config.numLanes)
    
    private val obstacles = mutableListOf<Obstacle>()
    private val skidMarks = mutableListOf<SkidMark>()
    private var spawnTimer: Float = 0f
    private var roadOffset: Float = 0f
    private var isGameOver: Boolean = false
    private var gameTime: Float = 0f
    private var lastSpeedIncreaseTime: Float = 0f
    private var score: Int = 0
    private var highScore: Int = 0
    
    private var shakeAmount: Float = 0f
    private var shakeDuration: Float = 0f

    private val prefs = Gdx.app.getPreferences("AutoslalomPrefs")

    init {
        camera.position.set(config.virtualWidth / 2f, config.virtualHeight / 2f, 0f)
        player.visualX = laneManager.getLaneCenterX(player.currentLane)
        highScore = prefs.getInteger("highScore", 0)
    }

    override fun show() {
        Gdx.input.inputProcessor = null
    }

    override fun render(delta: Float) {
        updateVisualEffects(delta)

        if (!isGameOver) {
            handleInput()
            update(delta)
        } else {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                resetGame()
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                game.setScreen(MenuScreen(game))
            }
        }
        draw()
    }

    private fun resetGame() {
        isGameOver = false
        obstacles.clear()
        skidMarks.clear()
        spawnTimer = 0f
        gameTime = 0f
        score = 0
        shakeAmount = 0f
        shakeDuration = 0f
        lastSpeedIncreaseTime = 0f
        config.gameSpeed = config.initialGameSpeed
        config.obstacleSpawnInterval = 1.0f
        player.reset(config.numLanes / 2, config.numLanes, laneManager)
    }

    private fun handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            player.moveLeft()
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            player.moveRight()
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(MenuScreen(game))
        }

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
        obstacles.clear()
    }

    private fun updateVisualEffects(delta: Float) {
        if (shakeDuration > 0) {
            shakeDuration -= delta
            shakeAmount = MathUtils.lerp(shakeAmount, 0f, 0.1f)
        } else {
            shakeAmount = 0f
            shakeDuration = 0f
        }

        val currentSpeed = if (isGameOver) 0f else config.gameSpeed
        val skidIterator = skidMarks.iterator()
        while (skidIterator.hasNext()) {
            val mark = skidIterator.next()
            mark.update(delta, currentSpeed)
            if (mark.y < -100f || mark.alpha <= 0f) skidIterator.remove()
        }
    }

    private fun update(delta: Float) {
        player.update(delta, laneManager, config)
        gameTime += delta
        score += 1

        if (Math.abs(player.rotation) > 5f) {
            val offset = player.bounds.width * 0.35f
            skidMarks.add(SkidMark(player.visualX - offset, player.bounds.y, 8f))
            skidMarks.add(SkidMark(player.visualX + offset, player.bounds.y, 8f))
        }

        if (gameTime - lastSpeedIncreaseTime >= 5f && config.gameSpeed < config.maxGameSpeed) {
            config.gameSpeed = Math.min(config.gameSpeed * 1.1f, config.maxGameSpeed)
            config.obstacleSpawnInterval = Math.max(1.0f * (config.initialGameSpeed / config.gameSpeed), 0.3f)
            lastSpeedIncreaseTime = gameTime
        }
        
        roadOffset -= config.gameSpeed * delta
        if (roadOffset < -120f) {
            roadOffset += 120f
        }

        spawnTimer += delta
        if (spawnTimer >= config.obstacleSpawnInterval) {
            spawnTimer = 0f
            spawnObstacle()
        }

        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            obstacle.update(delta, config.gameSpeed, laneManager, config.numLanes)
            
            if (player.bounds.overlaps(obstacle.bounds)) {
                isGameOver = true
                shakeAmount = 30f
                shakeDuration = 0.5f
                if (score > highScore) {
                    highScore = score
                    prefs.putInteger("highScore", highScore)
                    prefs.flush()
                }
            }

            if (obstacle.y + obstacle.height < 0) {
                iterator.remove()
                score += 100
            }
        }
    }

    private fun spawnObstacle() {
        val types = ObstacleType.values()
        val type = types[MathUtils.random(0, types.size - 1)]
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
                obstacles.add(Obstacle(lane, config.virtualHeight, laneManager.laneWidth * 1.2f, baseHeight, type))
            }
            ObstacleType.MOVING -> {
                obstacles.add(Obstacle(lane, config.virtualHeight, baseWidth, baseHeight, type, 150f))
            }
        }
    }

    private fun draw() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (shakeDuration > 0) {
            camera.position.set(
                config.virtualWidth / 2f + MathUtils.random(-shakeAmount, shakeAmount),
                config.virtualHeight / 2f + MathUtils.random(-shakeAmount, shakeAmount),
                0f
            )
        } else {
            camera.position.set(config.virtualWidth / 2f, config.virtualHeight / 2f, 0f)
        }

        camera.update()
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        for (i in 0..config.numLanes) {
            val x = i * laneManager.laneWidth
            
            if (i == 0 || i == config.numLanes) {
                shapeRenderer.color = Color.WHITE
                shapeRenderer.rect(x - 4, 0f, 8f, config.virtualHeight)
            } else {
                shapeRenderer.color = Color.GRAY
                var y = roadOffset
                val dashHeight = 50f + (config.gameSpeed / config.initialGameSpeed) * 30f
                while (y < config.virtualHeight) {
                    if (y + dashHeight > 0) {
                        shapeRenderer.rect(x - 2, y, 4f, dashHeight)
                    }
                    y += 120f
                }
            }
        }

        for (mark in skidMarks) {
            shapeRenderer.color.set(0f, 0f, 0f, mark.alpha)
            shapeRenderer.rect(mark.x - mark.width / 2, mark.y, mark.width, 30f)
        }

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

        if (isGameOver) shapeRenderer.color = Color.GRAY else shapeRenderer.color = Color.RED
        shapeRenderer.rect(
            player.bounds.x,
            player.bounds.y,
            player.bounds.width / 2,
            player.bounds.height / 2,
            player.bounds.width,
            player.bounds.height,
            1f, 1f,
            player.rotation
        )
        
        if (!isGameOver) {
            shapeRenderer.color = Color.YELLOW
            val rad = player.rotation * MathUtils.degreesToRadians
            val cos = MathUtils.cos(rad)
            val sin = MathUtils.sin(rad)
            
            val lx = -player.bounds.width * 0.35f
            val rx = player.bounds.width * 0.35f
            val fy = player.bounds.height * 0.45f
            
            shapeRenderer.rect(player.visualX + lx * cos - fy * sin - 5, player.bounds.y + player.bounds.height / 2 + lx * sin + fy * cos - 5, 10f, 10f)
            shapeRenderer.rect(player.visualX + rx * cos - fy * sin - 5, player.bounds.y + player.bounds.height / 2 + rx * sin + fy * cos - 5, 10f, 10f)
        }

        shapeRenderer.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = Color.WHITE
        font.draw(batch, "Score: $score", 20f, config.virtualHeight - 20f)
        font.draw(batch, "Best: $highScore", 20f, config.virtualHeight - 80f)

        if (isGameOver) {
            font.color = Color.RED
            font.draw(batch, "GAME OVER!", config.virtualWidth / 2f - 150f, config.virtualHeight / 2f + 50f)
            font.color = Color.YELLOW
            font.draw(batch, "PRESS SPACE TO RESTART", config.virtualWidth / 2f - 300f, config.virtualHeight / 2f - 50f)
            font.draw(batch, "PRESS ESC FOR MENU", config.virtualWidth / 2f - 250f, config.virtualHeight / 2f - 150f)
        }
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    override fun dispose() {
        shapeRenderer.dispose()
        batch.dispose()
        font.dispose()
    }
}
