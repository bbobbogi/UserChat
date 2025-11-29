package com.bbobbogi.userchat.service

import com.bbobbogi.user.UserService
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

/**
 * 플레이어 닉네임 제공자
 * BbobbogiPlugin의 UserService가 있으면 사용하고, 없으면 기본 마인크래프트 이름 사용
 */
class UserNameProvider(
    private val plugin: Plugin,
    private val logger: Logger,
) : IUserNameProvider {
    private var userService: UserService? = null

    fun initialize(): Boolean =
        try {
            val registration = Bukkit.getServicesManager().getRegistration(UserService::class.java)

            if (registration != null) {
                userService = registration.provider
                logger.info("[UserChat] BbobbogiPlugin UserService 연동 완료")
                true
            } else {
                logger.info("[UserChat] BbobbogiPlugin UserService를 찾을 수 없습니다. 기본 이름을 사용합니다.")
                false
            }
        } catch (e: NoClassDefFoundError) {
            logger.info("[UserChat] BbobbogiPlugin가 설치되지 않았습니다. 기본 이름을 사용합니다.")
            false
        } catch (e: Exception) {
            logger.warning("[UserChat] UserService 초기화 실패: ${e.message}")
            false
        }

    /**
     * 플레이어의 표시 이름 가져오기 (prefix 포함)
     */
    override fun getDisplayName(player: Player): String =
        try {
            userService?.getDisplayName(player) ?: player.name
        } catch (e: Exception) {
            logger.warning("[UserChat] 닉네임 가져오기 실패: ${e.message}")
            player.name
        }

    /**
     * 플레이어의 순수 닉네임 가져오기 (prefix 미포함, 명령어용)
     */
    override fun getPlayerName(player: Player): String =
        try {
            userService?.getPlayerName(player) ?: player.name
        } catch (e: Exception) {
            player.name
        }

    /**
     * 이름 또는 닉네임으로 온라인 플레이어 찾기
     * @param name 찾을 이름 (마인크래프트 이름 또는 닉네임)
     * @return 찾은 플레이어 또는 null
     */
    override fun findPlayerByName(name: String): Player? =
        try {
            userService?.findPlayerByName(name) ?: Bukkit.getPlayerExact(name)
        } catch (e: Exception) {
            Bukkit.getPlayerExact(name)
        }

    /**
     * 온라인 플레이어 검색 (앞에서부터 매칭, 명령어 자동완성용)
     * @param prefix 검색할 접두사
     * @param limit 최대 결과 개수
     * @return 검색된 플레이어 이름 목록
     */
    override fun searchByPrefix(
        prefix: String,
        limit: Int,
    ): List<String> =
        try {
            val service = userService
            if (service != null) {
                service
                    .searchByPrefix(prefix = prefix, limit = limit, cursor = null, exactMatch = false)
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

    /**
     * 이름 또는 닉네임으로 플레이어 찾기 (오프라인 포함, 네트워크 전체)
     * VelocityUserService 사용 시 네트워크 전체 플레이어 검색
     */
    override fun findOfflinePlayerByName(name: String): OfflinePlayer? =
        try {
            userService?.findOfflinePlayerByName(name)
        } catch (e: Exception) {
            logger.warning("[UserChat] 오프라인 플레이어 검색 실패: ${e.message}")
            null
        }

    /**
     * 플레이어 검색 (비동기, 온라인 플레이어)
     */
    override fun searchByPrefixAsync(
        prefix: String,
        limit: Int,
    ): CompletableFuture<List<String>> {
        val service = userService
        return if (service != null) {
            service
                .searchByPrefixAsync(prefix, limit, null, false)
                .thenApply { page ->
                    page.results
                        .flatMap { listOfNotNull(it.nickname, it.minecraftName) }
                        .distinct()
                }
        } else {
            CompletableFuture.completedFuture(
                Bukkit
                    .getOnlinePlayers()
                    .map { it.name }
                    .filter { it.lowercase().startsWith(prefix.lowercase()) }
                    .take(limit),
            )
        }
    }
}
