package com.bbobbogi.userchat.common.model

enum class ChatMode(
    val displayName: String,
) {
    DISTANCE("거리 기반"),
    GLOBAL("전체"),
    ;

    companion object {
        fun fromString(value: String): ChatMode? =
            when (value.lowercase()) {
                "distance", "거리" -> DISTANCE
                "global", "전체" -> GLOBAL
                else -> null
            }
    }
}
