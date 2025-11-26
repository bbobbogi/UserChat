package com.bbobbogi.userchat.velocity

import com.bbobbogi.userchat.common.protocol.ChannelConstants
import com.bbobbogi.userchat.common.protocol.MessageType
import com.bbobbogi.userchat.common.protocol.WhisperMessage
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import javax.inject.Inject

@Plugin(
    id = "userchat",
    name = "UserChat",
    version = "1.0.0-SNAPSHOT",
    description = "UserChat Velocity Module - Cross-server chat routing",
    authors = ["bbobbogi"]
)
class UserChatVelocity @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger
) {
    private val channel = MinecraftChannelIdentifier.from(ChannelConstants.PLUGIN_MESSAGE_CHANNEL)
    private val json = Json { ignoreUnknownKeys = true }

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        server.channelRegistrar.register(channel)
        logger.info("[UserChat] Velocity 모듈이 초기화되었습니다.")
    }

    @Subscribe
    fun onPluginMessage(event: PluginMessageEvent) {
        if (event.identifier != channel) return

        // 메시지 처리 (서버 → 프록시 → 서버)
        event.result = PluginMessageEvent.ForwardResult.handled()

        try {
            val (type, payload) = decodeMessage(event.data)

            when (type) {
                MessageType.GLOBAL_CHAT.name -> handleGlobalChat(payload, event.data)
                MessageType.WHISPER.name -> handleWhisper(payload, event.data)
            }
        } catch (e: Exception) {
            logger.warn("[UserChat] 메시지 처리 실패: ${e.message}")
        }
    }

    private fun handleGlobalChat(payload: String, originalData: ByteArray) {
        // 모든 서버에 브로드캐스트
        server.allServers.forEach { serverConnection ->
            serverConnection.sendPluginMessage(channel, originalData)
        }
    }

    private fun handleWhisper(payload: String, originalData: ByteArray) {
        val message = json.decodeFromString<WhisperMessage>(payload)
        val targetName = message.targetName

        // 대상 플레이어 찾기
        val targetPlayerOptional = server.getPlayer(targetName)

        if (targetPlayerOptional.isPresent) {
            val targetPlayer = targetPlayerOptional.get()
            // 대상 플레이어가 있는 서버에만 전송
            val currentServerOptional = targetPlayer.currentServer
            if (currentServerOptional.isPresent) {
                currentServerOptional.get().sendPluginMessage(channel, originalData)
            }
        } else {
            // 플레이어를 찾을 수 없음 - 발신자에게 알림
            val notFoundData = encodeMessage(
                MessageType.WHISPER_NOT_FOUND.name,
                "${message.senderUuid}:$targetName"
            )

            // 발신 서버로만 전송
            server.getServer(message.senderServerId).ifPresent { senderServer ->
                senderServer.sendPluginMessage(channel, notFoundData)
            }
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
}
