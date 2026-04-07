package io.github.dawciobiel

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle

enum class VehicleType(
    val acceleration: Float,
    val animationSpeedMultiplier: Float,
    val texturePath: String,
    val rotationOffset: Float = 0f,
    val aspectRatio: Float = 1.6f
) {
    TRASHMASTER(80f, 0.8f, "textures/vehicles/trashmaster.png", 90f, 2.4f),
    SEDAN(100f, 1.0f, "textures/vehicles/car/Previews/sedan.png"),
    RACE(200f, 1.5f, "textures/vehicles/car/Previews/race.png"),
    POLICE(150f, 1.2f, "textures/vehicles/car/Previews/police.png"),
    TRUCK(50f, 0.7f, "textures/vehicles/car/Previews/truck.png")
}

class PlayerVehicle(var currentLane: Int, var numLanes: Int) {
    var type: VehicleType = VehicleType.TRASHMASTER
    var targetLane: Int = currentLane
    var visualX: Float = 0f
    var targetX: Float = 0f
    var rotation: Float = 0f
    val bounds = Rectangle()
    
    // Additional properties for future use
    var currentSpeed: Float = 0f
    var health: Int = 100
    var isInvulnerable: Boolean = false
    var invulnerabilityTimer: Float = 0f

    fun update(delta: Float, laneManager: LaneManager, config: GameConfig) {
        targetX = laneManager.getLaneCenterX(targetLane)
        
        val prevX = visualX
        val animSpeed = config.animationSpeed * type.animationSpeedMultiplier
        
        if (Math.abs(visualX - targetX) > 0.1f) {
            visualX = MathUtils.lerp(visualX, targetX, animSpeed)
            val movement = visualX - prevX
            rotation = MathUtils.lerp(rotation, -movement * 1.5f, 0.2f)
        } else {
            visualX = targetX
            currentLane = targetLane
            rotation = MathUtils.lerp(rotation, 0f, 0.2f)
        }

        val width = laneManager.laneWidth * 0.5f
        val height = width * type.aspectRatio
        bounds.set(visualX - width / 2, 80f, width, height)
        
        if (invulnerabilityTimer > 0) {
            invulnerabilityTimer -= delta
            if (invulnerabilityTimer <= 0) {
                isInvulnerable = false
            }
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
        rotation = 0f
        health = 100
        isInvulnerable = false
        invulnerabilityTimer = 0f
    }
    
    fun changeType(newType: VehicleType) {
        type = newType
    }
    
    fun takeDamage(amount: Int) {
        if (!isInvulnerable) {
            health -= amount
            isInvulnerable = true
            invulnerabilityTimer = 1.0f
        }
    }
}
