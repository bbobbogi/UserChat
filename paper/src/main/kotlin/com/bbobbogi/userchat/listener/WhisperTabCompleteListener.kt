package com.bbobbogi.userchat.listener

import com.bbobbogi.userchat.service.IUserNameProvider
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * 귓속말 명령어 탭 완성 리스너
 * AsyncTabCompleteEvent를 사용하여 비동기로 플레이어 검색
 */
class WhisperTabCompleteListener(
    private val userNameProvider: IUserNameProvider,
) : Listener {
    companion object {
        private val WHISPER_COMMANDS =
            setOf(
                "귓속말",
                "귓",
                "w",
                "whisper",
                "답장",
                "답",
                "r",
                "reply",
            )
    }

    @EventHandler
    fun onAsyncTabComplete(event: AsyncTabCompleteEvent) {
        val buffer = event.buffer
        if (!buffer.startsWith("/")) return

        val parts = buffer.removePrefix("/").split(" ", limit = 3)
        if (parts.isEmpty()) return

        val command = parts[0].lowercase()

        // 귓속말 명령어가 아니면 무시
        if (command !in WHISPER_COMMANDS) return

        // 답장 명령어는 플레이어 자동완성 불필요
        if (command in setOf("답장", "답", "r", "reply")) return

        // 첫 번째 인자(플레이어 이름)에서만 자동완성
        if (parts.size != 2) return

        val prefix = parts[1]

        // 비동기로 플레이어 검색
        userNameProvider
            .searchByPrefixAsync(prefix, 10)
            .thenAccept { results ->
                if (results.isNotEmpty()) {
                    event.completions = results
                }
            }
    }
}
