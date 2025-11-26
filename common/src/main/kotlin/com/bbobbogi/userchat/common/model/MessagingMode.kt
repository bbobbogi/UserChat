package com.bbobbogi.userchat.common.model

enum class MessagingMode {
    OFF, // 단일 서버 (cross-server 없음)
    PLUGIN_MESSAGE, // Velocity/BungeeCord PluginMessage
    REDIS, // Redis Stream (ChzzkMultipleUser)
    ;

    companion object {
        fun fromString(value: String): MessagingMode =
            when (value.uppercase()) {
                "PLUGIN_MESSAGE", "PLUGINMESSAGE" -> PLUGIN_MESSAGE
                "REDIS" -> REDIS
                else -> OFF
            }
    }
}
