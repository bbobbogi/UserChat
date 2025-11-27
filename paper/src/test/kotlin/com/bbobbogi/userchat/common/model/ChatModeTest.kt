package com.bbobbogi.userchat.common.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ChatModeTest {
    @Test
    fun `fromString returns DISTANCE for distance`() {
        assertEquals(ChatMode.DISTANCE, ChatMode.fromString("distance"))
    }

    @Test
    fun `fromString returns DISTANCE for 거리`() {
        assertEquals(ChatMode.DISTANCE, ChatMode.fromString("거리"))
    }

    @Test
    fun `fromString returns GLOBAL for global`() {
        assertEquals(ChatMode.GLOBAL, ChatMode.fromString("global"))
    }

    @Test
    fun `fromString returns GLOBAL for 전체`() {
        assertEquals(ChatMode.GLOBAL, ChatMode.fromString("전체"))
    }

    @Test
    fun `fromString is case insensitive`() {
        assertEquals(ChatMode.DISTANCE, ChatMode.fromString("DISTANCE"))
        assertEquals(ChatMode.DISTANCE, ChatMode.fromString("Distance"))
        assertEquals(ChatMode.GLOBAL, ChatMode.fromString("GLOBAL"))
        assertEquals(ChatMode.GLOBAL, ChatMode.fromString("Global"))
    }

    @Test
    fun `fromString returns null for invalid values`() {
        assertNull(ChatMode.fromString("invalid"))
        assertNull(ChatMode.fromString(""))
        assertNull(ChatMode.fromString("distanc"))
        assertNull(ChatMode.fromString("글로벌"))
    }

    @Test
    fun `DISTANCE has correct display name`() {
        assertEquals("거리 기반", ChatMode.DISTANCE.displayName)
    }

    @Test
    fun `GLOBAL has correct display name`() {
        assertEquals("전체", ChatMode.GLOBAL.displayName)
    }
}
