package com.bbobbogi.userchat.messenger

import com.bbobbogi.userchat.common.model.MessagingMode
import com.bbobbogi.userchat.common.protocol.GlobalChatMessage
import com.bbobbogi.userchat.common.protocol.NoticeMessage
import com.bbobbogi.userchat.common.protocol.WhisperMessage
import java.util.UUID

/**
 * OFF 모드 - 단일 서버, 크로스서버 기능 없음
 */
class NoOpMessenger(
    private val serverName: String = "Server",
) : ChatMessenger {
    override fun getMode(): MessagingMode = MessagingMode.OFF

    override fun getServerId(): String = "local"

    override fun getServerDisplayName(): String = serverName

    override fun initialize() {
        // No-op
    }

    override fun shutdown() {
        // No-op
    }

    override fun broadcastGlobalChat(
        playerUuid: UUID,
        playerName: String,
        message: String,
    ) {
        // 단일 서버에서는 로컬에서 이미 처리됨
    }

    override fun setGlobalChatHandler(handler: (GlobalChatMessage) -> Unit) {
        // No-op
    }

    override fun broadcastNotice(
        senderName: String,
        message: String,
    ) {
        // 단일 서버에서는 로컬에서 이미 처리됨
    }

    override fun setNoticeHandler(handler: (NoticeMessage) -> Unit) {
        // No-op
    }

    override fun sendWhisper(
        senderUuid: UUID,
        senderName: String,
        targetName: String,
        message: String,
    ) {
        // 단일 서버에서는 로컬에서 이미 처리됨
    }

    override fun sendWhisperAck(
        senderUuid: String,
        success: Boolean,
    ) {
        // No-op
    }

    override fun setWhisperHandler(handler: (WhisperMessage) -> Unit) {
        // No-op
    }

    override fun setWhisperNotFoundHandler(handler: (UUID, String) -> Unit) {
        // No-op
    }
}
