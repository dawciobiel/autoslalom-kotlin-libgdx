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
    var stateTime: Float = MathUtils.random(0f, 10f)
    var scaleX: Float = 1f
    var scaleY: Float = 1f

    fun update(delta: Float, gameSpeed: Float, laneManager: LaneManager, numLanes: Int) {
        stateTime += delta
        y -= (gameSpeed + extraSpeed) * delta
        
        // Procedural animation based on type
        when (type) {
            ObstacleType.MOVING -> {
                scaleX = 1f + MathUtils.sin(stateTime * 8f) * 0.1f
                scaleY = 1f + MathUtils.cos(stateTime * 8f) * 0.1f
            }
            ObstacleType.WIDE -> {
                scaleY = 1f + MathUtils.sin(stateTime * 4f) * 0.05f
            }
            else -> {
                scaleX = 1f + MathUtils.sin(stateTime * 2f) * 0.02f
                scaleY = 1f + MathUtils.cos(stateTime * 2f) * 0.02f
            }
        }

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
