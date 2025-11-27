package com.bbobbogi.userchat.service

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * 플레이어 닉네임 제공자 인터페이스
 */
interface IUserNameProvider {
    /**
     * 플레이어의 표시 이름 가져오기 (prefix 포함)
     */
    fun getDisplayName(player: Player): String

    /**
     * 플레이어의 순수 닉네임 가져오기 (prefix 미포함, 명령어용)
     */
    fun getPlayerName(player: Player): String

    /**
     * 이름 또는 닉네임으로 온라인 플레이어 찾기
     * @param name 찾을 이름 (마인크래프트 이름 또는 닉네임)
     * @return 찾은 플레이어 또는 null
     */
    fun findPlayerByName(name: String): Player?

    /**
     * 온라인 플레이어 검색 (앞에서부터 매칭, 명령어 자동완성용)
     * @param prefix 검색할 접두사
     * @param limit 최대 결과 개수 (기본값 10)
     * @return 검색된 플레이어 이름 목록
     */
    fun searchByPrefix(
        prefix: String,
        limit: Int,
    ): List<String>

    /**
     * 온라인 플레이어 검색 (기본 제한 10개)
     */
    fun searchByPrefix(prefix: String): List<String> = searchByPrefix(prefix, 10)

    /**
     * 이름 또는 닉네임으로 플레이어 찾기 (오프라인 포함, 네트워크 전체)
     * VelocityUserService 사용 시 네트워크 전체 플레이어 검색
     * @param name 찾을 이름 (마인크래프트 이름 또는 닉네임)
     * @return 찾은 플레이어 또는 null
     */
    fun findOfflinePlayerByName(name: String): OfflinePlayer?

    /**
     * 플레이어 검색 (비동기, 네트워크 전체)
     * 탭 자동완성에서 사용
     * @param prefix 검색할 접두사
     * @param limit 최대 결과 개수
     * @return 검색된 플레이어 이름 목록의 CompletableFuture
     */
    fun searchByPrefixAsync(
        prefix: String,
        limit: Int,
    ): CompletableFuture<List<String>>

    /**
     * 플레이어 검색 (비동기, 기본 제한 10개)
     */
    fun searchByPrefixAsync(prefix: String): CompletableFuture<List<String>> = searchByPrefixAsync(prefix, 10)
}
