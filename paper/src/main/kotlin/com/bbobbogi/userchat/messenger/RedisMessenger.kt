package com.bbobbogi.userchat.messenger

import com.bbobbogi.userchat.common.model.MessagingMode
import com.bbobbogi.userchat.common.protocol.ChannelConstants
import com.bbobbogi.userchat.common.protocol.GlobalChatMessage
import com.bbobbogi.userchat.common.protocol.MessageType
import com.bbobbogi.userchat.common.protocol.NoticeMessage
import com.bbobbogi.userchat.common.protocol.WhisperMessage
import io.papermc.chzzkmultipleuser.messaging.MessagingProvider
import io.papermc.chzzkmultipleuser.messaging.api.IStreamBroker
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.logging.Logger

/**
 * Redis Stream 모드 (ChzzkMultipleUser 연동)
 */
class RedisMessenger(
    private val plugin: Plugin,
    private val logger: Logger,
) : ChatMessenger {
    private var streamBroker: IStreamBroker? = null

    private var serverId: String = "unknown"
    private var serverDisplayName: String = "Server"

    private var globalChatHandler: ((GlobalChatMessage) -> Unit)? = null
    private var noticeHandler: ((NoticeMessage) -> Unit)? = null
    private var whisperHandler: ((WhisperMessage) -> Unit)? = null
    private var whisperNotFoundHandler: ((UUID, String) -> Unit)? = null

    override fun getMode(): MessagingMode = MessagingMode.REDIS

    override fun getServerId(): String = serverId

    override fun getServerDisplayName(): String = serverDisplayName

    override fun initialize() {
        try {
            if (!MessagingProvider.isInitialized()) {
                logger.warning("[UserChat] ChzzkMultipleUser MessagingProvider가 초기화되지 않았습니다.")
                return
            }

            serverId = MessagingProvider.getServerId() ?: "unknown"
            serverDisplayName = MessagingProvider.getServerDisplayName() ?: "Server"
            streamBroker = MessagingProvider.getStreamBroker()

            if (streamBroker == null) {
                logger.warning("[UserChat] Redis Stream Broker를 찾을 수 없습니다.")
                return
            }

            // Consumer 시작
            startConsumers()

            logger.info("[UserChat] Redis 메시징 초기화 완료 (서버: $serverId)")
        } catch (e: NoClassDefFoundError) {
            logger.warning("[UserChat] ChzzkMultipleUser를 찾을 수 없습니다. Redis 모드를 사용할 수 없습니다.")
        } catch (e: Exception) {
            logger.warning("[UserChat] Redis 초기화 실패: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun startConsumers() {
        val broker = streamBroker ?: return

        // 전체 채팅 스트림 구독
        broker.consumeStream(
            ChannelConstants.REDIS_GLOBAL_CHAT_STREAM,
            serverId,
        ) { msg ->
            try {
                val data = msg.data

                // 자기 서버 메시지는 무시
                val messageServerId = data["serverId"] ?: return@consumeStream
                if (messageServerId == serverId) return@consumeStream

                val message =
                    GlobalChatMessage(
                        serverId = messageServerId,
                        serverDisplayName = data["serverDisplayName"] ?: "Server",
                        playerUuid = data["playerUuid"] ?: return@consumeStream,
                        playerName = data["playerName"] ?: "Unknown",
                        message = data["message"] ?: return@consumeStream,
                        timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis(),
                    )

                Bukkit.getScheduler().runTask(
                    plugin,
                    Runnable {
                        globalChatHandler?.invoke(message)
                    },
                )
            } catch (e: Exception) {
                logger.warning("[UserChat] 전체 채팅 메시지 처리 실패: ${e.message}")
            }
        }

        // 공지 스트림 구독
        broker.consumeStream(
            ChannelConstants.REDIS_NOTICE_STREAM,
            serverId,
        ) { msg ->
            try {
                val data = msg.data

                // 자기 서버 메시지는 무시
                val messageServerId = data["serverId"] ?: return@consumeStream
                if (messageServerId == serverId) return@consumeStream

                val message =
                    NoticeMessage(
                        serverId = messageServerId,
                        senderName = data["senderName"] ?: "Unknown",
                        message = data["message"] ?: return@consumeStream,
                        timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis(),
                    )

                Bukkit.getScheduler().runTask(
                    plugin,
                    Runnable {
                        noticeHandler?.invoke(message)
                    },
                )
            } catch (e: Exception) {
                logger.warning("[UserChat] 공지 메시지 처리 실패: ${e.message}")
            }
        }

        // 귓속말 스트림 구독
        broker.consumeStream(
            ChannelConstants.REDIS_WHISPER_STREAM,
            serverId,
        ) { msg ->
            try {
                val data = msg.data
                val type = data["type"] ?: return@consumeStream

                when (type) {
                    MessageType.WHISPER.name -> {
                        val message =
                            WhisperMessage(
                                senderUuid = data["senderUuid"] ?: return@consumeStream,
                                senderName = data["senderName"] ?: "Unknown",
                                senderServerId = data["senderServerId"] ?: return@consumeStream,
                                targetName = data["targetName"] ?: return@consumeStream,
                                message = data["message"] ?: return@consumeStream,
                                timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis(),
                            )

                        Bukkit.getScheduler().runTask(
                            plugin,
                            Runnable {
                                whisperHandler?.invoke(message)
                            },
                        )
                    }
                    MessageType.WHISPER_NOT_FOUND.name -> {
                        val senderUuid = data["senderUuid"] ?: return@consumeStream
                        val targetName = data["targetName"] ?: return@consumeStream

                        Bukkit.getScheduler().runTask(
                            plugin,
                            Runnable {
                                whisperNotFoundHandler?.invoke(UUID.fromString(senderUuid), targetName)
                            },
                        )
                    }
                }
            } catch (e: Exception) {
                logger.warning("[UserChat] 귓속말 메시지 처리 실패: ${e.message}")
            }
        }
    }

    override fun shutdown() {
        streamBroker?.close()
    }

    override fun broadcastGlobalChat(
        playerUuid: UUID,
        playerName: String,
        message: String,
    ) {
        val broker = streamBroker ?: return

        try {
            val data =
                mapOf(
                    "type" to MessageType.GLOBAL_CHAT.name,
                    "serverId" to serverId,
                    "serverDisplayName" to serverDisplayName,
                    "playerUuid" to playerUuid.toString(),
                    "playerName" to playerName,
                    "message" to message,
                    "timestamp" to System.currentTimeMillis().toString(),
                )

            broker.publishStream(ChannelConstants.REDIS_GLOBAL_CHAT_STREAM, data)
        } catch (e: Exception) {
            logger.warning("[UserChat] 전체 채팅 전송 실패: ${e.message}")
        }
    }

    override fun setGlobalChatHandler(handler: (GlobalChatMessage) -> Unit) {
        globalChatHandler = handler
    }

    override fun broadcastNotice(
        senderName: String,
        message: String,
    ) {
        val broker = streamBroker ?: return

        try {
            val data =
                mapOf(
                    "serverId" to serverId,
                    "senderName" to senderName,
                    "message" to message,
                    "timestamp" to System.currentTimeMillis().toString(),
                )

            broker.publishStream(ChannelConstants.REDIS_NOTICE_STREAM, data)
        } catch (e: Exception) {
            logger.warning("[UserChat] 공지 전송 실패: ${e.message}")
        }
    }

    override fun setNoticeHandler(handler: (NoticeMessage) -> Unit) {
        noticeHandler = handler
    }

    override fun sendWhisper(
        senderUuid: UUID,
        senderName: String,
        targetName: String,
        message: String,
    ) {
        val broker = streamBroker ?: return

        try {
            val data =
                mapOf(
                    "type" to MessageType.WHISPER.name,
                    "senderUuid" to senderUuid.toString(),
                    "senderName" to senderName,
                    "senderServerId" to serverId,
                    "targetName" to targetName,
                    "message" to message,
                    "timestamp" to System.currentTimeMillis().toString(),
                )

            broker.publishStream(ChannelConstants.REDIS_WHISPER_STREAM, data)
        } catch (e: Exception) {
            logger.warning("[UserChat] 귓속말 전송 실패: ${e.message}")
        }
    }

    override fun sendWhisperAck(
        senderUuid: String,
        success: Boolean,
    ) {
        // Redis에서는 별도 ACK 불필요
    }

    override fun setWhisperHandler(handler: (WhisperMessage) -> Unit) {
        whisperHandler = handler
    }

    override fun setWhisperNotFoundHandler(handler: (UUID, String) -> Unit) {
        whisperNotFoundHandler = handler
    }
}
