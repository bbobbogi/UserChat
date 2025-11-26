package com.bbobbogi.userchat.chat

import com.bbobbogi.userchat.common.model.ChatMode
import com.bbobbogi.userchat.config.UserChatConfig
import com.bbobbogi.userchat.item.GlobalChatItemManager
import com.bbobbogi.userchat.messenger.ChatMessenger
import com.bbobbogi.userchat.service.UserNameProvider
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class GlobalChatHandler(
    private val config: UserChatConfig,
    private val itemManager: GlobalChatItemManager,
    private val messenger: ChatMessenger,
    private val modeManager: ChatModeManager,
    private val userNameProvider: UserNameProvider,
) {
    /**
     * 전체 채팅 처리
     * @return 처리 결과 (SUCCESS, NO_ITEM, SWITCHED_TO_DISTANCE)
     */
    fun handleChat(
        player: Player,
        message: String,
    ): GlobalChatResult {
        val hasBypass = player.hasPermission("userchat.bypass")

        // bypass 권한이 없고 아이템이 필요한 경우
        if (!hasBypass && config.globalRequireItem) {
            if (itemManager.countItems(player) <= 0) {
                // 아이템 없음
                if (config.switchToDistanceOnNoItem) {
                    // 거리 모드로 전환
                    modeManager.setMode(player.uniqueId, ChatMode.DISTANCE)
                    player.sendMessage(config.getMessage("no-item-switched"))
                    return GlobalChatResult.SWITCHED_TO_DISTANCE
                } else {
                    player.sendMessage(config.getMessage("no-item"))
                    return GlobalChatResult.NO_ITEM
                }
            }

            // 아이템 소비
            itemManager.consumeItem(player)
            val remaining = itemManager.countItems(player)
            player.sendMessage(config.getMessage("item-consumed", "remaining" to remaining.toString()))
        }

        val playerName = userNameProvider.getDisplayName(player)
        val serverName = messenger.getServerDisplayName()

        // 로컬 브로드캐스트
        broadcastLocal(serverName, playerName, message)

        // 다른 서버에 전송
        messenger.broadcastGlobalChat(player.uniqueId, playerName, message)

        return GlobalChatResult.SUCCESS
    }

    /**
     * 원격 서버에서 받은 전체 채팅 처리
     */
    fun handleRemoteMessage(
        serverId: String,
        serverDisplayName: String,
        playerName: String,
        message: String,
    ) {
        // 자기 서버 메시지면 무시
        if (serverId == messenger.getServerId()) return

        broadcastLocal(serverDisplayName, playerName, message)
    }

    private fun broadcastLocal(
        serverName: String,
        playerName: String,
        message: String,
    ) {
        val formattedMessage = config.formatGlobalChat(serverName, playerName, message)
        Bukkit.getOnlinePlayers().forEach { it.sendMessage(formattedMessage) }
    }

    enum class GlobalChatResult {
        SUCCESS,
        NO_ITEM,
        SWITCHED_TO_DISTANCE,
    }
}
