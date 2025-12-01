package com.bbobbogi.userchat.gui

import com.bbobbogi.userchat.config.UserChatConfig
import com.bbobbogi.userchat.item.GlobalChatItemManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

class SettingsGui(
    private val plugin: Plugin,
    private val config: UserChatConfig,
    private val itemManager: GlobalChatItemManager,
) : Listener {
    private val miniMessage = MiniMessage.miniMessage()

    companion object {
        private const val GUI_TITLE = "<dark_gray>UserChat 설정"
        private const val SLOT_DISTANCE_TOGGLE = 10
        private const val SLOT_DISTANCE_RANGE = 11
        private const val SLOT_REQUIRE_ITEM = 12
        private const val SLOT_ITEM_PREVIEW = 14
        private const val SLOT_CLOSE = 22
    }

    fun open(player: Player) {
        val inventory = Bukkit.createInventory(null, 27, miniMessage.deserialize(GUI_TITLE))

        // 거리 기반 채팅 ON/OFF
        inventory.setItem(
            SLOT_DISTANCE_TOGGLE,
            createToggleItem(
                name = "거리 기반 채팅",
                enabled = config.distanceEnabled,
                lore =
                    listOf(
                        "<gray>현재 거리: <white>${config.distanceRange}블록",
                        "",
                        "<yellow>클릭하여 ON/OFF",
                    ),
            ),
        )

        // 거리 설정
        inventory.setItem(
            SLOT_DISTANCE_RANGE,
            createItem(
                material = Material.SPYGLASS,
                name = "<white>채팅 거리 설정",
                lore =
                    listOf(
                        "<gray>현재: <white>${config.distanceRange}블록",
                        "",
                        "<yellow>좌클릭: +10",
                        "<yellow>우클릭: -10",
                        "<yellow>Shift+클릭: ±50",
                    ),
            ),
        )

        // 전체 채팅 아이템 필요 여부
        inventory.setItem(
            SLOT_REQUIRE_ITEM,
            createToggleItem(
                name = "전체 채팅 아이템 필요",
                enabled = config.globalRequireItem,
                lore =
                    listOf(
                        "<gray>활성화 시 아이템 소모",
                        "",
                        "<yellow>클릭하여 ON/OFF",
                    ),
            ),
        )

        // 아이템 미리보기
        val previewItem =
            itemManager.createItem(1).apply {
                editMeta { meta ->
                    val lore = meta.lore()?.toMutableList() ?: mutableListOf()
                    lore.add(Component.empty())
                    lore.add(miniMessage.deserialize("<gray>(설정 미리보기)"))
                    meta.lore(lore)
                }
            }
        inventory.setItem(SLOT_ITEM_PREVIEW, previewItem)

        // 닫기
        inventory.setItem(
            SLOT_CLOSE,
            createItem(
                material = Material.BARRIER,
                name = "<red>닫기",
            ),
        )

        // 빈 칸 채우기
        val filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ")
        for (i in 0 until 27) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler)
            }
        }

        player.openInventory(inventory)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val title = event.view.title()
        if (title != miniMessage.deserialize(GUI_TITLE)) return

        event.isCancelled = true

        val player = event.whoClicked as? Player ?: return

        when (event.slot) {
            SLOT_DISTANCE_TOGGLE -> {
                config.distanceEnabled = !config.distanceEnabled
                config.save()
                open(player)
            }
            SLOT_DISTANCE_RANGE -> {
                val delta =
                    when {
                        event.isShiftClick && event.isLeftClick -> 50
                        event.isShiftClick && event.isRightClick -> -50
                        event.isLeftClick -> 10
                        event.isRightClick -> -10
                        else -> 0
                    }
                config.distanceRange = (config.distanceRange + delta).coerceIn(1, 500)
                config.save()
                open(player)
            }
            SLOT_REQUIRE_ITEM -> {
                config.globalRequireItem = !config.globalRequireItem
                config.save()
                open(player)
            }
            SLOT_CLOSE -> {
                player.closeInventory()
            }
        }
    }

    private fun createToggleItem(
        name: String,
        enabled: Boolean,
        lore: List<String>,
    ): ItemStack {
        val material = if (enabled) Material.LIME_DYE else Material.GRAY_DYE
        val status = if (enabled) "<green>활성화" else "<red>비활성화"

        return createItem(
            material = material,
            name = "<white>$name",
            lore = listOf("<gray>상태: $status") + lore,
        )
    }

    private fun createItem(
        material: Material,
        name: String,
        lore: List<String> = emptyList(),
    ): ItemStack =
        ItemStack(material).apply {
            editMeta { meta ->
                meta.displayName(
                    miniMessage
                        .deserialize(name)
                        .decoration(TextDecoration.ITALIC, false),
                )
                if (lore.isNotEmpty()) {
                    meta.lore(
                        lore.map { line ->
                            miniMessage
                                .deserialize(line)
                                .decoration(TextDecoration.ITALIC, false)
                        },
                    )
                }
            }
        }
}
