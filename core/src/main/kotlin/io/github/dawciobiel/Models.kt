package io.github.dawciobiel

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle

data class GameConfig(
    val virtualWidth: Float = 1000f,
    val virtualHeight: Float = 1500f,
    var numLanes: Int = 5,
    var gameSpeed: Float = 400f,
    val initialGameSpeed: Float = 400f,
    val maxGameSpeed: Float = 1200f,
    var obstacleSpawnInterval: Float = 1.0f,
    val animationSpeed: Float = 0.1f
)

class LaneManager(val config: GameConfig) {
    var laneWidth: Float = 0f
    
    init {
        updateLaneWidth()
    }

    fun updateLaneWidth() {
        laneWidth = config.virtualWidth / config.numLanes
    }

    fun getLaneCenterX(lane: Int): Float {
        return (lane + 0.5f) * laneWidth
    }
}

enum class ObstacleType {
    STANDARD, LONG, WIDE, MOVING, PEDESTRIAN
}

class Obstacle(
    var lane: Int,
    var y: Float,
    var width: Float,
    var height: Float,
    val type: ObstacleType,
    var speed: Float = 0f
) {
    val bounds = Rectangle(0f, y, width, height)
    var scaleX: Float = 1f
    var scaleY: Float = 1f
    var stateTime: Float = MathUtils.random(0f, 100f)

    fun update(delta: Float, gameSpeed: Float, laneManager: LaneManager, numLanes: Int) {
        stateTime += delta
        y -= (gameSpeed + speed) * delta
        val centerX = laneManager.getLaneCenterX(lane)
        bounds.set(centerX - width / 2f, y, width, height)
    }

    fun draw(renderer: ShapeRenderer, laneWidth: Float) {
        if (type == ObstacleType.PEDESTRIAN) {
            drawPedestrian(renderer, laneWidth)
        } else {
            drawSimpleObstacle(renderer)
        }
    }

    private fun drawSimpleObstacle(renderer: ShapeRenderer) {
        renderer.color = when (type) {
            ObstacleType.STANDARD -> Color.ORANGE
            ObstacleType.LONG -> Color.BROWN
            ObstacleType.WIDE -> Color.PURPLE
            ObstacleType.MOVING -> Color.CYAN
            else -> Color.WHITE
        }
        renderer.rect(
            bounds.x, bounds.y,
            bounds.width / 2f, bounds.height / 2f,
            bounds.width, bounds.height,
            scaleX, scaleY, 0f
        )
    }

    private fun drawPedestrian(renderer: ShapeRenderer, laneWidth: Float) {
        val centerX = bounds.x + bounds.width / 2f
        val centerY = bounds.y
        val limbWidth = laneWidth * 0.1f
        val limbHeight = limbWidth * 2f
        
        val walkFreq = 10f
        val walkAmp = 20f
        val legAngle = MathUtils.sin(stateTime * walkFreq) * walkAmp
        
        renderer.color = Color.CORAL // Skin/Clothes
        
        // Body
        val bodyWidth = bounds.width * 0.8f
        val bodyHeight = bounds.height * 0.5f
        renderer.rect(centerX - bodyWidth/2f, centerY + limbHeight, bodyWidth, bodyHeight)
        
        // Head
        val headRadius = limbWidth * 1.2f
        renderer.circle(centerX, centerY + limbHeight + bodyHeight + headRadius, headRadius)
        
        // Legs
        renderer.color = Color.DARK_GRAY
        renderer.rect(centerX - limbWidth, centerY, limbWidth/2f, limbHeight, limbWidth, limbHeight, 1f, 1f, legAngle)
        renderer.rect(centerX + limbWidth/2f, centerY, limbWidth/2f, limbHeight, limbWidth, limbHeight, 1f, 1f, -legAngle)
    }
}

class SkidMark(val x: Float, var y: Float, val width: Float) {
    var alpha: Float = 0.6f
    fun update(delta: Float, gameSpeed: Float) {
        y -= gameSpeed * delta
        alpha -= delta * 0.5f
    }
}

enum class WeaponType {
    NONE, PISTOL, RIFLE, SWORD
}

class Weapon(
    val type: WeaponType,
    var relativeX: Float = 0f,
    var relativeY: Float = 0f,
    var rotation: Float = 0f,
    var scaleX: Float = 1f,
    var scaleY: Float = 1f
) {
    val bounds = Rectangle()

    fun update(delta: Float, playerX: Float, playerY: Float, playerRotation: Float, playerScaleX: Float, playerScaleY: Float) {
        when (type) {
            WeaponType.PISTOL -> {
                scaleX = 1f + MathUtils.sin(delta * 10f) * 0.05f
                scaleY = 1f + MathUtils.cos(delta * 10f) * 0.05f
                rotation = playerRotation
            }
            WeaponType.SWORD -> {
                rotation = playerRotation + MathUtils.sin(delta * 5f) * 15f
            }
            WeaponType.RIFLE -> {
                scaleX = 1f + MathUtils.sin(delta * 5f) * 0.03f
                scaleY = 1f + MathUtils.cos(delta * 5f) * 0.03f
                rotation = playerRotation
            }
            WeaponType.NONE -> {}
        }

        val dims = getDimensions()
        val width = dims.first
        val height = dims.second
        bounds.set(
            playerX + relativeX * playerScaleX - width / 2f,
            playerY + relativeY * playerScaleY - height / 2f,
            width,
            height
        )
    }

    fun getDimensions(): Pair<Float, Float> {
        return when (type) {
            WeaponType.PISTOL -> Pair(20f, 60f)
            WeaponType.RIFLE -> Pair(25f, 100f)
            WeaponType.SWORD -> Pair(10f, 80f)
            WeaponType.NONE -> Pair(0f, 0f)
        }
    }
}
