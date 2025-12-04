package com.bbobbogi.userchat.listener

import com.bbobbogi.userchat.chat.ChatModeManager
import com.bbobbogi.userchat.common.model.ChatMode
import com.bbobbogi.userchat.config.UserChatConfig
import com.bbobbogi.userchat.item.GlobalChatItemManager
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.time.Duration

class ItemInteractListener(
    private val config: UserChatConfig,
    private val itemManager: GlobalChatItemManager,
    private val modeManager: ChatModeManager,
) : Listener {
    private val miniMessage = MiniMessage.miniMessage()

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // 우클릭만 처리
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val item = event.item ?: return
        if (!itemManager.isGlobalChatItem(item)) return

        event.isCancelled = true

        val player = event.player
        val currentMode = modeManager.getMode(player.uniqueId)

        if (currentMode == ChatMode.GLOBAL) {
            // 이미 전체 채팅 모드 → 거리 모드로 전환
            modeManager.setMode(player.uniqueId, ChatMode.DISTANCE)
            player.sendMessage(config.getMessage("global-mode-deactivated"))
        } else {
            // 전체 채팅 모드로 전환
            modeManager.setMode(player.uniqueId, ChatMode.GLOBAL)

            // 타이틀 표시
            player.showTitle(
                Title.title(
                    miniMessage.deserialize("<gold>전체 채팅 모드"),
                    miniMessage.deserialize("<gray>다음 채팅부터 전체 채팅됩니다. 채팅당 아이템 1개 소모"),
                    Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofSeconds(3),
                        Duration.ofMillis(500),
                    ),
                ),
            )

            player.sendMessage(config.getMessage("global-mode-activated"))
        }
    }
}
