package com.bbobbogi.userchat.chat

import com.bbobbogi.userchat.config.UserChatConfig
import com.bbobbogi.userchat.service.IUserNameProvider
import org.bukkit.entity.Player

class DistanceChatHandler(
    private val config: UserChatConfig,
    private val userNameProvider: IUserNameProvider,
) {
    /**
     * 거리 기반 채팅 처리
     * @return 메시지를 받은 플레이어 수
     */
    fun handleChat(
        player: Player,
        message: String,
    ): Int {
        if (!config.distanceEnabled) {
            // 거리 기반 채팅이 비활성화되면 일반 채팅처럼 동작
            return -1 // -1은 기본 채팅으로 처리하라는 신호
        }

        val range = config.distanceRange.toDouble()
        val playerName = userNameProvider.getPlayerName(player)
        val displayName = userNameProvider.getDisplayName(player)

        // 범위 내 플레이어 필터링 (같은 월드)
        val nearbyPlayers =
            player.world.players.filter { other ->
                other.location.distance(player.location) <= range
            }

        val formattedMessage = config.formatDistanceChat(playerName, displayName, message)

        // 범위 내 플레이어에게만 전송
        nearbyPlayers.forEach { it.sendMessage(formattedMessage) }

        return nearbyPlayers.size
    }
}
