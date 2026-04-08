package io.github.dawciobiel

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
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
    private val player = PlayerVehicle(config.numLanes / 2, config.numLanes)
    
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

    private val highscoreManager = HighscoreManager()
    private val remoteScores = mutableListOf<RemoteScoreResponse>()
    private var isUploading: Boolean = false
    private var playerTexture: Texture? = null
    private val audio = game.audioManager

    init {
        camera.position.set(config.virtualWidth / 2f, config.virtualHeight / 2f, 0f)
        player.visualX = laneManager.getLaneCenterX(player.currentLane)
        highScore = highscoreManager.loadScores().firstOrNull()?.score ?: 0
        
        loadAssets()
    }

    private fun loadAssets() {
        try {
            playerTexture = Texture(Gdx.files.internal(player.type.texturePath))
        } catch (e: Exception) {
            Gdx.app.error("GameScreen", "Could not load player texture: ${player.type.texturePath}")
        }
        
        audio.loadSound("crash", "audio/sfx/crash.wav")
        audio.loadSound("skid", "audio/sfx/skid.wav")
        audio.loadSound("engine", "audio/sfx/engine.wav")
        audio.loadSound("select", "audio/sfx/select.wav")
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
        remoteScores.clear()
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            cycleVehicleType()
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

    private fun cycleVehicleType() {
        val types = VehicleType.values()
        val nextType = types[(player.type.ordinal + 1) % types.size]
        player.changeType(nextType)
        audio.playSound("select")
        
        playerTexture?.dispose()
        try {
            playerTexture = Texture(Gdx.files.internal(player.type.texturePath))
        } catch (e: Exception) {
            Gdx.app.error("GameScreen", "Could not load player texture: ${player.type.texturePath}")
            playerTexture = null
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
            
            if ((gameTime * 10).toInt() % 5 == 0) {
                audio.playSound("skid", 0.8f + Math.abs(player.rotation) / 20f)
            }
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
                if (!player.isInvulnerable) {
                    player.takeDamage(20)
                    shakeAmount = 30f
                    shakeDuration = 0.5f
                    audio.playSound("crash")
                    
                    // Impact squash effect
                    player.scaleX = 1.5f
                    player.scaleY = 0.5f
                    
                    if (player.health <= 0) {
                        isGameOver = true
                        val playerName = System.getProperty("user.name") ?: "Player"
                        
                        // Save local
                        highscoreManager.saveScore(playerName, score)
                        highScore = highscoreManager.loadScores().firstOrNull()?.score ?: score
                        
                        // Save remote to Supabase
                        isUploading = true
                        game.supabaseHighscoreManager.saveScore(playerName, score) { success ->
                            isUploading = false
                            // After saving, fetch top 10 to update the list
                            game.supabaseHighscoreManager.fetchTopScores(10) { scores ->
                                remoteScores.clear()
                                remoteScores.addAll(scores)
                            }
                        }
                        
                        // Initial fetch even if save is in progress
                        game.supabaseHighscoreManager.fetchTopScores(10) { scores ->
                            if (remoteScores.isEmpty()) {
                                remoteScores.addAll(scores)
                            }
                        }
                    }
                    iterator.remove()
                    continue
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
            // Shadow
            shapeRenderer.color.set(0f, 0f, 0f, 0.3f)
            shapeRenderer.rect(
                obstacle.bounds.x + 10f,
                obstacle.bounds.y - 10f,
                obstacle.bounds.width / 2f,
                obstacle.bounds.height / 2f,
                obstacle.bounds.width,
                obstacle.bounds.height,
                obstacle.scaleX, obstacle.scaleY,
                0f
            )

            // Main shape
            shapeRenderer.color = when (obstacle.type) {
                ObstacleType.STANDARD -> Color.ORANGE
                ObstacleType.LONG -> Color.BROWN
                ObstacleType.WIDE -> Color.PURPLE
                ObstacleType.MOVING -> Color.CYAN
            }
            shapeRenderer.rect(
                obstacle.bounds.x,
                obstacle.bounds.y,
                obstacle.bounds.width / 2f,
                obstacle.bounds.height / 2f,
                obstacle.bounds.width,
                obstacle.bounds.height,
                obstacle.scaleX, obstacle.scaleY,
                0f
            )
        }

        shapeRenderer.end()

        if (playerTexture == null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            if (isGameOver) shapeRenderer.color = Color.GRAY else shapeRenderer.color = Color.RED
            shapeRenderer.rect(
                player.bounds.x,
                player.bounds.y + player.bounceY,
                player.bounds.width / 2,
                player.bounds.height / 2,
                player.bounds.width,
                player.bounds.height,
                player.scaleX, player.scaleY,
                player.rotation
            )
            shapeRenderer.end()
        }

        batch.projectionMatrix = camera.combined
        batch.begin()
        
        playerTexture?.let {
            val isVisible = !player.isInvulnerable || (gameTime * 10).toInt() % 2 == 0
            if (isVisible) {
                // Determine drawing dimensions based on rotation offset to prevent squashing
                val isRotated = (player.type.rotationOffset / 90f).toInt() % 2 != 0
                val drawW = if (isRotated) player.bounds.height else player.bounds.width
                val drawH = if (isRotated) player.bounds.width else player.bounds.height
                
                // Shadow
                batch.setColor(0f, 0f, 0f, 0.4f)
                batch.draw(
                    it,
                    player.visualX - drawW / 2 + 15f,
                    player.bounds.y + player.bounds.height / 2 - drawH / 2 + player.bounceY - 15f,
                    drawW / 2,
                    drawH / 2,
                    drawW,
                    drawH,
                    player.scaleX, player.scaleY,
                    player.rotation + player.type.rotationOffset,
                    0, 0,
                    it.width,
                    it.height,
                    false, false
                )

                // Main sprite
                batch.setColor(1f, 1f, 1f, 1f)
                batch.draw(
                    it,
                    player.visualX - drawW / 2,
                    player.bounds.y + player.bounds.height / 2 - drawH / 2 + player.bounceY,
                    drawW / 2,
                    drawH / 2,
                    drawW,
                    drawH,
                    player.scaleX, player.scaleY,
                    player.rotation + player.type.rotationOffset,
                    0, 0,
                    it.width,
                    it.height,
                    false, false
                )
            }
        }
        
        font.color = Color.WHITE
        font.draw(batch, "Score: $score", 20f, config.virtualHeight - 20f)
        font.draw(batch, "Best: $highScore", 20f, config.virtualHeight - 80f)
        font.draw(batch, "Vehicle: ${player.type.name}", 20f, config.virtualHeight - 140f)
        font.draw(batch, "Health: ${player.health}%", 20f, config.virtualHeight - 200f)

        if (isGameOver) {
            font.color = Color.RED
            font.draw(batch, "GAME OVER!", config.virtualWidth / 2f - 150f, config.virtualHeight / 2f + 400f)
            
            font.color = Color.WHITE
            font.data.setScale(1.5f)
            
            // Local Scores
            font.draw(batch, "--- LOCAL TOP 5 ---", config.virtualWidth / 2f - 350f, config.virtualHeight / 2f + 320f)
            val localTop = highscoreManager.loadScores().take(5)
            localTop.forEachIndexed { index, entry ->
                font.draw(batch, "${index + 1}. ${entry.name}: ${entry.score}", config.virtualWidth / 2f - 350f, config.virtualHeight / 2f + 280f - index * 35f)
            }

            // Global Scores
            font.draw(batch, "--- GLOBAL TOP 5 ---", config.virtualWidth / 2f + 50f, config.virtualHeight / 2f + 320f)
            if (remoteScores.isEmpty()) {
                font.draw(batch, "Loading...", config.virtualWidth / 2f + 50f, config.virtualHeight / 2f + 280f)
            } else {
                remoteScores.take(5).forEachIndexed { index, entry ->
                    font.draw(batch, "${index + 1}. ${entry.player_name}: ${entry.score}", config.virtualWidth / 2f + 50f, config.virtualHeight / 2f + 280f - index * 35f)
                }
            }

            if (isUploading) {
                font.color = Color.CYAN
                font.draw(batch, "Uploading your score...", config.virtualWidth / 2f - 150f, config.virtualHeight / 2f + 360f)
            }

            font.data.setScale(3f)
            font.color = Color.YELLOW
            font.draw(batch, "PRESS SPACE TO RESTART", config.virtualWidth / 2f - 300f, config.virtualHeight / 2f - 300f)
            font.draw(batch, "PRESS ESC FOR MENU", config.virtualWidth / 2f - 250f, config.virtualHeight / 2f - 400f)
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
        playerTexture?.dispose()
    }
}
