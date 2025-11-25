package com.bbobbogi.userchat.listener

import com.bbobbogi.userchat.chat.ChatModeManager
import com.bbobbogi.userchat.chat.DistanceChatHandler
import com.bbobbogi.userchat.chat.GlobalChatHandler
import com.bbobbogi.userchat.common.model.ChatMode
import com.bbobbogi.userchat.config.UserChatConfig
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class ChatListener(
    private val config: UserChatConfig,
    private val modeManager: ChatModeManager,
    private val distanceChatHandler: DistanceChatHandler,
    private val globalChatHandler: GlobalChatHandler
) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val message = PlainTextComponentSerializer.plainText().serialize(event.message())

        // 거리 기반 채팅이 비활성화되어 있으면 기본 채팅 사용
        if (!config.distanceEnabled) {
            return
        }

        val mode = modeManager.getMode(player.uniqueId)

        when (mode) {
            ChatMode.DISTANCE -> {
                val result = distanceChatHandler.handleChat(player, message)
                if (result >= 0) {
                    // 거리 기반 채팅으로 처리됨
                    event.isCancelled = true
                }
                // result < 0 이면 기본 채팅으로 처리
            }
            ChatMode.GLOBAL -> {
                val result = globalChatHandler.handleChat(player, message)

                when (result) {
                    GlobalChatHandler.GlobalChatResult.SUCCESS -> {
                        event.isCancelled = true
                    }
                    GlobalChatHandler.GlobalChatResult.SWITCHED_TO_DISTANCE -> {
                        // 거리 모드로 전환됨, 거리 채팅으로 처리
                        val distanceResult = distanceChatHandler.handleChat(player, message)
                        if (distanceResult >= 0) {
                            event.isCancelled = true
                        }
                    }
                    GlobalChatHandler.GlobalChatResult.NO_ITEM -> {
                        // 아이템 없음, 메시지 전송 안 됨
                        event.isCancelled = true
                    }
                }
            }
        }
    }
}
