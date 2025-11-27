package com.bbobbogi.userchat.whisper

import com.bbobbogi.userchat.common.model.MessagingMode
import com.bbobbogi.userchat.config.UserChatConfig
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class WhisperManagerTest {
    private lateinit var config: UserChatConfig
    private lateinit var messenger: ChatMessenger
    private lateinit var userNameProvider: IUserNameProvider
    private lateinit var whisperManager: WhisperManager
    private lateinit var mockedBukkit: MockedStatic<Bukkit>

    private val senderUuid = UUID.randomUUID()
    private val targetUuid = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        config = mock()
        messenger = mock()
        userNameProvider = mock()

        whenever(config.getMessage(any(), any())).thenReturn(Component.text("test message"))
        whenever(config.formatWhisperSent(any(), any(), any())).thenReturn(Component.text("sent"))
        whenever(config.formatWhisperReceived(any(), any(), any())).thenReturn(Component.text("received"))
        whenever(messenger.getMode()).thenReturn(MessagingMode.OFF)

        whisperManager = WhisperManager(config, messenger, userNameProvider)

        mockedBukkit = mockStatic(Bukkit::class.java)
    }

    @AfterEach
    fun tearDown() {
        mockedBukkit.close()
    }

    @Test
    fun `sendWhisper to self returns SELF`() {
        val sender = createMockPlayer(senderUuid, "TestPlayer")
        whenever(userNameProvider.findPlayerByName("TestPlayer")).thenReturn(sender)

        val result = whisperManager.sendWhisper(sender, "TestPlayer", "hello")

        assertEquals(WhisperManager.WhisperResult.SELF, result)
        verify(config).getMessage(eq("whisper-self"))
    }

    @Test
    fun `sendWhisper to self is case insensitive`() {
        val sender = createMockPlayer(senderUuid, "TestPlayer")
        whenever(userNameProvider.findPlayerByName("testplayer")).thenReturn(sender)

        val result = whisperManager.sendWhisper(sender, "testplayer", "hello")

        assertEquals(WhisperManager.WhisperResult.SELF, result)
    }

    @Test
    fun `sendWhisper to local player returns SUCCESS`() {
        val sender = createMockPlayer(senderUuid, "Sender")
        val target = createMockPlayer(targetUuid, "Target")

        whenever(userNameProvider.findPlayerByName("Target")).thenReturn(target)
        whenever(userNameProvider.getPlayerName(sender)).thenReturn("Sender")
        whenever(userNameProvider.getDisplayName(sender)).thenReturn("Sender")
        whenever(userNameProvider.getPlayerName(target)).thenReturn("Target")
        whenever(userNameProvider.getDisplayName(target)).thenReturn("Target")

        val result = whisperManager.sendWhisper(sender, "Target", "hello")

        assertEquals(WhisperManager.WhisperResult.SUCCESS, result)
        verify(sender).sendMessage(any<Component>())
        verify(target).sendMessage(any<Component>())
    }

    @Test
    fun `sendWhisper to unknown player with messaging OFF returns NOT_FOUND`() {
        val sender = createMockPlayer(senderUuid, "Sender")

        whenever(userNameProvider.findPlayerByName("Unknown")).thenReturn(null)
        whenever(messenger.getMode()).thenReturn(MessagingMode.OFF)

        val result = whisperManager.sendWhisper(sender, "Unknown", "hello")

        assertEquals(WhisperManager.WhisperResult.NOT_FOUND, result)
        verify(config).getMessage(eq("player-not-found"), any())
    }

    @Test
    fun `sendWhisper to unknown player with messaging ON returns SENT_REMOTE`() {
        val sender = createMockPlayer(senderUuid, "Sender")

        whenever(userNameProvider.findPlayerByName("RemotePlayer")).thenReturn(null)
        whenever(messenger.getMode()).thenReturn(MessagingMode.REDIS)
        whenever(userNameProvider.getDisplayName(sender)).thenReturn("Sender")

        val result = whisperManager.sendWhisper(sender, "RemotePlayer", "hello")

        assertEquals(WhisperManager.WhisperResult.SENT_REMOTE, result)
        verify(messenger).sendWhisper(
            senderUuid = eq(senderUuid),
            senderName = eq("Sender"),
            targetName = eq("RemotePlayer"),
            message = eq("hello"),
        )
    }

    @Test
    fun `reply without previous whisper returns NO_TARGET`() {
        val sender = createMockPlayer(senderUuid, "Sender")

        val result = whisperManager.reply(sender, "hello")

        assertEquals(WhisperManager.WhisperResult.NO_TARGET, result)
        verify(config).getMessage(eq("no-reply-target"))
    }

    @Test
    fun `reply to local player returns SUCCESS`() {
        val sender = createMockPlayer(senderUuid, "Sender")
        val target = createMockPlayer(targetUuid, "Target")

        // First, establish a whisper conversation
        whenever(userNameProvider.findPlayerByName("Target")).thenReturn(target)
        whenever(userNameProvider.getPlayerName(sender)).thenReturn("Sender")
        whenever(userNameProvider.getDisplayName(sender)).thenReturn("Sender")
        whenever(userNameProvider.getPlayerName(target)).thenReturn("Target")
        whenever(userNameProvider.getDisplayName(target)).thenReturn("Target")

        whisperManager.sendWhisper(sender, "Target", "first message")

        // Mock Bukkit.getPlayer for reply
        mockedBukkit.`when`<Player> { Bukkit.getPlayer(targetUuid) }.thenReturn(target)

        val result = whisperManager.reply(sender, "reply message")

        assertEquals(WhisperManager.WhisperResult.SUCCESS, result)
    }

    @Test
    fun `reply to offline local player returns NOT_FOUND`() {
        val sender = createMockPlayer(senderUuid, "Sender")
        val target = createMockPlayer(targetUuid, "Target")

        // Establish whisper conversation
        whenever(userNameProvider.findPlayerByName("Target")).thenReturn(target)
        whenever(userNameProvider.getPlayerName(sender)).thenReturn("Sender")
        whenever(userNameProvider.getDisplayName(sender)).thenReturn("Sender")
        whenever(userNameProvider.getPlayerName(target)).thenReturn("Target")
        whenever(userNameProvider.getDisplayName(target)).thenReturn("Target")

        whisperManager.sendWhisper(sender, "Target", "first message")

        // Target is now offline
        mockedBukkit.`when`<Player> { Bukkit.getPlayer(targetUuid) }.thenReturn(null)

        val result = whisperManager.reply(sender, "reply message")

        assertEquals(WhisperManager.WhisperResult.NOT_FOUND, result)
    }

    @Test
    fun `clearPlayer removes whisper target`() {
        val sender = createMockPlayer(senderUuid, "Sender")
        val target = createMockPlayer(targetUuid, "Target")

        // Establish whisper conversation
        whenever(userNameProvider.findPlayerByName("Target")).thenReturn(target)
        whenever(userNameProvider.getPlayerName(sender)).thenReturn("Sender")
        whenever(userNameProvider.getDisplayName(sender)).thenReturn("Sender")
        whenever(userNameProvider.getPlayerName(target)).thenReturn("Target")
        whenever(userNameProvider.getDisplayName(target)).thenReturn("Target")

        whisperManager.sendWhisper(sender, "Target", "hello")

        // Clear sender's data
        whisperManager.clearPlayer(senderUuid)

        // Reply should fail
        val result = whisperManager.reply(sender, "reply")
        assertEquals(WhisperManager.WhisperResult.NO_TARGET, result)
    }

    @Test
    fun `handleRemoteWhisper stores reply target`() {
        val target = createMockPlayer(targetUuid, "Target")
        val remoteUuid = UUID.randomUUID()

        whenever(userNameProvider.findPlayerByName("Target")).thenReturn(target)
        whenever(userNameProvider.getDisplayName(target)).thenReturn("Target")

        whisperManager.handleRemoteWhisper(
            senderUuid = remoteUuid,
            senderName = "RemoteSender",
            senderServerId = "server-2",
            targetName = "Target",
            message = "hello from remote",
        )

        verify(target).sendMessage(any<Component>())
        verify(messenger).sendWhisperAck(eq(remoteUuid.toString()), eq(true))
    }

    @Test
    fun `handleWhisperNotFound notifies sender`() {
        val sender = createMockPlayer(senderUuid, "Sender")
        mockedBukkit.`when`<Player> { Bukkit.getPlayer(senderUuid) }.thenReturn(sender)

        whisperManager.handleWhisperNotFound(senderUuid, "UnknownPlayer")

        verify(sender).sendMessage(any<Component>())
        verify(config).getMessage(eq("player-not-found"), any())
    }

    @Test
    fun `handleWhisperNotFound does nothing if sender offline`() {
        mockedBukkit.`when`<Player> { Bukkit.getPlayer(senderUuid) }.thenReturn(null)

        // Should not throw
        whisperManager.handleWhisperNotFound(senderUuid, "UnknownPlayer")
    }

    private fun createMockPlayer(
        uuid: UUID,
        name: String,
    ): Player {
        val player = mock<Player>()
        whenever(player.uniqueId).thenReturn(uuid)
        whenever(player.name).thenReturn(name)
        return player
    }
}
