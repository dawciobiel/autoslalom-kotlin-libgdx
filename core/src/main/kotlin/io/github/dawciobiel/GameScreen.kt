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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class GameScreen(private val game: AutoslalomGame) : ScreenAdapter() {

    private val shapeRenderer = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(3f) }
    private val config = GameConfig(numLanes = 5)
    private val camera = OrthographicCamera()
    private val viewport = FitViewport(config.virtualWidth, config.virtualHeight, camera)
    private val laneManager = LaneManager(config)
    private val player = PlayerVehicle(config.numLanes / 2, config.numLanes, laneManager, config)
    
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
    private val audio = game.audioManager
    
    private var currentWeaponIndex: Int = 0
    private val availableWeapons = listOf(WeaponType.NONE, WeaponType.PISTOL, WeaponType.RIFLE, WeaponType.SWORD)


    init {
        camera.position.set(config.virtualWidth / 2f, config.virtualHeight / 2f, 0f)
        player.visualX = laneManager.getLaneCenterX(player.currentLane)
        highScore = highscoreManager.loadScores().firstOrNull()?.score ?: 0
        
        player.changeType(player.type)
        setPlayerWeapon(availableWeapons[currentWeaponIndex])

        loadAssets()
    }

    private fun loadAssets() {
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
        player.reset(config.numLanes / 2, config.numLanes, laneManager, config)
        
        player.changeType(player.type)
        setPlayerWeapon(availableWeapons[currentWeaponIndex])
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            cycleWeapon()
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
        val types = VehicleType.entries
        val nextTypeIndex = (player.type.ordinal + 1) % types.size
        val nextType = types[nextTypeIndex]
        player.changeType(nextType)
        audio.playSound("select")
        
        setPlayerWeapon(availableWeapons[currentWeaponIndex])
    }
    
    private fun cycleWeapon() {
        currentWeaponIndex = (currentWeaponIndex + 1) % availableWeapons.size
        setPlayerWeapon(availableWeapons[currentWeaponIndex])
        audio.playSound("select")
    }
    
    private fun setPlayerWeapon(weaponType: WeaponType) {
        val newWeapon = Weapon(weaponType)
        when (weaponType) {
            WeaponType.PISTOL -> {
                newWeapon.relativeY = -player.bounds.height * 0.2f
                newWeapon.relativeX = player.bounds.width * 0.4f
            }
            WeaponType.RIFLE -> {
                newWeapon.relativeY = -player.bounds.height * 0.1f
                newWeapon.relativeX = player.bounds.width * 0.5f
            }
            WeaponType.SWORD -> {
                newWeapon.relativeY = -player.bounds.height * 0.3f
                newWeapon.relativeX = player.bounds.width * 0.3f
            }
            WeaponType.NONE -> {}
        }
        player.weapon = newWeapon
    }

    private fun changeLaneCount(newCount: Int) {
        config.numLanes = newCount
        laneManager.updateLaneWidth()
        player.reset(newCount / 2, newCount, laneManager, config)
        obstacles.clear()
        setPlayerWeapon(availableWeapons[currentWeaponIndex]) 
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
        player.update(delta)
        gameTime += delta
        score += 1

        if (abs(player.rotation) > 5f || abs(player.scaleX - 1f) > 0.1f || abs(player.scaleY - 1f) > 0.1f) {
            val offset = player.bounds.width * 0.35f
            skidMarks.add(SkidMark(player.visualX - offset, player.bounds.y, 8f))
            skidMarks.add(SkidMark(player.visualX + offset, player.bounds.y, 8f))
            
            if ((gameTime * 10).toInt() % 5 == 0) {
                audio.playSound("skid", 0.8f + abs(player.rotation) / 20f)
            }
        }

        if (gameTime - lastSpeedIncreaseTime >= 5f && config.gameSpeed < config.maxGameSpeed) {
            config.gameSpeed = min(config.gameSpeed * 1.1f, config.maxGameSpeed)
            config.obstacleSpawnInterval = max(1.0f * (config.initialGameSpeed / config.gameSpeed), 0.3f)
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
                    
                    if (player.health <= 0) {
                        isGameOver = true
                        val playerName = System.getProperty("user.name") ?: "Player"
                        
                        highscoreManager.saveScore(playerName, score)
                        highScore = highscoreManager.loadScores().firstOrNull()?.score ?: score
                        
                        isUploading = true
                        game.supabaseHighscoreManager.saveScore(playerName, score) { _ ->
                            isUploading = false
                            game.supabaseHighscoreManager.fetchTopScores(10) { scores ->
                                remoteScores.clear()
                                remoteScores.addAll(scores)
                            }
                        }
                        
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
        val types = ObstacleType.entries
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
            ObstacleType.PEDESTRIAN -> {
                obstacles.add(Obstacle(lane, config.virtualHeight, laneManager.laneWidth * 0.3f, laneManager.laneWidth * 0.5f, type))
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

        // Draw road lanes
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

        // Draw obstacles using their internal draw method
        for (obstacle in obstacles) {
            obstacle.draw(shapeRenderer, laneManager.laneWidth)
        }
        
        // Draw Player and Weapon using PlayerVehicle's draw method
        player.draw(shapeRenderer)

        shapeRenderer.end()

        batch.projectionMatrix = camera.combined
        batch.begin()

        font.color = Color.WHITE
        font.draw(batch, "Score: $score", 20f, config.virtualHeight - 20f)
        font.draw(batch, "Best: $highScore", 20f, config.virtualHeight - 80f)
        font.draw(batch, "Vehicle: ${player.type.name}", 20f, config.virtualHeight - 140f)
        font.draw(batch, "Health: ${player.health}%", 20f, config.virtualHeight - 200f)
        font.draw(batch, "Weapon: ${player.weapon?.type?.name ?: "NONE"}", 20f, config.virtualHeight - 260f)


        if (isGameOver) {
            font.color = Color.RED
            font.draw(batch, "GAME OVER!", config.virtualWidth / 2f - 150f, config.virtualHeight / 2f + 400f)
            
            font.color = Color.WHITE
            font.data.setScale(1.5f)
            
            font.draw(batch, "--- LOCAL TOP 5 ---", config.virtualWidth / 2f - 350f, config.virtualHeight / 2f + 320f)
            val localTop = highscoreManager.loadScores().take(5)
            localTop.forEachIndexed { index, entry ->
                font.draw(batch, "${index + 1}. ${entry.name}: ${entry.score}", config.virtualWidth / 2f - 350f, config.virtualHeight / 2f + 280f - index * 35f)
            }

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
                font.draw(batch, "Uploading your score...", config.virtualWidth / 2f - 190f, config.virtualHeight / 2f + 360f)
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
    }
}
