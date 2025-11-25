package com.bbobbogi.userchat.common.protocol

import kotlinx.serialization.Serializable

@Serializable
data class GlobalChatMessage(
    val type: String = MessageType.GLOBAL_CHAT.name,
    val serverId: String,
    val serverDisplayName: String,
    val playerUuid: String,
    val playerName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class WhisperMessage(
    val type: String = MessageType.WHISPER.name,
    val senderUuid: String,
    val senderName: String,
    val senderServerId: String,
    val targetName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class WhisperAckMessage(
    val type: String = MessageType.WHISPER_ACK.name,
    val targetUuid: String,
    val success: Boolean,
    val targetServerId: String? = null
)

@Serializable
data class WhisperNotFoundMessage(
    val type: String = MessageType.WHISPER_NOT_FOUND.name,
    val senderUuid: String,
    val targetName: String
)
