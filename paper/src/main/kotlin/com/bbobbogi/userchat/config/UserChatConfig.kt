package com.bbobbogi.userchat.config

import com.bbobbogi.userchat.common.model.ChatMode
import com.bbobbogi.userchat.common.model.MessagingMode
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.Plugin

class UserChatConfig(private val plugin: Plugin) {
    private val miniMessage = MiniMessage.miniMessage()

    // Messaging
    var messagingMode: MessagingMode = MessagingMode.OFF
        private set

    // Chat
    var defaultChatMode: ChatMode = ChatMode.DISTANCE
        private set
    var distanceEnabled: Boolean = true
    var distanceRange: Int = 100
    var globalRequireItem: Boolean = true
    var switchToDistanceOnNoItem: Boolean = true

    // Item
    var itemMaterial: Material = Material.PAPER
        private set
    var itemCustomModelData: Int? = 1001
        private set
    var itemDisplayName: String = "<gradient:gold:yellow>전체 채팅권</gradient>"
        private set
    var itemLore: List<String> = listOf()
        private set

    // Messages
    private var messages: Map<String, String> = mapOf()

    fun load() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()

        val config = plugin.config

        // Messaging
        messagingMode = MessagingMode.fromString(config.getString("messaging.mode", "OFF") ?: "OFF")

        // Chat
        defaultChatMode = ChatMode.fromString(config.getString("chat.default-mode", "DISTANCE") ?: "DISTANCE")
            ?: ChatMode.DISTANCE
        distanceEnabled = config.getBoolean("chat.distance.enabled", true)
        distanceRange = config.getInt("chat.distance.range", 100)
        globalRequireItem = config.getBoolean("chat.global.require-item", true)
        switchToDistanceOnNoItem = config.getBoolean("chat.global.switch-to-distance-on-no-item", true)

        // Item
        itemMaterial = Material.matchMaterial(config.getString("item.material", "PAPER") ?: "PAPER")
            ?: Material.PAPER
        itemCustomModelData = config.getInt("item.custom-model-data", 1001).takeIf { it > 0 }
        itemDisplayName = config.getString("item.display-name", "<gradient:gold:yellow>전체 채팅권</gradient>")
            ?: "<gradient:gold:yellow>전체 채팅권</gradient>"
        itemLore = config.getStringList("item.lore")

        // Messages
        val messagesSection = config.getConfigurationSection("messages")
        if (messagesSection != null) {
            messages = messagesSection.getKeys(false).associateWith { key ->
                messagesSection.getString(key, "") ?: ""
            }
        }
    }

    fun save() {
        val config = plugin.config

        config.set("chat.distance.enabled", distanceEnabled)
        config.set("chat.distance.range", distanceRange)
        config.set("chat.global.require-item", globalRequireItem)
        config.set("chat.global.switch-to-distance-on-no-item", switchToDistanceOnNoItem)

        plugin.saveConfig()
    }

    fun reload() {
        load()
    }

    // Message helpers
    fun getMessage(key: String, vararg replacements: Pair<String, String>): Component {
        var message = messages[key] ?: return Component.text("Missing message: $key")

        for ((placeholder, value) in replacements) {
            message = message.replace("%$placeholder%", value)
        }

        return miniMessage.deserialize(message)
    }

    fun getMessageRaw(key: String): String {
        return messages[key] ?: ""
    }

    fun getItemDisplayNameComponent(): Component {
        return miniMessage.deserialize(itemDisplayName)
    }

    fun getItemLoreComponents(): List<Component> {
        return itemLore.map { miniMessage.deserialize(it) }
    }

    fun formatDistanceChat(playerName: String, message: String): Component {
        val format = messages["distance-format"]
            ?: "<gray>[근처]</gray> <white>%player%</white>: %message%"
        return miniMessage.deserialize(
            format.replace("%player%", playerName).replace("%message%", message)
        )
    }

    fun formatGlobalChat(serverName: String, playerName: String, message: String): Component {
        val format = messages["global-format"]
            ?: "<gold>[전체]</gold> <gray>[%server%]</gray> <white>%player%</white>: %message%"
        return miniMessage.deserialize(
            format.replace("%server%", serverName)
                .replace("%player%", playerName)
                .replace("%message%", message)
        )
    }

    fun formatWhisperSent(targetName: String, message: String): Component {
        val format = messages["whisper-sent"] ?: "<gray>[나 → %target%] %message%</gray>"
        return miniMessage.deserialize(
            format.replace("%target%", targetName).replace("%message%", message)
        )
    }

    fun formatWhisperReceived(senderName: String, message: String): Component {
        val format = messages["whisper-received"] ?: "<gray>[%sender% → 나] %message%</gray>"
        return miniMessage.deserialize(
            format.replace("%sender%", senderName).replace("%message%", message)
        )
    }
}
