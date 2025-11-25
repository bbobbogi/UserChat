package com.bbobbogi.userchat.item

import com.bbobbogi.userchat.config.UserChatConfig
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class GlobalChatItemManager(
    private val plugin: Plugin,
    private val config: UserChatConfig
) {
    private val nbtKey = NamespacedKey(plugin, "global_chat_item")

    /**
     * 전체 채팅 아이템 생성
     */
    fun createItem(amount: Int = 1): ItemStack {
        return ItemStack(config.itemMaterial, amount).apply {
            editMeta { meta ->
                meta.displayName(config.getItemDisplayNameComponent())
                meta.lore(config.getItemLoreComponents())

                config.itemCustomModelData?.let { customModelData ->
                    meta.setCustomModelData(customModelData)
                }

                // 식별용 PDC 태그
                meta.persistentDataContainer.set(
                    nbtKey,
                    PersistentDataType.BYTE,
                    1.toByte()
                )
            }
        }
    }

    /**
     * 전체 채팅 아이템인지 확인
     */
    fun isGlobalChatItem(item: ItemStack?): Boolean {
        if (item == null || item.type != config.itemMaterial) return false
        return item.itemMeta?.persistentDataContainer
            ?.has(nbtKey, PersistentDataType.BYTE) == true
    }

    /**
     * 플레이어의 전체 채팅 아이템 개수
     */
    fun countItems(player: Player): Int {
        return player.inventory.contents
            .filterNotNull()
            .filter { isGlobalChatItem(it) }
            .sumOf { it.amount }
    }

    /**
     * 아이템 1개 소비
     * @return 소비 성공 여부
     */
    fun consumeItem(player: Player): Boolean {
        for (item in player.inventory.contents) {
            if (item != null && isGlobalChatItem(item)) {
                item.amount -= 1
                return true
            }
        }
        return false
    }

    /**
     * 플레이어에게 아이템 지급
     */
    fun giveItem(player: Player, amount: Int) {
        val item = createItem(amount)
        val overflow = player.inventory.addItem(item)

        // 인벤토리가 가득 찬 경우 바닥에 드롭
        overflow.values.forEach { overflowItem ->
            player.world.dropItem(player.location, overflowItem)
        }
    }
}
