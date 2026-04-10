package io.github.dawciobiel

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

enum class VehicleType(
    val acceleration: Float,
    val animationSpeedMultiplier: Float,
    val texturePath: String,
    val rotationOffset: Float = 0f,
    val aspectRatio: Float = 1.6f,
    val bodyColor: Color = Color.RED,
    val limbColor: Color = Color.DARK_GRAY
) {
    TRASHMASTER(80f, 0.8f, "textures/vehicles/trashmaster.png", 90f, 2.4f, Color.LIGHT_GRAY, Color.BLACK),
    SEDAN(100f, 1.0f, "textures/vehicles/car/Previews/sedan.png", 0f, 1.6f, Color.BLUE, Color.DARK_GRAY),
    RACE(200f, 1.5f, "textures/vehicles/car/Previews/race.png", 0f, 1.6f, Color.RED, Color.BLACK),
    POLICE(150f, 1.2f, "textures/vehicles/car/Previews/police.png", 90f, 1.8f, Color.WHITE, Color.DARK_GRAY),
    TRUCK(50f, 0.7f, "textures/vehicles/car/Previews/truck.png", 0f, 2.5f, Color.ORANGE, Color.BROWN)
}

class PlayerVehicle(
    initialLane: Int,
    initialNumLanes: Int,
    private var laneManager: LaneManager,
    private var config: GameConfig
) {
    var type: VehicleType = VehicleType.SEDAN
    var currentLane: Int = initialLane
    var targetLane: Int = initialLane
    var visualX: Float = 0f
    var targetX: Float = 0f
    var rotation: Float = 0f
    val bounds = Rectangle()
    var numLanes: Int = initialNumLanes 
    
    var weapon: Weapon? = null

    var bounceY: Float = 0f
    var scaleX: Float = 1f
    var scaleY: Float = 1f
    private var stateTime: Float = 0f
    
    var health: Int = 100
    var isInvulnerable: Boolean = false
    var invulnerabilityTimer: Float = 0f

    init {
        visualX = laneManager.getLaneCenterX(currentLane)
        targetX = visualX
        updateBounds()
    }

    fun update(delta: Float) {
        stateTime += delta
        targetX = laneManager.getLaneCenterX(targetLane)
        
        val animSpeed = config.animationSpeed * type.animationSpeedMultiplier
        
        if (Math.abs(visualX - targetX) > 0.1f) {
            visualX = MathUtils.lerp(visualX, targetX, animSpeed)
            rotation = MathUtils.lerp(rotation, -(targetX - visualX) * 0.5f, 0.2f)
            scaleX = MathUtils.lerp(scaleX, 0.85f, 0.15f)
            scaleY = MathUtils.lerp(scaleY, 1.15f, 0.15f)
        } else {
            visualX = targetX
            currentLane = targetLane
            rotation = MathUtils.lerp(rotation, 0f, 0.2f)
            scaleX = MathUtils.lerp(scaleX, 1f, 0.1f)
            scaleY = MathUtils.lerp(scaleY, 1f, 0.1f)
        }

        val bounceFreq = 10f * (config.gameSpeed / config.initialGameSpeed)
        val bounceAmp = 2f * (config.gameSpeed / config.initialGameSpeed)
        bounceY = MathUtils.sin(stateTime * bounceFreq) * bounceAmp

        weapon?.update(delta, visualX, bounds.y + bounds.height / 2f, rotation + type.rotationOffset, scaleX, scaleY)
        updateBounds()
        
        if (invulnerabilityTimer > 0) {
            invulnerabilityTimer -= delta
            if (invulnerabilityTimer <= 0) {
                isInvulnerable = false
            }
        }
    }

    private fun updateBounds() {
        val baseWidth = laneManager.laneWidth * 0.5f
        val height = baseWidth * type.aspectRatio
        bounds.set(visualX - baseWidth / 2f, 80f, baseWidth, height)
    }

    fun moveLeft() {
        if (targetLane > 0) {
            targetLane--
            scaleX = 0.8f
            scaleY = 1.2f
        }
    }

    fun moveRight() {
        if (targetLane < numLanes - 1) {
            targetLane++
            scaleX = 0.8f
            scaleY = 1.2f
        }
    }

    fun reset(newLane: Int, newNumLanes: Int, newLaneManager: LaneManager, newConfig: GameConfig) {
        currentLane = newLane
        targetLane = newLane
        numLanes = newNumLanes 
        this.laneManager = newLaneManager
        this.config = newConfig
        visualX = laneManager.getLaneCenterX(currentLane)
        targetX = visualX
        rotation = 0f
        bounceY = 0f
        scaleX = 1f
        scaleY = 1f
        stateTime = 0f
        health = 100
        isInvulnerable = false
        invulnerabilityTimer = 0f
        weapon = null
        updateBounds()
    }
    
    fun changeType(newType: VehicleType) {
        type = newType
        updateBounds()
    }
    
    fun takeDamage(amount: Int) {
        if (!isInvulnerable) {
            health -= amount
            isInvulnerable = true
            invulnerabilityTimer = 1.0f
        }
    }

    fun draw(renderer: ShapeRenderer) { 
        renderer.color = Color.BLACK.cpy().apply { a = 0.3f }
        drawBody(renderer, visualX + 5f, bounds.y - 5f, rotation, scaleX, scaleY, Color.BLACK.cpy().apply { a = 0.3f })
        
        renderer.color = type.bodyColor
        drawBody(renderer, visualX, bounds.y, rotation, scaleX, scaleY, type.bodyColor)

        weapon?.let {
            renderer.color = when(it.type) {
                WeaponType.PISTOL -> Color.GRAY
                WeaponType.RIFLE -> Color.DARK_GRAY
                WeaponType.SWORD -> Color.YELLOW
                else -> Color.WHITE
            }
            renderer.rect(it.bounds.x, it.bounds.y, it.bounds.width / 2f, it.bounds.height / 2f, it.bounds.width, it.bounds.height, it.scaleX, it.scaleY, it.rotation)
        }
    }

    private fun drawBody(renderer: ShapeRenderer, x: Float, y: Float, rotation: Float, scaleX: Float, scaleY: Float, color: Color) {
        renderer.color = color
        val baseWidth = laneManager.laneWidth * 0.5f
        val height = baseWidth * type.aspectRatio
        val centerX = x
        val centerY = y + height / 2f + bounceY

        val bodyHeight = height * 0.7f
        val bodyWidth = baseWidth * 0.9f
        val cabinHeight = height * 0.3f
        val cabinWidth = baseWidth * 0.7f

        renderer.rect(
            centerX - bodyWidth / 2f,
            centerY - bodyHeight / 2f,
            bodyWidth / 2f,
            bodyHeight / 2f,
            bodyWidth,
            bodyHeight,
            scaleX, scaleY,
            rotation
        )
        
        renderer.rect(
            centerX - cabinWidth / 2f,
            centerY - bodyHeight / 2f + bodyHeight * 0.4f,
            cabinWidth / 2f,
            cabinHeight / 2f,
            cabinWidth,
            cabinHeight,
            scaleX, scaleY,
            rotation
        )

        val wheelRadius = baseWidth * 0.15f
        val wheelYOffset = -bodyHeight / 2f + 5f
        val wheelXOffset = bodyWidth / 2f * 0.75f

        renderer.color = if (color.a < 1f) color else Color.BLACK
        renderer.rect(centerX - wheelXOffset - wheelRadius, centerY + wheelYOffset, wheelRadius*2, wheelRadius*2)
        renderer.rect(centerX + wheelXOffset - wheelRadius, centerY + wheelYOffset, wheelRadius*2, wheelRadius*2)
        renderer.rect(centerX - wheelXOffset - wheelRadius, centerY + wheelYOffset + bodyHeight * 0.6f, wheelRadius*2, wheelRadius*2)
        renderer.rect(centerX + wheelXOffset - wheelRadius, centerY + wheelYOffset + bodyHeight * 0.6f, wheelRadius*2, wheelRadius*2)
    }
}
