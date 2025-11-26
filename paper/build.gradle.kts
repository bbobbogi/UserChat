import xyz.jpenilla.resourcefactory.bukkit.Permission

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.resource.factory)
}

bukkitPluginYaml {
    name = "UserChat"
    version = project.version.toString()
    main = "com.bbobbogi.userchat.UserChatPlugin"
    apiVersion = "1.20"
    description = "거리 기반 채팅, 전체 채팅, 귓속말 시스템"
    authors.add("bbobbogi")

    softDepend.add("ChzzkMultipleUser")

    // libraries.addAll(
    //     libs.kotlin.stdlib
    //         .get()
    //         .toString(),
    //     libs.kotlinx.serialization.json
    //         .get()
    //         .toString(),
    //     libs.exposed.core
    //         .get()
    //         .toString(),
    //     libs.exposed.dao
    //         .get()
    //         .toString(),
    // )

    commands {
        register("유저채팅") {
            description = "유저 채팅 관리"
            permission = "userchat.use"
            aliases.addAll("userchat", "uc")
        }
        register("귓속말") {
            description = "귓속말 전송"
            permission = "userchat.whisper"
            aliases.addAll("귓", "w", "whisper")
        }
        register("답장") {
            description = "마지막 귓속말 상대에게 답장"
            permission = "userchat.whisper"
            aliases.addAll("답", "r", "reply")
        }
    }

    permissions {
        register("userchat.use") {
            description = "기본 채팅 기능 사용"
            default = Permission.Default.TRUE
        }
        register("userchat.bypass") {
            description = "아이템 없이 전체 채팅 사용"
            default = Permission.Default.OP
        }
        register("userchat.admin") {
            description = "관리자 명령어 사용"
            default = Permission.Default.OP
        }
        register("userchat.whisper") {
            description = "귓속말 사용"
            default = Permission.Default.TRUE
        }
        register("userchat.whisper.bypass") {
            description = "귓속말 차단 무시"
            default = Permission.Default.OP
        }
    }
}

dependencies {
    implementation(project(":common"))
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlinx.serialization.json)

    // Paper API
    compileOnly(libs.paper.api)

    // ChzzkMultipleUser modules
    compileOnly(libs.chzzk.core)
    compileOnly(libs.chzzk.common)
    compileOnly(libs.chzzk.database)
    compileOnly(libs.chzzk.feature.integration)
    compileOnly(libs.chzzk.messaging)

    // Exposed (for table definitions)
    compileOnly(libs.exposed.core)
    compileOnly(libs.exposed.dao)

    // Optional dependencies
    compileOnly(libs.essentialsx)
    compileOnly(libs.placeholderapi)

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.junit.jupiter)
}

base {
    archivesName.set("UserChat-Paper")
}

tasks {
    runServer {
        minecraftVersion("1.20.4")
    }

    test {
        useJUnitPlatform()
    }

    build {
        dependsOn(shadowJar)
    }
}
