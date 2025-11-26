package com.bbobbogi.userchat.command

import com.bbobbogi.userchat.config.UserChatConfig
import com.bbobbogi.userchat.whisper.WhisperManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ReplyCommand(
    private val config: UserChatConfig,
    private val whisperManager: WhisperManager,
) : CommandExecutor {
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

        if (args.isEmpty()) {
            sender.sendMessage(config.getMessage("invalid-usage", "usage" to "/답장 <메시지>"))
            return true
        }

        val message = args.joinToString(" ")
        whisperManager.reply(sender, message)
        return true
    }
}
