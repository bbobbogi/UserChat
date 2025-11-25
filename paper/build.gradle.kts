plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

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

    // Paper API
    compileOnly(libs.paper.api)
    implementation(libs.kotlin.stdlib)

    // ChzzkMultipleUser modules (optional)
    compileOnly(libs.chzzk.common)
    compileOnly(libs.chzzk.feature.integration)
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
