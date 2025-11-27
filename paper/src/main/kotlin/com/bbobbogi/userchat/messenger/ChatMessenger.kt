package com.bbobbogi.userchat.messenger

import com.bbobbogi.userchat.common.model.MessagingMode
import com.bbobbogi.userchat.common.protocol.GlobalChatMessage
import com.bbobbogi.userchat.common.protocol.NoticeMessage
import com.bbobbogi.userchat.common.protocol.WhisperMessage
import java.util.UUID

/**
 * 크로스서버 메시징 인터페이스
 */
interface ChatMessenger {
    fun getMode(): MessagingMode

    fun getServerId(): String

    fun getServerDisplayName(): String

    fun initialize()

    fun shutdown()

    // 전체 채팅
    fun broadcastGlobalChat(
        playerUuid: UUID,
        playerName: String,
        message: String,
    )

    fun setGlobalChatHandler(handler: (GlobalChatMessage) -> Unit)

    // 공지
    fun broadcastNotice(
        senderName: String,
        message: String,
    )

    fun setNoticeHandler(handler: (NoticeMessage) -> Unit)

    // 귓속말
    fun sendWhisper(
        senderUuid: UUID,
        senderName: String,
        targetName: String,
        message: String,
    )

    fun sendWhisperAck(
        senderUuid: String,
        success: Boolean,
    )

    fun setWhisperHandler(handler: (WhisperMessage) -> Unit)

    fun setWhisperNotFoundHandler(handler: (UUID, String) -> Unit)
}
