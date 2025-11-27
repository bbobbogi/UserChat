package com.bbobbogi.userchat.chat

import com.bbobbogi.userchat.common.model.ChatMode
import com.bbobbogi.userchat.config.UserChatConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class ChatModeManagerTest {
    private lateinit var config: UserChatConfig
    private lateinit var manager: ChatModeManager

    @BeforeEach
    fun setUp() {
        config = mock()
        whenever(config.defaultChatMode).thenReturn(ChatMode.DISTANCE)
        manager = ChatModeManager(config)
    }

    @Test
    fun `getMode returns default mode for unknown player`() {
        val playerId = UUID.randomUUID()

        val result = manager.getMode(playerId)

        assertEquals(ChatMode.DISTANCE, result)
    }

    @Test
    fun `getMode returns GLOBAL when default is GLOBAL`() {
        whenever(config.defaultChatMode).thenReturn(ChatMode.GLOBAL)
        val newManager = ChatModeManager(config)
        val playerId = UUID.randomUUID()

        val result = newManager.getMode(playerId)

        assertEquals(ChatMode.GLOBAL, result)
    }

    @Test
    fun `setMode changes player mode`() {
        val playerId = UUID.randomUUID()

        manager.setMode(playerId, ChatMode.GLOBAL)

        assertEquals(ChatMode.GLOBAL, manager.getMode(playerId))
    }

    @Test
    fun `setMode can change mode multiple times`() {
        val playerId = UUID.randomUUID()

        manager.setMode(playerId, ChatMode.GLOBAL)
        assertEquals(ChatMode.GLOBAL, manager.getMode(playerId))

        manager.setMode(playerId, ChatMode.DISTANCE)
        assertEquals(ChatMode.DISTANCE, manager.getMode(playerId))
    }

    @Test
    fun `toggleMode switches from DISTANCE to GLOBAL`() {
        val playerId = UUID.randomUUID()
        manager.setMode(playerId, ChatMode.DISTANCE)

        val result = manager.toggleMode(playerId)

        assertEquals(ChatMode.GLOBAL, result)
        assertEquals(ChatMode.GLOBAL, manager.getMode(playerId))
    }

    @Test
    fun `toggleMode switches from GLOBAL to DISTANCE`() {
        val playerId = UUID.randomUUID()
        manager.setMode(playerId, ChatMode.GLOBAL)

        val result = manager.toggleMode(playerId)

        assertEquals(ChatMode.DISTANCE, result)
        assertEquals(ChatMode.DISTANCE, manager.getMode(playerId))
    }

    @Test
    fun `toggleMode toggles default mode when player has no set mode`() {
        val playerId = UUID.randomUUID()

        val result = manager.toggleMode(playerId)

        assertEquals(ChatMode.GLOBAL, result)
    }

    @Test
    fun `resetMode removes player mode and returns to default`() {
        val playerId = UUID.randomUUID()
        manager.setMode(playerId, ChatMode.GLOBAL)

        manager.resetMode(playerId)

        assertEquals(ChatMode.DISTANCE, manager.getMode(playerId))
    }

    @Test
    fun `clearAll removes all player modes`() {
        val player1 = UUID.randomUUID()
        val player2 = UUID.randomUUID()
        manager.setMode(player1, ChatMode.GLOBAL)
        manager.setMode(player2, ChatMode.GLOBAL)

        manager.clearAll()

        assertEquals(ChatMode.DISTANCE, manager.getMode(player1))
        assertEquals(ChatMode.DISTANCE, manager.getMode(player2))
    }

    @Test
    fun `different players have independent modes`() {
        val player1 = UUID.randomUUID()
        val player2 = UUID.randomUUID()

        manager.setMode(player1, ChatMode.GLOBAL)
        manager.setMode(player2, ChatMode.DISTANCE)

        assertEquals(ChatMode.GLOBAL, manager.getMode(player1))
        assertEquals(ChatMode.DISTANCE, manager.getMode(player2))
    }
}
