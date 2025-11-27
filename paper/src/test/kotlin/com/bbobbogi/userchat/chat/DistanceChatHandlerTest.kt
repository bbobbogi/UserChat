package com.bbobbogi.userchat.chat

import com.bbobbogi.userchat.config.UserChatConfig
import com.bbobbogi.userchat.service.IUserNameProvider
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class DistanceChatHandlerTest {
    private lateinit var config: UserChatConfig
    private lateinit var userNameProvider: IUserNameProvider
    private lateinit var handler: DistanceChatHandler
    private lateinit var world: World

    @BeforeEach
    fun setUp() {
        config = mock()
        userNameProvider = mock()
        world = mock()

        whenever(config.distanceEnabled).thenReturn(true)
        whenever(config.distanceRange).thenReturn(100)
        whenever(config.formatDistanceChat(any(), any(), any())).thenReturn(Component.text("formatted"))

        handler = DistanceChatHandler(config, userNameProvider)
    }

    @Test
    fun `handleChat returns -1 when distance chat is disabled`() {
        val player = createMockPlayer(UUID.randomUUID(), "TestPlayer", 0.0, 0.0, 0.0)
        whenever(config.distanceEnabled).thenReturn(false)

        val result = handler.handleChat(player, "hello")

        assertEquals(-1, result)
    }

    @Test
    fun `handleChat sends message to nearby players only`() {
        val sender = createMockPlayer(UUID.randomUUID(), "Sender", 0.0, 0.0, 0.0)
        val nearbyPlayer = createMockPlayer(UUID.randomUUID(), "Nearby", 50.0, 0.0, 0.0)
        val farPlayer = createMockPlayer(UUID.randomUUID(), "Far", 200.0, 0.0, 0.0)

        whenever(world.players).thenReturn(listOf(sender, nearbyPlayer, farPlayer))
        whenever(userNameProvider.getPlayerName(sender)).thenReturn("Sender")
        whenever(userNameProvider.getDisplayName(sender)).thenReturn("Sender")

        val result = handler.handleChat(sender, "hello")

        // sender (0,0,0), nearby (50,0,0) are within range 100
        // far (200,0,0) is outside range
        assertEquals(2, result)
        verify(sender).sendMessage(any<Component>())
        verify(nearbyPlayer).sendMessage(any<Component>())
        verify(farPlayer, never()).sendMessage(any<Component>())
    }

    @Test
    fun `handleChat returns count of players who received message`() {
        val sender = createMockPlayer(UUID.randomUUID(), "Sender", 0.0, 0.0, 0.0)
        val player1 = createMockPlayer(UUID.randomUUID(), "Player1", 10.0, 0.0, 0.0)
        val player2 = createMockPlayer(UUID.randomUUID(), "Player2", 20.0, 0.0, 0.0)
        val player3 = createMockPlayer(UUID.randomUUID(), "Player3", 30.0, 0.0, 0.0)

        whenever(world.players).thenReturn(listOf(sender, player1, player2, player3))
        whenever(userNameProvider.getPlayerName(sender)).thenReturn("Sender")
        whenever(userNameProvider.getDisplayName(sender)).thenReturn("Sender")

        val result = handler.handleChat(sender, "hello")

        assertEquals(4, result)
    }

    @Test
    fun `handleChat uses configured distance range`() {
        val sender = createMockPlayer(UUID.randomUUID(), "Sender", 0.0, 0.0, 0.0)
        val player1 = createMockPlayer(UUID.randomUUID(), "Player1", 30.0, 0.0, 0.0)
        val player2 = createMockPlayer(UUID.randomUUID(), "Player2", 60.0, 0.0, 0.0)

        whenever(config.distanceRange).thenReturn(50)
        whenever(world.players).thenReturn(listOf(sender, player1, player2))
        whenever(userNameProvider.getPlayerName(sender)).thenReturn("Sender")
        whenever(userNameProvider.getDisplayName(sender)).thenReturn("Sender")

        val result = handler.handleChat(sender, "hello")

        // With range 50: sender (0) and player1 (30) are in range, player2 (60) is out
        assertEquals(2, result)
        verify(player2, never()).sendMessage(any<Component>())
    }

    @Test
    fun `handleChat with no other players returns 1 (sender only)`() {
        val sender = createMockPlayer(UUID.randomUUID(), "Sender", 0.0, 0.0, 0.0)

        whenever(world.players).thenReturn(listOf(sender))
        whenever(userNameProvider.getPlayerName(sender)).thenReturn("Sender")
        whenever(userNameProvider.getDisplayName(sender)).thenReturn("Sender")

        val result = handler.handleChat(sender, "hello")

        assertEquals(1, result)
        verify(sender).sendMessage(any<Component>())
    }

    @Test
    fun `handleChat formats message with player name and display name`() {
        val sender = createMockPlayer(UUID.randomUUID(), "Sender", 0.0, 0.0, 0.0)

        whenever(world.players).thenReturn(listOf(sender))
        whenever(userNameProvider.getPlayerName(sender)).thenReturn("SenderName")
        whenever(userNameProvider.getDisplayName(sender)).thenReturn("[Admin] SenderName")

        handler.handleChat(sender, "test message")

        verify(config).formatDistanceChat("SenderName", "[Admin] SenderName", "test message")
    }

    private fun createMockPlayer(
        uuid: UUID,
        name: String,
        x: Double,
        y: Double,
        z: Double,
    ): Player {
        val player = mock<Player>()
        val location = Location(world, x, y, z)

        whenever(player.uniqueId).thenReturn(uuid)
        whenever(player.name).thenReturn(name)
        whenever(player.world).thenReturn(world)
        whenever(player.location).thenReturn(location)

        return player
    }
}
