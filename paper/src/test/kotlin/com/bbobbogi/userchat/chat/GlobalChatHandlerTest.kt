package com.bbobbogi.userchat.chat

import com.bbobbogi.userchat.common.model.ChatMode
import com.bbobbogi.userchat.config.UserChatConfig
import com.bbobbogi.userchat.item.GlobalChatItemManager
import com.bbobbogi.userchat.messenger.ChatMessenger
import com.bbobbogi.userchat.service.IUserNameProvider
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class GlobalChatHandlerTest {
    private lateinit var config: UserChatConfig
    private lateinit var itemManager: GlobalChatItemManager
    private lateinit var messenger: ChatMessenger
    private lateinit var modeManager: ChatModeManager
    private lateinit var userNameProvider: IUserNameProvider
    private lateinit var handler: GlobalChatHandler
    private lateinit var mockedBukkit: MockedStatic<Bukkit>

    private val playerUuid = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        config = mock()
        itemManager = mock()
        messenger = mock()
        modeManager = mock()
        userNameProvider = mock()

        whenever(config.globalRequireItem).thenReturn(true)
        whenever(config.switchToDistanceOnNoItem).thenReturn(true)
        whenever(config.getMessage(any(), any())).thenReturn(Component.text("test"))
        whenever(config.formatGlobalChat(any(), any(), any(), any())).thenReturn(Component.text("formatted"))
        whenever(config.formatNotice(any(), any())).thenReturn(Component.text("notice"))
        whenever(messenger.getServerId()).thenReturn("server-1")
        whenever(messenger.getServerDisplayName()).thenReturn("Server 1")

        handler = GlobalChatHandler(config, itemManager, messenger, modeManager, userNameProvider)

        mockedBukkit = mockStatic(Bukkit::class.java)
        mockedBukkit.`when`<Collection<Player>> { Bukkit.getOnlinePlayers() }.thenReturn(emptyList())
    }

    @AfterEach
    fun tearDown() {
        mockedBukkit.close()
    }

    @Test
    fun `handleChat with bypass permission returns SUCCESS without item consumption`() {
        val player = createMockPlayer(playerUuid, "TestPlayer", hasBypass = true)

        whenever(userNameProvider.getPlayerName(player)).thenReturn("TestPlayer")
        whenever(userNameProvider.getDisplayName(player)).thenReturn("TestPlayer")

        val result = handler.handleChat(player, "hello")

        assertEquals(GlobalChatHandler.GlobalChatResult.SUCCESS, result)
        verify(itemManager, never()).consumeItem(any())
        verify(messenger).broadcastGlobalChat(eq(playerUuid), any(), eq("hello"))
    }

    @Test
    fun `handleChat without bypass consumes item and returns SUCCESS`() {
        val player = createMockPlayer(playerUuid, "TestPlayer", hasBypass = false)

        whenever(itemManager.countItems(player)).thenReturn(5)
        whenever(userNameProvider.getPlayerName(player)).thenReturn("TestPlayer")
        whenever(userNameProvider.getDisplayName(player)).thenReturn("TestPlayer")

        val result = handler.handleChat(player, "hello")

        assertEquals(GlobalChatHandler.GlobalChatResult.SUCCESS, result)
        verify(itemManager).consumeItem(player)
        verify(config).getMessage(eq("item-consumed"), any())
    }

    @Test
    fun `handleChat with no items and switchToDistance enabled returns SWITCHED_TO_DISTANCE`() {
        val player = createMockPlayer(playerUuid, "TestPlayer", hasBypass = false)

        whenever(itemManager.countItems(player)).thenReturn(0)
        whenever(config.switchToDistanceOnNoItem).thenReturn(true)

        val result = handler.handleChat(player, "hello")

        assertEquals(GlobalChatHandler.GlobalChatResult.SWITCHED_TO_DISTANCE, result)
        verify(modeManager).setMode(playerUuid, ChatMode.DISTANCE)
        verify(config).getMessage(eq("no-item-switched"))
        verify(messenger, never()).broadcastGlobalChat(any(), any(), any())
    }

    @Test
    fun `handleChat with no items and switchToDistance disabled returns NO_ITEM`() {
        val player = createMockPlayer(playerUuid, "TestPlayer", hasBypass = false)

        whenever(itemManager.countItems(player)).thenReturn(0)
        whenever(config.switchToDistanceOnNoItem).thenReturn(false)

        val result = handler.handleChat(player, "hello")

        assertEquals(GlobalChatHandler.GlobalChatResult.NO_ITEM, result)
        verify(config).getMessage(eq("no-item"))
        verify(modeManager, never()).setMode(any(), any())
        verify(messenger, never()).broadcastGlobalChat(any(), any(), any())
    }

    @Test
    fun `handleChat without item requirement returns SUCCESS`() {
        val player = createMockPlayer(playerUuid, "TestPlayer", hasBypass = false)

        whenever(config.globalRequireItem).thenReturn(false)
        whenever(userNameProvider.getPlayerName(player)).thenReturn("TestPlayer")
        whenever(userNameProvider.getDisplayName(player)).thenReturn("TestPlayer")

        val result = handler.handleChat(player, "hello")

        assertEquals(GlobalChatHandler.GlobalChatResult.SUCCESS, result)
        verify(itemManager, never()).countItems(any())
        verify(itemManager, never()).consumeItem(any())
    }

    @Test
    fun `handleRemoteMessage ignores message from same server`() {
        val onlinePlayer = createMockPlayer(UUID.randomUUID(), "Online")
        mockedBukkit.`when`<Collection<Player>> { Bukkit.getOnlinePlayers() }.thenReturn(listOf(onlinePlayer))

        handler.handleRemoteMessage(
            serverId = "server-1",
            serverDisplayName = "Server 1",
            playerName = "RemotePlayer",
            message = "hello",
        )

        // Should not broadcast since it's from the same server
        verify(onlinePlayer, never()).sendMessage(any<Component>())
    }

    @Test
    fun `handleRemoteMessage broadcasts message from different server`() {
        val onlinePlayer = createMockPlayer(UUID.randomUUID(), "Online")
        mockedBukkit.`when`<Collection<Player>> { Bukkit.getOnlinePlayers() }.thenReturn(listOf(onlinePlayer))

        handler.handleRemoteMessage(
            serverId = "server-2",
            serverDisplayName = "Server 2",
            playerName = "RemotePlayer",
            message = "hello",
        )

        verify(onlinePlayer).sendMessage(any<Component>())
    }

    @Test
    fun `broadcastNotice sends to all online players and remote`() {
        val player1 = createMockPlayer(UUID.randomUUID(), "Player1")
        val player2 = createMockPlayer(UUID.randomUUID(), "Player2")
        mockedBukkit.`when`<Collection<Player>> { Bukkit.getOnlinePlayers() }.thenReturn(listOf(player1, player2))

        handler.broadcastNotice("Admin", "Test notice")

        verify(player1).sendMessage(any<Component>())
        verify(player2).sendMessage(any<Component>())
        verify(messenger).broadcastNotice("Admin", "Test notice")
    }

    @Test
    fun `handleRemoteNotice ignores notice from same server`() {
        val onlinePlayer = createMockPlayer(UUID.randomUUID(), "Online")
        mockedBukkit.`when`<Collection<Player>> { Bukkit.getOnlinePlayers() }.thenReturn(listOf(onlinePlayer))

        handler.handleRemoteNotice(
            serverId = "server-1",
            senderName = "Admin",
            message = "notice",
        )

        verify(onlinePlayer, never()).sendMessage(any<Component>())
    }

    @Test
    fun `handleRemoteNotice broadcasts notice from different server`() {
        val onlinePlayer = createMockPlayer(UUID.randomUUID(), "Online")
        mockedBukkit.`when`<Collection<Player>> { Bukkit.getOnlinePlayers() }.thenReturn(listOf(onlinePlayer))

        handler.handleRemoteNotice(
            serverId = "server-2",
            senderName = "Admin",
            message = "notice",
        )

        verify(onlinePlayer).sendMessage(any<Component>())
    }

    private fun createMockPlayer(
        uuid: UUID,
        name: String,
        hasBypass: Boolean = false,
    ): Player {
        val player = mock<Player>()
        whenever(player.uniqueId).thenReturn(uuid)
        whenever(player.name).thenReturn(name)
        whenever(player.hasPermission("userchat.bypass")).thenReturn(hasBypass)
        return player
    }
}
