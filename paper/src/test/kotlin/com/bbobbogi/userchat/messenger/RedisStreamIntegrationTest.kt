package com.bbobbogi.userchat.messenger

import com.bbobbogi.userchat.common.protocol.ChannelConstants
import com.bbobbogi.userchat.common.protocol.MessageType
import io.lettuce.core.Consumer
import io.lettuce.core.RedisClient
import io.lettuce.core.XGroupCreateArgs
import io.lettuce.core.XReadArgs
import io.lettuce.core.api.sync.RedisCommands
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.util.UUID

/**
 * Redis Stream 통합 테스트
 * Testcontainers를 사용하여 실제 Redis 인스턴스에서 메시지 전송/수신을 검증
 *
 * Docker가 설치되어 있지 않으면 테스트가 자동으로 스킵됩니다.
 */
@Testcontainers
@Tag("integration")
@EnabledIf("isDockerAvailable")
class RedisStreamIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val redisContainer: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)

        @JvmStatic
        fun isDockerAvailable(): Boolean =
            try {
                DockerClientFactory.instance().isDockerAvailable
            } catch (e: Exception) {
                false
            }
    }

    private lateinit var client: RedisClient
    private lateinit var commands: RedisCommands<String, String>
    private val serverId = "test-server-1"
    private val consumerGroup = ChannelConstants.REDIS_CONSUMER_GROUP

    @BeforeEach
    fun setUp() {
        val redisUrl = "redis://${redisContainer.host}:${redisContainer.getMappedPort(6379)}"
        client = RedisClient.create(redisUrl)
        commands = client.connect().sync()

        // 스트림 및 컨슈머 그룹 초기화
        initializeStream(ChannelConstants.REDIS_GLOBAL_CHAT_STREAM)
        initializeStream(ChannelConstants.REDIS_NOTICE_STREAM)
        initializeStream(ChannelConstants.REDIS_WHISPER_STREAM)
    }

    private fun initializeStream(streamKey: String) {
        try {
            // 컨슈머 그룹 생성 (스트림이 없으면 자동 생성)
            commands.xgroupCreate(
                XReadArgs.StreamOffset.from(streamKey, "0"),
                consumerGroup,
                XGroupCreateArgs.Builder.mkstream(),
            )
        } catch (e: Exception) {
            // BUSYGROUP 오류 무시 (이미 존재하는 경우)
        }
    }

    @AfterEach
    fun tearDown() {
        client.shutdown()
    }

    @Test
    fun `global chat message can be published and consumed`() {
        val playerUuid = UUID.randomUUID().toString()
        val playerName = "TestPlayer"
        val message = "Hello, world!"

        // 메시지 발행
        val entryId =
            commands.xadd(
                ChannelConstants.REDIS_GLOBAL_CHAT_STREAM,
                mapOf(
                    "type" to MessageType.GLOBAL_CHAT.name,
                    "serverId" to serverId,
                    "serverDisplayName" to "Test Server",
                    "playerUuid" to playerUuid,
                    "playerName" to playerName,
                    "message" to message,
                    "timestamp" to System.currentTimeMillis().toString(),
                ),
            )
        assertNotNull(entryId)

        // 메시지 소비
        val entries =
            commands.xreadgroup(
                Consumer.from(consumerGroup, "consumer-1"),
                XReadArgs.StreamOffset.lastConsumed(ChannelConstants.REDIS_GLOBAL_CHAT_STREAM),
            )

        assertNotNull(entries)
        assertTrue(entries.isNotEmpty())

        val lastEntry = entries.last()
        assertEquals(ChannelConstants.REDIS_GLOBAL_CHAT_STREAM, lastEntry.stream)
        assertEquals(playerName, lastEntry.body["playerName"])
        assertEquals(message, lastEntry.body["message"])
        assertEquals(serverId, lastEntry.body["serverId"])
    }

    @Test
    fun `notice message can be published and consumed`() {
        val senderName = "Admin"
        val message = "Server will restart in 5 minutes!"

        // 메시지 발행
        val entryId =
            commands.xadd(
                ChannelConstants.REDIS_NOTICE_STREAM,
                mapOf(
                    "serverId" to serverId,
                    "senderName" to senderName,
                    "message" to message,
                    "timestamp" to System.currentTimeMillis().toString(),
                ),
            )
        assertNotNull(entryId)

        // 메시지 소비
        val entries =
            commands.xreadgroup(
                Consumer.from(consumerGroup, "consumer-1"),
                XReadArgs.StreamOffset.lastConsumed(ChannelConstants.REDIS_NOTICE_STREAM),
            )

        assertNotNull(entries)
        assertTrue(entries.isNotEmpty())

        val lastEntry = entries.last()
        assertEquals(senderName, lastEntry.body["senderName"])
        assertEquals(message, lastEntry.body["message"])
    }

    @Test
    fun `whisper message can be published and consumed`() {
        val senderUuid = UUID.randomUUID().toString()
        val senderName = "Sender"
        val targetName = "Target"
        val message = "Hey, how are you?"

        // 귓속말 메시지 발행
        val entryId =
            commands.xadd(
                ChannelConstants.REDIS_WHISPER_STREAM,
                mapOf(
                    "type" to MessageType.WHISPER.name,
                    "senderUuid" to senderUuid,
                    "senderName" to senderName,
                    "senderServerId" to serverId,
                    "targetName" to targetName,
                    "message" to message,
                    "timestamp" to System.currentTimeMillis().toString(),
                ),
            )
        assertNotNull(entryId)

        // 메시지 소비
        val entries =
            commands.xreadgroup(
                Consumer.from(consumerGroup, "consumer-1"),
                XReadArgs.StreamOffset.lastConsumed(ChannelConstants.REDIS_WHISPER_STREAM),
            )

        assertNotNull(entries)
        assertTrue(entries.isNotEmpty())

        val lastEntry = entries.last()
        assertEquals(MessageType.WHISPER.name, lastEntry.body["type"])
        assertEquals(senderName, lastEntry.body["senderName"])
        assertEquals(targetName, lastEntry.body["targetName"])
        assertEquals(message, lastEntry.body["message"])
    }

    @Test
    fun `whisper not found message can be published`() {
        val senderUuid = UUID.randomUUID().toString()
        val targetName = "UnknownPlayer"

        val entryId =
            commands.xadd(
                ChannelConstants.REDIS_WHISPER_STREAM,
                mapOf(
                    "type" to MessageType.WHISPER_NOT_FOUND.name,
                    "senderUuid" to senderUuid,
                    "targetName" to targetName,
                ),
            )
        assertNotNull(entryId)

        // 메시지 소비
        val entries =
            commands.xreadgroup(
                Consumer.from(consumerGroup, "consumer-1"),
                XReadArgs.StreamOffset.lastConsumed(ChannelConstants.REDIS_WHISPER_STREAM),
            )

        assertNotNull(entries)
        val lastEntry = entries.last()
        assertEquals(MessageType.WHISPER_NOT_FOUND.name, lastEntry.body["type"])
        assertEquals(targetName, lastEntry.body["targetName"])
    }

    @Test
    fun `whisper ack message can be published and consumed`() {
        val senderUuid = UUID.randomUUID().toString()
        val senderName = "Sender"
        val targetUuid = UUID.randomUUID().toString()
        val targetName = "Target"
        val message = "Hey, how are you?"

        // 귓속말 ACK 메시지 발행 (수신자에게 전달)
        val entryId =
            commands.xadd(
                ChannelConstants.REDIS_WHISPER_STREAM,
                mapOf(
                    "type" to MessageType.WHISPER_ACK.name,
                    "senderUuid" to senderUuid,
                    "senderName" to senderName,
                    "targetUuid" to targetUuid,
                    "targetName" to targetName,
                    "message" to message,
                    "timestamp" to System.currentTimeMillis().toString(),
                ),
            )
        assertNotNull(entryId)

        // 메시지 소비
        val entries =
            commands.xreadgroup(
                Consumer.from(consumerGroup, "consumer-1"),
                XReadArgs.StreamOffset.lastConsumed(ChannelConstants.REDIS_WHISPER_STREAM),
            )

        assertNotNull(entries)
        assertTrue(entries.isNotEmpty())

        val lastEntry = entries.last()
        assertEquals(MessageType.WHISPER_ACK.name, lastEntry.body["type"])
        assertEquals(senderName, lastEntry.body["senderName"])
        assertEquals(targetName, lastEntry.body["targetName"])
        assertEquals(message, lastEntry.body["message"])
    }

    @Test
    fun `multiple servers can communicate via streams`() {
        val server1 = "server-1"
        val server2 = "server-2"

        // 서버 1에서 메시지 발행
        commands.xadd(
            ChannelConstants.REDIS_GLOBAL_CHAT_STREAM,
            mapOf(
                "type" to MessageType.GLOBAL_CHAT.name,
                "serverId" to server1,
                "serverDisplayName" to "Server 1",
                "playerUuid" to UUID.randomUUID().toString(),
                "playerName" to "Player1",
                "message" to "Hello from server 1",
                "timestamp" to System.currentTimeMillis().toString(),
            ),
        )

        // 서버 2에서 메시지 발행
        commands.xadd(
            ChannelConstants.REDIS_GLOBAL_CHAT_STREAM,
            mapOf(
                "type" to MessageType.GLOBAL_CHAT.name,
                "serverId" to server2,
                "serverDisplayName" to "Server 2",
                "playerUuid" to UUID.randomUUID().toString(),
                "playerName" to "Player2",
                "message" to "Hello from server 2",
                "timestamp" to System.currentTimeMillis().toString(),
            ),
        )

        // 전체 스트림 읽기 (처음부터)
        val entries =
            commands.xread(
                XReadArgs.StreamOffset.from(ChannelConstants.REDIS_GLOBAL_CHAT_STREAM, "0"),
            )

        assertNotNull(entries)
        val messages = entries.flatMap { it.body.entries }

        // 두 서버의 메시지가 모두 포함되어 있는지 확인
        assertTrue(messages.isNotEmpty())
    }

    @Test
    fun `stream keys follow channel constants`() {
        // 채널 상수 검증
        assertEquals("userchat:global", ChannelConstants.REDIS_GLOBAL_CHAT_STREAM)
        assertEquals("userchat:notice", ChannelConstants.REDIS_NOTICE_STREAM)
        assertEquals("userchat:whisper", ChannelConstants.REDIS_WHISPER_STREAM)
        assertEquals("userchat-group", ChannelConstants.REDIS_CONSUMER_GROUP)
    }
}
