package io.github.dawciobiel

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach

class PlayerVehicleTest {
    private lateinit var config: GameConfig
    private lateinit var laneManager: LaneManager
    private lateinit var player: PlayerVehicle

    @BeforeEach
    fun setUp() {
        config = GameConfig(numLanes = 5)
        laneManager = LaneManager(config)
        player = PlayerVehicle(config.numLanes / 2, config.numLanes)
        player.visualX = laneManager.getLaneCenterX(player.currentLane)
    }

    @Test
    fun testInitialState() {
        assertEquals(2, player.currentLane)
        assertEquals(2, player.targetLane)
        assertEquals(100, player.health)
        assertFalse(player.isInvulnerable)
    }

    @Test
    fun testMoveLeft() {
        player.moveLeft()
        assertEquals(1, player.targetLane)
    }

    @Test
    fun testMoveRight() {
        player.moveRight()
        assertEquals(3, player.targetLane)
    }

    @Test
    fun testMoveLeftLimit() {
        player.targetLane = 0
        player.moveLeft()
        assertEquals(0, player.targetLane)
    }

    @Test
    fun testMoveRightLimit() {
        player.targetLane = 4
        player.moveRight()
        assertEquals(4, player.targetLane)
    }

    @Test
    fun testTakeDamage() {
        player.takeDamage(20)
        assertEquals(80, player.health)
        assertTrue(player.isInvulnerable)
        assertEquals(1.0f, player.invulnerabilityTimer)
    }

    @Test
    fun testInvulnerabilityAfterDamage() {
        player.takeDamage(20)
        player.takeDamage(20) // Should not take damage again
        assertEquals(80, player.health)
    }

    @Test
    fun testChangeType() {
        player.changeType(VehicleType.RACE)
        assertEquals(VehicleType.RACE, player.type)
        assertEquals(200f, player.type.acceleration)
    }

    @Test
    fun testInitialType() {
        assertEquals(VehicleType.TRASHMASTER, player.type)
        assertEquals(80f, player.type.acceleration)
    }

    @Test
    fun testReset() {
        player.takeDamage(20)
        player.moveLeft()
        player.reset(2, 5, laneManager)
        
        assertEquals(2, player.currentLane)
        assertEquals(2, player.targetLane)
        assertEquals(100, player.health)
        assertFalse(player.isInvulnerable)
    }
}
