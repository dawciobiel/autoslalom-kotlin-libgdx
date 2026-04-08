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
    
    // Procedural animation properties
    var bounceY: Float = 0f
    var scaleX: Float = 1f
    var scaleY: Float = 1f
    private var stateTime: Float = 0f
    
    // Additional properties for future use
    var currentSpeed: Float = 0f
    var health: Int = 100
    var isInvulnerable: Boolean = false
    var invulnerabilityTimer: Float = 0f

    fun update(delta: Float, laneManager: LaneManager, config: GameConfig) {
        stateTime += delta
        targetX = laneManager.getLaneCenterX(targetLane)
        
        val prevX = visualX
        val animSpeed = config.animationSpeed * type.animationSpeedMultiplier
        
        if (Math.abs(visualX - targetX) > 0.1f) {
            visualX = MathUtils.lerp(visualX, targetX, animSpeed)
            val movement = visualX - prevX
            
            // Tilting based on movement
            rotation = MathUtils.lerp(rotation, -movement * 1.5f, 0.2f)
            
            // Squash and stretch during lane change
            scaleX = MathUtils.lerp(scaleX, 0.9f, 0.1f)
            scaleY = MathUtils.lerp(scaleY, 1.1f, 0.1f)
        } else {
            visualX = targetX
            currentLane = targetLane
            rotation = MathUtils.lerp(rotation, 0f, 0.2f)
            
            // Return to normal scale
            scaleX = MathUtils.lerp(scaleX, 1f, 0.1f)
            scaleY = MathUtils.lerp(scaleY, 1f, 0.1f)
        }

        // Bouncing effect based on game speed
        val bounceFreq = 10f * (config.gameSpeed / config.initialGameSpeed)
        val bounceAmp = 2f * (config.gameSpeed / config.initialGameSpeed)
        bounceY = MathUtils.sin(stateTime * bounceFreq) * bounceAmp

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
        if (targetLane > 0) {
            targetLane--
            // Initial squash on command
            scaleX = 0.8f
            scaleY = 1.2f
        }
    }

    fun moveRight() {
        if (targetLane < numLanes - 1) {
            targetLane++
            // Initial squash on command
            scaleX = 0.8f
            scaleY = 1.2f
        }
    }

    fun reset(newLane: Int, newNumLanes: Int, laneManager: LaneManager) {
        currentLane = newLane
        targetLane = newLane
        numLanes = newNumLanes
        visualX = laneManager.getLaneCenterX(currentLane)
        rotation = 0f
        bounceY = 0f
        scaleX = 1f
        scaleY = 1f
        stateTime = 0f
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
