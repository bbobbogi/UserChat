package com.bbobbogi.userchat.messenger

import com.bbobbogi.userchat.common.model.MessagingMode
import com.bbobbogi.userchat.common.protocol.ChannelConstants
import com.bbobbogi.userchat.common.protocol.GlobalChatMessage
import com.bbobbogi.userchat.common.protocol.MessageType
import com.bbobbogi.userchat.common.protocol.WhisperMessage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.messaging.PluginMessageListener
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Velocity/BungeeCord PluginMessage 모드
 */
class PluginMessageMessenger(
    private val plugin: Plugin,
    private val serverId: String,
    private val serverDisplayName: String,
    private val logger: Logger
) : ChatMessenger, PluginMessageListener {

    private val json = Json { ignoreUnknownKeys = true }

    private var globalChatHandler: ((GlobalChatMessage) -> Unit)? = null
    private var whisperHandler: ((WhisperMessage) -> Unit)? = null
    private var whisperNotFoundHandler: ((UUID, String) -> Unit)? = null
    private var lastNoPlayerLogTime = 0L

    override fun getMode(): MessagingMode = MessagingMode.PLUGIN_MESSAGE
    override fun getServerId(): String = serverId
    override fun getServerDisplayName(): String = serverDisplayName

    override fun initialize() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, ChannelConstants.PLUGIN_MESSAGE_CHANNEL)
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, ChannelConstants.PLUGIN_MESSAGE_CHANNEL, this)
        logger.info("[UserChat] PluginMessage 메시징 초기화 완료")
    }

    override fun shutdown() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, ChannelConstants.PLUGIN_MESSAGE_CHANNEL)
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, ChannelConstants.PLUGIN_MESSAGE_CHANNEL, this)
    }

    override fun broadcastGlobalChat(playerUuid: UUID, playerName: String, message: String) {
        val chatMessage = GlobalChatMessage(
            serverId = serverId,
            serverDisplayName = serverDisplayName,
            playerUuid = playerUuid.toString(),
            playerName = playerName,
            message = message
        )

        val data = encodeMessage(MessageType.GLOBAL_CHAT.name, json.encodeToString(chatMessage))
        sendPluginMessage(data)
    }

    override fun setGlobalChatHandler(handler: (GlobalChatMessage) -> Unit) {
        globalChatHandler = handler
    }

    override fun sendWhisper(senderUuid: UUID, senderName: String, targetName: String, message: String) {
        val whisperMessage = WhisperMessage(
            senderUuid = senderUuid.toString(),
            senderName = senderName,
            senderServerId = serverId,
            targetName = targetName,
            message = message
        )

        val data = encodeMessage(MessageType.WHISPER.name, json.encodeToString(whisperMessage))
        sendPluginMessage(data)
    }

    override fun sendWhisperAck(senderUuid: String, success: Boolean) {
        // Velocity에서 처리
    }

    override fun setWhisperHandler(handler: (WhisperMessage) -> Unit) {
        whisperHandler = handler
    }

    override fun setWhisperNotFoundHandler(handler: (UUID, String) -> Unit) {
        whisperNotFoundHandler = handler
    }

    override fun onPluginMessageReceived(channel: String, player: Player, data: ByteArray) {
        if (channel != ChannelConstants.PLUGIN_MESSAGE_CHANNEL) return

        try {
            val (type, payload) = decodeMessage(data)

            when (type) {
                MessageType.GLOBAL_CHAT.name -> {
                    val message = json.decodeFromString<GlobalChatMessage>(payload)
                    globalChatHandler?.invoke(message)
                }
                MessageType.WHISPER.name -> {
                    val message = json.decodeFromString<WhisperMessage>(payload)
                    whisperHandler?.invoke(message)
                }
                MessageType.WHISPER_NOT_FOUND.name -> {
                    // 대상을 찾을 수 없음
                    val parts = payload.split(":")
                    if (parts.size == 2) {
                        whisperNotFoundHandler?.invoke(UUID.fromString(parts[0]), parts[1])
                    }
                }
            }
        } catch (e: Exception) {
            logger.warning("[UserChat] PluginMessage 처리 실패: ${e.message}")
        }
    }

    private fun encodeMessage(type: String, payload: String): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        dos.writeUTF(type)
        dos.writeUTF(payload)
        return baos.toByteArray()
    }

    private fun decodeMessage(data: ByteArray): Pair<String, String> {
        val bais = ByteArrayInputStream(data)
        val dis = DataInputStream(bais)
        val type = dis.readUTF()
        val payload = dis.readUTF()
        return type to payload
    }

    private fun sendPluginMessage(data: ByteArray) {
        // 온라인 플레이어가 있어야 전송 가능
        val player = Bukkit.getOnlinePlayers().firstOrNull()
        if (player == null) {
            val now = System.currentTimeMillis()
            if (now - lastNoPlayerLogTime > 30_000) {
                logger.log(Level.FINE, "[UserChat] PluginMessage 전송 실패: 온라인 플레이어가 없습니다.")
                lastNoPlayerLogTime = now
            }
            return
        }
        player.sendPluginMessage(plugin, ChannelConstants.PLUGIN_MESSAGE_CHANNEL, data)
    }
}
