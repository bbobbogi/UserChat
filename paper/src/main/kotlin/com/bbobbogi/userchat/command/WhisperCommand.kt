package com.bbobbogi.userchat.command

import com.bbobbogi.userchat.config.UserChatConfig
import com.bbobbogi.userchat.service.UserNameProvider
import com.bbobbogi.userchat.whisper.WhisperManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class WhisperCommand(
    private val config: UserChatConfig,
    private val whisperManager: WhisperManager,
    private val userNameProvider: UserNameProvider,
) : CommandExecutor,
    TabCompleter {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage(config.getMessage("player-only"))
            return true
        }

        if (!sender.hasPermission("userchat.whisper")) {
            sender.sendMessage(config.getMessage("no-permission"))
            return true
        }

        if (args.size < 2) {
            sender.sendMessage(config.getMessage("invalid-usage", "usage" to "/귓속말 <플레이어> <메시지>"))
            return true
        }

        val targetName = args[0]
        val message = args.drop(1).joinToString(" ")

        whisperManager.sendWhisper(sender, targetName, message)
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): List<String> {
        if (args.size == 1) {
            return userNameProvider.searchByPrefix(args[0])
        }
        return emptyList()
    }
}
