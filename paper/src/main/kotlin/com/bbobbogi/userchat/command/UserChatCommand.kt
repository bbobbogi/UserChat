package com.bbobbogi.userchat.command

import com.bbobbogi.userchat.chat.ChatModeManager
import com.bbobbogi.userchat.common.model.ChatMode
import com.bbobbogi.userchat.config.UserChatConfig
import com.bbobbogi.userchat.gui.SettingsGui
import com.bbobbogi.userchat.item.GlobalChatItemManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class UserChatCommand(
    private val config: UserChatConfig,
    private val modeManager: ChatModeManager,
    private val itemManager: GlobalChatItemManager,
    private val settingsGui: SettingsGui
) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "모드", "mode" -> handleMode(sender, args)
            "아이템지급", "give" -> handleGive(sender, args)
            "재로드", "reload" -> handleReload(sender)
            "관리", "admin", "settings" -> handleSettings(sender)
            else -> sendHelp(sender)
        }

        return true
    }

    private fun handleMode(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage(config.getMessage("player-only"))
            return
        }

        if (args.size < 2) {
            // 현재 모드 표시
            val mode = modeManager.getMode(sender.uniqueId)
            sender.sendMessage(config.getMessage("mode-current", "mode" to mode.displayName))
            return
        }

        val newMode = ChatMode.fromString(args[1])
        if (newMode == null) {
            sender.sendMessage(config.getMessage("invalid-mode"))
            return
        }

        modeManager.setMode(sender.uniqueId, newMode)
        sender.sendMessage(config.getMessage("mode-changed", "mode" to newMode.displayName))
    }

    private fun handleGive(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("userchat.admin")) {
            sender.sendMessage(config.getMessage("no-permission"))
            return
        }

        if (args.size < 2) {
            sender.sendMessage(config.getMessage("invalid-usage", "usage" to "/유저채팅 아이템지급 <플레이어> [수량]"))
            return
        }

        val target = Bukkit.getPlayer(args[1])
        if (target == null) {
            sender.sendMessage(config.getMessage("player-not-found", "player" to args[1]))
            return
        }

        val amount = args.getOrNull(2)?.toIntOrNull() ?: 1
        itemManager.giveItem(target, amount)

        sender.sendMessage(config.getMessage("item-given-admin", "player" to target.name, "amount" to amount.toString()))
        target.sendMessage(config.getMessage("item-given", "amount" to amount.toString()))
    }

    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("userchat.admin")) {
            sender.sendMessage(config.getMessage("no-permission"))
            return
        }

        config.reload()
        sender.sendMessage(config.getMessage("config-reloaded"))
    }

    private fun handleSettings(sender: CommandSender) {
        if (!sender.hasPermission("userchat.admin")) {
            sender.sendMessage(config.getMessage("no-permission"))
            return
        }

        if (sender !is Player) {
            sender.sendMessage(config.getMessage("player-only"))
            return
        }

        settingsGui.open(sender)
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6=== UserChat 도움말 ===")
        sender.sendMessage("§e/유저채팅 모드 [거리|전체] §7- 채팅 모드 확인/변경")

        if (sender.hasPermission("userchat.admin")) {
            sender.sendMessage("§e/유저채팅 아이템지급 <플레이어> [수량] §7- 전체 채팅권 지급")
            sender.sendMessage("§e/유저채팅 재로드 §7- 설정 리로드")
            sender.sendMessage("§e/유저채팅 관리 §7- GUI 설정 열기")
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            val subCommands = mutableListOf("모드", "mode")
            if (sender.hasPermission("userchat.admin")) {
                subCommands.addAll(listOf("아이템지급", "give", "재로드", "reload", "관리", "admin", "settings"))
            }
            return subCommands.filter { it.startsWith(args[0].lowercase()) }
        }

        if (args.size == 2) {
            when (args[0].lowercase()) {
                "모드", "mode" -> return listOf("거리", "전체", "distance", "global")
                    .filter { it.startsWith(args[1].lowercase()) }
                "아이템지급", "give" -> {
                    if (sender.hasPermission("userchat.admin")) {
                        return Bukkit.getOnlinePlayers()
                            .map { it.name }
                            .filter { it.lowercase().startsWith(args[1].lowercase()) }
                    }
                }
            }
        }

        return emptyList()
    }
}
