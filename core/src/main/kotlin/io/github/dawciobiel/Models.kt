package io.github.dawciobiel

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle

class GameConfig(
    var numLanes: Int = 5,
    val initialGameSpeed: Float = 600f,
    var gameSpeed: Float = 600f,
    var animationSpeed: Float = 0.2f,
    var obstacleSpawnInterval: Float = 1.0f,
    val maxGameSpeed: Float = 1800f,
    val virtualWidth: Float = 1000f,
    val virtualHeight: Float = 1500f
)

class Player(var currentLane: Int, var numLanes: Int) {
    var targetLane: Int = currentLane
    var visualX: Float = 0f
    var targetX: Float = 0f
    var rotation: Float = 0f
    val bounds = Rectangle()

    fun update(delta: Float, laneManager: LaneManager, config: GameConfig) {
        targetX = laneManager.getLaneCenterX(targetLane)
        
        val prevX = visualX
        if (Math.abs(visualX - targetX) > 0.1f) {
            visualX = MathUtils.lerp(visualX, targetX, config.animationSpeed)
            val movement = visualX - prevX
            rotation = MathUtils.lerp(rotation, -movement * 1.5f, 0.2f)
        } else {
            visualX = targetX
            currentLane = targetLane
            rotation = MathUtils.lerp(rotation, 0f, 0.2f)
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
        rotation = 0f
    }
}

class SkidMark(var x: Float, var y: Float, var width: Float, var alpha: Float = 0.6f) {
    fun update(delta: Float, gameSpeed: Float) {
        y -= gameSpeed * delta
        alpha -= 0.5f * delta
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
