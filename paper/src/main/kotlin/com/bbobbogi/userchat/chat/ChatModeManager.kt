package com.bbobbogi.userchat.chat

import com.bbobbogi.userchat.common.model.ChatMode
import com.bbobbogi.userchat.config.UserChatConfig
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ChatModeManager(
    private val config: UserChatConfig,
) {
    private val playerModes = ConcurrentHashMap<UUID, ChatMode>()

    fun getMode(playerUuid: UUID): ChatMode = playerModes[playerUuid] ?: config.defaultChatMode

    fun setMode(
        playerUuid: UUID,
        mode: ChatMode,
    ) {
        playerModes[playerUuid] = mode
    }

    fun toggleMode(playerUuid: UUID): ChatMode {
        val currentMode = getMode(playerUuid)
        val newMode =
            when (currentMode) {
                ChatMode.DISTANCE -> ChatMode.GLOBAL
                ChatMode.GLOBAL -> ChatMode.DISTANCE
            }
        setMode(playerUuid, newMode)
        return newMode
    }

    fun resetMode(playerUuid: UUID) {
        playerModes.remove(playerUuid)
    }

    fun clearAll() {
        playerModes.clear()
    }
}
