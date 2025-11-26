package com.bbobbogi.userchat.listener

import com.bbobbogi.userchat.chat.ChatModeManager
import com.bbobbogi.userchat.whisper.WhisperManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class PlayerConnectionListener(
    private val modeManager: ChatModeManager,
    private val whisperManager: WhisperManager,
) : Listener {
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val playerUuid = event.player.uniqueId

        // 채팅 모드 초기화
        modeManager.resetMode(playerUuid)

        // 귓속말 대상 정리
        whisperManager.clearPlayer(playerUuid)
    }
}
