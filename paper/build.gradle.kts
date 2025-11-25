import java.util.Properties

plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper")
}

val chzzkVersion = "0.0.2"

// 루트 프로젝트에서 인증 정보 가져오기
val gprUser: String? = rootProject.extra["gprUser"] as String?
val gprToken: String? = rootProject.extra["gprToken"] as String?

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
        content {
            excludeGroup("io.github.bbobbogi")
        }
    }
    maven("https://maven.pkg.github.com/bbobbogi/chzzkmultipleuser") {
        name = "GitHubPackagesChzzkMultipleUser"
        content {
            includeGroup("io.papermc.chzzkmultipleuser")
        }
        credentials {
            username = gprUser ?: ""
            password = gprToken ?: ""
        }
    }
}

dependencies {
    implementation(project(":common"))

    // Paper API (1.20 minimum support)
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // ChzzkMultipleUser modules (optional)
    compileOnly("io.papermc.chzzkmultipleuser:common:$chzzkVersion")
    compileOnly("io.papermc.chzzkmultipleuser:feature-integration:$chzzkVersion")
}

tasks {
    runServer {
        minecraftVersion("1.20.4")
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("kotlinx.serialization", "com.bbobbogi.userchat.libs.kotlinx.serialization")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
