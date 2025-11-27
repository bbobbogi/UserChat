package com.bbobbogi.userchat.whisper

import com.bbobbogi.userchat.common.model.MessagingMode
import com.bbobbogi.userchat.config.UserChatConfig
import com.bbobbogi.userchat.messenger.ChatMessenger
import com.bbobbogi.userchat.service.IUserNameProvider
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class WhisperManager(
    private val config: UserChatConfig,
    private val messenger: ChatMessenger,
    private val userNameProvider: IUserNameProvider,
) {
    companion object {
        val NIL_UUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    }

    // 마지막 귓속말 상대 (답장용)
    private val lastWhisperFrom = ConcurrentHashMap<UUID, WhisperTarget>()

    data class WhisperTarget(
        val uuid: UUID,
        val name: String,
        // null이면 로컬
        val serverId: String?,
    )

    /**
     * 귓속말 전송
     */
    fun sendWhisper(
        sender: Player,
        targetName: String,
        message: String,
    ): WhisperResult {
        // 자기 자신에게 귓속말 방지
        if (targetName.equals(sender.name, ignoreCase = true)) {
            sender.sendMessage(config.getMessage("whisper-self"))
            return WhisperResult.SELF
        }

        // 로컬에서 대상 찾기 (마인크래프트 이름 또는 닉네임)
        val localTarget = userNameProvider.findPlayerByName(targetName)

        if (localTarget != null) {
            // 로컬 귓속말
            sendLocalWhisper(sender, localTarget, message)
            return WhisperResult.SUCCESS
        }

        // 원격 귓속말 시도 (메시징 모드가 OFF가 아닌 경우)
        if (messenger.getMode() != MessagingMode.OFF) {
            // 네트워크에서 플레이어 존재 확인 (VelocityUserService 사용 시 네트워크 전체 검색)
            val networkTarget = userNameProvider.findOfflinePlayerByName(targetName)
            if (networkTarget == null) {
                // 네트워크 전체에서 찾을 수 없음
                sender.sendMessage(config.getMessage("player-not-found", "player" to targetName))
                return WhisperResult.NOT_FOUND
            }

            sendRemoteWhisper(sender, targetName, message)
            return WhisperResult.SENT_REMOTE
        }

        // 메시징 OFF - 네트워크에서도 확인
        val networkTarget = userNameProvider.findOfflinePlayerByName(targetName)
        if (networkTarget != null) {
            // 플레이어가 네트워크에 있지만 메시징 불가
            sender.sendMessage(config.getMessage("player-not-found", "player" to targetName))
        } else {
            // 네트워크 전체에서 찾을 수 없음
            sender.sendMessage(config.getMessage("player-not-found", "player" to targetName))
        }
        return WhisperResult.NOT_FOUND
    }

    /**
     * 로컬 귓속말 전송
     */
    private fun sendLocalWhisper(
        sender: Player,
        target: Player,
        message: String,
    ) {
        val senderPlayerName = userNameProvider.getPlayerName(sender)
        val senderDisplayName = userNameProvider.getDisplayName(sender)
        val targetPlayerName = userNameProvider.getPlayerName(target)
        val targetDisplayName = userNameProvider.getDisplayName(target)

        // 발신자 화면
        sender.sendMessage(config.formatWhisperSent(targetPlayerName, targetDisplayName, message))

        // 수신자 화면
        target.sendMessage(config.formatWhisperReceived(senderPlayerName, senderDisplayName, message))

        // 답장 대상 저장 (순수 이름 저장)
        lastWhisperFrom[target.uniqueId] =
            WhisperTarget(
                uuid = sender.uniqueId,
                name = senderPlayerName,
                serverId = null,
            )
        lastWhisperFrom[sender.uniqueId] =
            WhisperTarget(
                uuid = target.uniqueId,
                name = targetPlayerName,
                serverId = null,
            )
    }

    /**
     * 원격 귓속말 전송
     */
    private fun sendRemoteWhisper(
        sender: Player,
        targetName: String,
        message: String,
    ) {
        val senderDisplayName = userNameProvider.getDisplayName(sender)

        // 메시지 전송
        messenger.sendWhisper(
            senderUuid = sender.uniqueId,
            senderName = senderDisplayName,
            targetName = targetName,
            message = message,
        )

        // 발신자에게 표시 (일단 전송됨으로 표시, 원격에서는 displayName 없으므로 둘 다 같은 값)
        sender.sendMessage(config.formatWhisperSent(targetName, targetName, message))

        // 답장 대상 저장 (원격)
        lastWhisperFrom[sender.uniqueId] =
            WhisperTarget(
                uuid = NIL_UUID,
                name = targetName,
                serverId = "remote",
            )
    }

    /**
     * 원격 귓속말 수신 처리
     */
    fun handleRemoteWhisper(
        senderUuid: UUID,
        senderName: String,
        senderServerId: String,
        targetName: String,
        message: String,
    ) {
        val target = userNameProvider.findPlayerByName(targetName) ?: return

        // 수신자에게 표시 (원격에서는 displayName만 전송되므로 둘 다 같은 값)
        target.sendMessage(config.formatWhisperReceived(senderName, senderName, message))

        // 답장 대상 저장 (원격, senderName은 displayName이지만 원격이므로 그대로 사용)
        lastWhisperFrom[target.uniqueId] =
            WhisperTarget(
                uuid = senderUuid,
                name = senderName,
                serverId = senderServerId,
            )

        // 발신 서버에 전달 완료 알림
        messenger.sendWhisperAck(senderUuid.toString(), true)
    }

    /**
     * 귓속말 대상을 찾을 수 없을 때 발신자에게 알림
     */
    fun handleWhisperNotFound(
        senderUuid: UUID,
        targetName: String,
    ) {
        val sender = Bukkit.getPlayer(senderUuid) ?: return
        sender.sendMessage(config.getMessage("player-not-found", "player" to targetName))
    }

    /**
     * 답장
     */
    fun reply(
        sender: Player,
        message: String,
    ): WhisperResult {
        val target = lastWhisperFrom[sender.uniqueId]

        if (target == null) {
            sender.sendMessage(config.getMessage("no-reply-target"))
            return WhisperResult.NO_TARGET
        }

        if (target.serverId == null) {
            // 로컬 답장
            val localTarget = Bukkit.getPlayer(target.uuid)
            if (localTarget != null) {
                sendLocalWhisper(sender, localTarget, message)
                return WhisperResult.SUCCESS
            } else {
                sender.sendMessage(config.getMessage("player-not-found", "player" to target.name))
                return WhisperResult.NOT_FOUND
            }
        } else {
            // 원격 답장
            sendRemoteWhisper(sender, target.name, message)
            return WhisperResult.SENT_REMOTE
        }
    }

    /**
     * 플레이어 로그아웃 시 정리
     */
    fun clearPlayer(playerUuid: UUID) {
        lastWhisperFrom.remove(playerUuid)
    }

    enum class WhisperResult {
        SUCCESS,
        SENT_REMOTE,
        NOT_FOUND,
        NO_TARGET,
        SELF,
    }
}
