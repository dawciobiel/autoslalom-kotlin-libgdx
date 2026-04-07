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
