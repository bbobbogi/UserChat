package com.bbobbogi.userchat.service

import io.papermc.chzzkmultipleuser.feature.user.UserService
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.logging.Logger

/**
 * 플레이어 닉네임 제공자
 * ChzzkMultipleUser의 UserService가 있으면 사용하고, 없으면 기본 마인크래프트 이름 사용
 */
class UserNameProvider(
    private val plugin: Plugin,
    private val logger: Logger,
) {
    private var userService: UserService? = null

    fun initialize(): Boolean =
        try {
            val registration = Bukkit.getServicesManager().getRegistration(UserService::class.java)

            if (registration != null) {
                userService = registration.provider
                logger.info("[UserChat] ChzzkMultipleUser UserService 연동 완료")
                true
            } else {
                logger.info("[UserChat] ChzzkMultipleUser UserService를 찾을 수 없습니다. 기본 이름을 사용합니다.")
                false
            }
        } catch (e: NoClassDefFoundError) {
            logger.info("[UserChat] ChzzkMultipleUser가 설치되지 않았습니다. 기본 이름을 사용합니다.")
            false
        } catch (e: Exception) {
            logger.warning("[UserChat] UserService 초기화 실패: ${e.message}")
            false
        }

    /**
     * 플레이어의 표시 이름 가져오기
     */
    fun getDisplayName(player: Player): String =
        try {
            userService?.getDisplayName(player) ?: player.name
        } catch (e: Exception) {
            logger.warning("[UserChat] 닉네임 가져오기 실패: ${e.message}")
            player.name
        }

    /**
     * 온라인 플레이어 검색 (앞에서부터 매칭, 명령어 자동완성용)
     * @param prefix 검색할 접두사
     * @param limit 최대 결과 개수
     * @return 검색된 플레이어 이름 목록
     */
    fun searchByPrefix(
        prefix: String,
        limit: Int = 10,
    ): List<String> =
        try {
            val service = userService
            if (service != null) {
                service
                    .searchByPrefix(prefix, limit)
                    .results
                    .flatMap { listOfNotNull(it.nickname, it.minecraftName) }
                    .distinct()
            } else {
                Bukkit
                    .getOnlinePlayers()
                    .map { it.name }
                    .filter { it.lowercase().startsWith(prefix.lowercase()) }
                    .take(limit)
            }
        } catch (e: Exception) {
            Bukkit
                .getOnlinePlayers()
                .map { it.name }
                .filter { it.lowercase().startsWith(prefix.lowercase()) }
                .take(limit)
        }
}
