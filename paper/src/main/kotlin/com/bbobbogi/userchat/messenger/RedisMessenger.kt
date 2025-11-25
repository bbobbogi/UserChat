package com.bbobbogi.userchat.messenger

import com.bbobbogi.userchat.common.model.MessagingMode
import com.bbobbogi.userchat.common.protocol.ChannelConstants
import com.bbobbogi.userchat.common.protocol.GlobalChatMessage
import com.bbobbogi.userchat.common.protocol.MessageType
import com.bbobbogi.userchat.common.protocol.WhisperMessage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.logging.Logger

/**
 * Redis Stream 모드 (ChzzkMultipleUser 연동)
 */
class RedisMessenger(
    private val plugin: Plugin,
    private val logger: Logger
) : ChatMessenger {

    private val json = Json { ignoreUnknownKeys = true }

    private var streamBroker: Any? = null
    private var publishMethod: java.lang.reflect.Method? = null
    private var consumeMethod: java.lang.reflect.Method? = null

    private var serverId: String = "unknown"
    private var serverDisplayName: String = "Server"

    private var globalChatHandler: ((GlobalChatMessage) -> Unit)? = null
    private var whisperHandler: ((WhisperMessage) -> Unit)? = null
    private var whisperNotFoundHandler: ((UUID, String) -> Unit)? = null

    override fun getMode(): MessagingMode = MessagingMode.REDIS
    override fun getServerId(): String = serverId
    override fun getServerDisplayName(): String = serverDisplayName

    override fun initialize() {
        try {
            // MessagingProvider에서 정보 가져오기
            val providerClass = Class.forName("io.papermc.chzzkmultipleuser.messaging.MessagingProvider")

            val getServerIdMethod = providerClass.getMethod("getServerId")
            val getDisplayNameMethod = providerClass.getMethod("getServerDisplayName")
            val getBrokerMethod = providerClass.getMethod("getStreamBroker")

            serverId = (getServerIdMethod.invoke(null) as? String) ?: "unknown"
            serverDisplayName = (getDisplayNameMethod.invoke(null) as? String) ?: "Server"
            streamBroker = getBrokerMethod.invoke(null)

            if (streamBroker == null) {
                logger.warning("[UserChat] Redis Stream Broker를 찾을 수 없습니다.")
                return
            }

            // IStreamBroker 메서드 가져오기
            val brokerClass = streamBroker!!::class.java
            publishMethod = brokerClass.getMethod("publishStream", String::class.java, Map::class.java)

            // Consumer 시작
            startConsumers()

            logger.info("[UserChat] Redis 메시징 초기화 완료 (서버: $serverId)")
        } catch (e: ClassNotFoundException) {
            logger.warning("[UserChat] ChzzkMultipleUser를 찾을 수 없습니다. Redis 모드를 사용할 수 없습니다.")
        } catch (e: Exception) {
            logger.warning("[UserChat] Redis 초기화 실패: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun startConsumers() {
        // 전체 채팅 스트림 구독
        subscribeToStream(ChannelConstants.REDIS_GLOBAL_CHAT_STREAM) { data ->
            try {
                val type = data["type"] ?: return@subscribeToStream
                if (type != MessageType.GLOBAL_CHAT.name) return@subscribeToStream

                val message = GlobalChatMessage(
                    serverId = data["serverId"] ?: return@subscribeToStream,
                    serverDisplayName = data["serverDisplayName"] ?: "Server",
                    playerUuid = data["playerUuid"] ?: return@subscribeToStream,
                    playerName = data["playerName"] ?: "Unknown",
                    message = data["message"] ?: return@subscribeToStream,
                    timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
                )

                // 자기 서버 메시지는 무시
                if (message.serverId != serverId) {
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        globalChatHandler?.invoke(message)
                    })
                }
            } catch (e: Exception) {
                logger.warning("[UserChat] 전체 채팅 메시지 처리 실패: ${e.message}")
            }
        }

        // 귓속말 스트림 구독
        subscribeToStream(ChannelConstants.REDIS_WHISPER_STREAM) { data ->
            try {
                val type = data["type"] ?: return@subscribeToStream

                when (type) {
                    MessageType.WHISPER.name -> {
                        val message = WhisperMessage(
                            senderUuid = data["senderUuid"] ?: return@subscribeToStream,
                            senderName = data["senderName"] ?: "Unknown",
                            senderServerId = data["senderServerId"] ?: return@subscribeToStream,
                            targetName = data["targetName"] ?: return@subscribeToStream,
                            message = data["message"] ?: return@subscribeToStream,
                            timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
                        )

                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            whisperHandler?.invoke(message)
                        })
                    }
                    MessageType.WHISPER_NOT_FOUND.name -> {
                        val senderUuid = data["senderUuid"] ?: return@subscribeToStream
                        val targetName = data["targetName"] ?: return@subscribeToStream

                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            whisperNotFoundHandler?.invoke(UUID.fromString(senderUuid), targetName)
                        })
                    }
                }
            } catch (e: Exception) {
                logger.warning("[UserChat] 귓속말 메시지 처리 실패: ${e.message}")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun subscribeToStream(streamKey: String, handler: (Map<String, String>) -> Unit) {
        try {
            val brokerClass = streamBroker!!::class.java

            // consumeStream 메서드 찾기
            val consumeMethod = brokerClass.methods.find {
                it.name == "consumeStream" && it.parameterCount == 4
            } ?: return

            consumeMethod.invoke(
                streamBroker,
                streamKey,
                ChannelConstants.REDIS_CONSUMER_GROUP,
                serverId,
                { msg: Any ->
                    try {
                        // StreamMessage에서 data 가져오기
                        val dataMethod = msg::class.java.getMethod("getData")
                        val data = dataMethod.invoke(msg) as? Map<String, String>
                        if (data != null) {
                            handler(data)
                        }
                    } catch (e: Exception) {
                        logger.warning("[UserChat] Stream 메시지 파싱 실패: ${e.message}")
                    }
                }
            )
        } catch (e: Exception) {
            logger.warning("[UserChat] Stream 구독 실패 ($streamKey): ${e.message}")
        }
    }

    override fun shutdown() {
        // Stream consumer 정리는 ChzzkMultipleUser에서 처리
    }

    override fun broadcastGlobalChat(playerUuid: UUID, playerName: String, message: String) {
        if (streamBroker == null || publishMethod == null) return

        try {
            val data = mapOf(
                "type" to MessageType.GLOBAL_CHAT.name,
                "serverId" to serverId,
                "serverDisplayName" to serverDisplayName,
                "playerUuid" to playerUuid.toString(),
                "playerName" to playerName,
                "message" to message,
                "timestamp" to System.currentTimeMillis().toString()
            )

            publishMethod?.invoke(streamBroker, ChannelConstants.REDIS_GLOBAL_CHAT_STREAM, data)
        } catch (e: Exception) {
            logger.warning("[UserChat] 전체 채팅 전송 실패: ${e.message}")
        }
    }

    override fun setGlobalChatHandler(handler: (GlobalChatMessage) -> Unit) {
        globalChatHandler = handler
    }

    override fun sendWhisper(senderUuid: UUID, senderName: String, targetName: String, message: String) {
        if (streamBroker == null || publishMethod == null) return

        try {
            val data = mapOf(
                "type" to MessageType.WHISPER.name,
                "senderUuid" to senderUuid.toString(),
                "senderName" to senderName,
                "senderServerId" to serverId,
                "targetName" to targetName,
                "message" to message,
                "timestamp" to System.currentTimeMillis().toString()
            )

            publishMethod?.invoke(streamBroker, ChannelConstants.REDIS_WHISPER_STREAM, data)
        } catch (e: Exception) {
            logger.warning("[UserChat] 귓속말 전송 실패: ${e.message}")
        }
    }

    override fun sendWhisperAck(senderUuid: String, success: Boolean) {
        // Redis에서는 별도 ACK 불필요
    }

    override fun setWhisperHandler(handler: (WhisperMessage) -> Unit) {
        whisperHandler = handler
    }

    override fun setWhisperNotFoundHandler(handler: (UUID, String) -> Unit) {
        whisperNotFoundHandler = handler
    }
}
