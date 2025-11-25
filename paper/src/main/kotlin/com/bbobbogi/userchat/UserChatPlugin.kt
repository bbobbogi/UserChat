package com.bbobbogi.userchat

import com.bbobbogi.userchat.chat.ChatModeManager
import com.bbobbogi.userchat.chat.DistanceChatHandler
import com.bbobbogi.userchat.chat.GlobalChatHandler
import com.bbobbogi.userchat.command.ReplyCommand
import com.bbobbogi.userchat.command.UserChatCommand
import com.bbobbogi.userchat.command.WhisperCommand
import com.bbobbogi.userchat.common.model.MessagingMode
import com.bbobbogi.userchat.config.UserChatConfig
import com.bbobbogi.userchat.gui.SettingsGui
import com.bbobbogi.userchat.item.GlobalChatItemManager
import com.bbobbogi.userchat.listener.ChatListener
import com.bbobbogi.userchat.listener.ItemInteractListener
import com.bbobbogi.userchat.listener.PlayerConnectionListener
import com.bbobbogi.userchat.messenger.ChatMessenger
import com.bbobbogi.userchat.messenger.NoOpMessenger
import com.bbobbogi.userchat.messenger.PluginMessageMessenger
import com.bbobbogi.userchat.messenger.RedisMessenger
import com.bbobbogi.userchat.service.UserNameProvider
import com.bbobbogi.userchat.whisper.WhisperManager
import org.bukkit.plugin.java.JavaPlugin

class UserChatPlugin : JavaPlugin() {

    lateinit var config: UserChatConfig
        private set

    private lateinit var userNameProvider: UserNameProvider
    private lateinit var messenger: ChatMessenger
    private lateinit var modeManager: ChatModeManager
    private lateinit var itemManager: GlobalChatItemManager
    private lateinit var distanceChatHandler: DistanceChatHandler
    private lateinit var globalChatHandler: GlobalChatHandler
    private lateinit var whisperManager: WhisperManager
    private lateinit var settingsGui: SettingsGui

    override fun onEnable() {
        // 설정 로드
        config = UserChatConfig(this)
        config.load()

        // 서비스 초기화
        userNameProvider = UserNameProvider(this, logger)
        userNameProvider.initialize()

        // 메시징 초기화
        messenger = createMessenger()
        messenger.initialize()

        // 매니저 초기화
        modeManager = ChatModeManager(config)
        itemManager = GlobalChatItemManager(this, config)

        // 핸들러 초기화
        distanceChatHandler = DistanceChatHandler(config, userNameProvider)
        globalChatHandler = GlobalChatHandler(config, itemManager, messenger, modeManager, userNameProvider)
        whisperManager = WhisperManager(config, messenger, userNameProvider)

        // GUI 초기화
        settingsGui = SettingsGui(this, config, itemManager)

        // 메시징 핸들러 설정
        setupMessagingHandlers()

        // 명령어 등록
        registerCommands()

        // 리스너 등록
        registerListeners()

        logger.info("[UserChat] 플러그인이 활성화되었습니다.")
        logger.info("[UserChat] 메시징 모드: ${messenger.getMode()}")
    }

    override fun onDisable() {
        messenger.shutdown()
        modeManager.clearAll()
        logger.info("[UserChat] 플러그인이 비활성화되었습니다.")
    }

    private fun createMessenger(): ChatMessenger {
        return when (config.messagingMode) {
            MessagingMode.OFF -> {
                NoOpMessenger(getServerName())
            }
            MessagingMode.PLUGIN_MESSAGE -> {
                val serverId = getServerId() ?: "server-${System.currentTimeMillis() % 10000}"
                val serverName = getServerName()
                PluginMessageMessenger(this, serverId, serverName, logger)
            }
            MessagingMode.REDIS -> {
                RedisMessenger(this, logger)
            }
        }
    }

    private fun getServerId(): String? {
        return try {
            val providerClass = Class.forName("io.papermc.chzzkmultipleuser.messaging.MessagingProvider")
            val method = providerClass.getMethod("getServerId")
            method.invoke(null) as? String
        } catch (e: Exception) {
            null
        }
    }

    private fun getServerName(): String {
        return try {
            val providerClass = Class.forName("io.papermc.chzzkmultipleuser.messaging.MessagingProvider")
            val method = providerClass.getMethod("getServerDisplayName")
            (method.invoke(null) as? String) ?: "Server"
        } catch (e: Exception) {
            "Server"
        }
    }

    private fun setupMessagingHandlers() {
        // 전체 채팅 수신 핸들러
        messenger.setGlobalChatHandler { message ->
            globalChatHandler.handleRemoteMessage(
                serverId = message.serverId,
                serverDisplayName = message.serverDisplayName,
                playerName = message.playerName,
                message = message.message
            )
        }

        // 귓속말 수신 핸들러
        messenger.setWhisperHandler { message ->
            whisperManager.handleRemoteWhisper(
                senderUuid = java.util.UUID.fromString(message.senderUuid),
                senderName = message.senderName,
                senderServerId = message.senderServerId,
                targetName = message.targetName,
                message = message.message
            )
        }

        // 귓속말 대상 없음 핸들러
        messenger.setWhisperNotFoundHandler { senderUuid, targetName ->
            whisperManager.handleWhisperNotFound(senderUuid, targetName)
        }
    }

    private fun registerCommands() {
        val userChatCommand = UserChatCommand(config, modeManager, itemManager, settingsGui)
        getCommand("유저채팅")?.apply {
            setExecutor(userChatCommand)
            tabCompleter = userChatCommand
        }

        val whisperCommand = WhisperCommand(config, whisperManager)
        getCommand("귓속말")?.apply {
            setExecutor(whisperCommand)
            tabCompleter = whisperCommand
        }

        val replyCommand = ReplyCommand(config, whisperManager)
        getCommand("답장")?.setExecutor(replyCommand)
    }

    private fun registerListeners() {
        val pluginManager = server.pluginManager

        pluginManager.registerEvents(
            ChatListener(config, modeManager, distanceChatHandler, globalChatHandler),
            this
        )

        pluginManager.registerEvents(
            ItemInteractListener(config, itemManager, modeManager),
            this
        )

        pluginManager.registerEvents(
            PlayerConnectionListener(modeManager, whisperManager),
            this
        )

        pluginManager.registerEvents(settingsGui, this)
    }
}
