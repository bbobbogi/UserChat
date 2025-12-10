package com.bbobbogi.userchat.config

import com.bbobbogi.core.message.MessageService
import com.bbobbogi.userchat.common.model.ChatMode
import com.bbobbogi.userchat.common.model.MessagingMode
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

class UserChatConfig(
    private val plugin: JavaPlugin,
) {
    private val miniMessage = MiniMessage.miniMessage()
    lateinit var messages: MessageService
        private set

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
    var globalChatCommand: String = ""
        private set

    // Item
    var itemMaterial: Material = Material.PAPER
        private set
    var itemCustomModelData: Int? = 1001
        private set
    var itemDisplayName: String = "<gradient:gold:yellow>전체 채팅권</gradient>"
        private set
    var itemLore: List<String> = listOf()
        private set

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
        globalChatCommand = config.getString("chat.global.command", "") ?: ""

        // Item
        itemMaterial = Material.matchMaterial(config.getString("item.material", "PAPER") ?: "PAPER")
            ?: Material.PAPER
        itemCustomModelData = config.getInt("item.custom-model-data", 1001).takeIf { it > 0 }
        itemDisplayName = config.getString("item.display-name", "<gradient:gold:yellow>전체 채팅권</gradient>")
            ?: "<gradient:gold:yellow>전체 채팅권</gradient>"
        itemLore = config.getStringList("item.lore")

        // Messages
        if (!::messages.isInitialized) {
            messages = MessageService(plugin, "messages.yml")
        } else {
            messages.reload()
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

    /**
     * 설정을 리로드합니다.
     * @return 서버 재시작이 필요한 변경사항이 있으면 경고 메시지 목록 반환
     */
    fun reload(): List<String> {
        val warnings = mutableListOf<String>()
        val oldGlobalChatCommand = globalChatCommand

        load()

        if (oldGlobalChatCommand != globalChatCommand) {
            warnings.add("전체채팅 명령어 변경은 서버 재시작 후 적용됩니다.")
        }

        return warnings
    }

    // Message helpers
    fun getMessage(
        key: String,
        vararg replacements: Pair<String, String>,
    ): Component =
        messages.getComponentOrDefault(
            key,
            "Missing message: $key",
            *replacements,
        )

    fun getMessageRaw(key: String): String = messages.getOrDefault(key, "")

    fun getItemDisplayNameComponent(): Component = miniMessage.deserialize(itemDisplayName)

    fun getItemLoreComponents(): List<Component> = itemLore.map { miniMessage.deserialize(it) }

    fun formatDistanceChat(
        playerName: String,
        displayName: String,
        message: String,
    ): Component =
        messages.getComponentOrDefault(
            "distance-format",
            "<gray>[근처]</gray> <white>%player%</white>: %message%",
            "player_name" to playerName,
            "player" to displayName,
            "message" to miniMessage.escapeTags(message),
        )

    fun formatGlobalChat(
        serverName: String,
        playerName: String,
        displayName: String,
        message: String,
    ): Component =
        messages.getComponentOrDefault(
            "global-format",
            "<gold>[전체]</gold> <gray>[%server%]</gray> <white>%player%</white>: %message%",
            "server" to serverName,
            "player_name" to playerName,
            "player" to displayName,
            "message" to miniMessage.escapeTags(message),
        )

    fun formatWhisperSent(
        targetName: String,
        targetDisplayName: String,
        message: String,
    ): Component =
        messages.getComponentOrDefault(
            "whisper-sent",
            "<gray>[나 → %target%] %message%</gray>",
            "target_name" to targetName,
            "target" to targetDisplayName,
            "message" to miniMessage.escapeTags(message),
        )

    fun formatWhisperReceived(
        senderName: String,
        senderDisplayName: String,
        message: String,
    ): Component =
        messages.getComponentOrDefault(
            "whisper-received",
            "<gray>[%sender% → 나] %message%</gray>",
            "sender_name" to senderName,
            "sender" to senderDisplayName,
            "message" to miniMessage.escapeTags(message),
        )

    fun formatNotice(
        senderName: String,
        message: String,
    ): Component =
        messages.getComponentOrDefault(
            "notice-format",
            "<red>[공지]</red> <yellow>%sender%</yellow>: %message%",
            "sender" to senderName,
            "message" to miniMessage.escapeTags(message),
        )
}
