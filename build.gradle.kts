import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.run.paper) apply false
    alias(libs.plugins.resource.factory) apply false
    alias(libs.plugins.resource.factory.velocity) apply false
}

// GitHub Packages 인증 정보 (local.properties 또는 환경변수에서 읽기)
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

val gprUser: String? = localProperties.getProperty("gpr.user")
    ?: (findProperty("gpr.user") as? String)
    ?: System.getenv("GITHUB_ACTOR")

val gprToken: String? = localProperties.getProperty("gpr.token")
    ?: (findProperty("gpr.token") as? String)
    ?: System.getenv("GITHUB_TOKEN")

// 하위 프로젝트에서 사용할 수 있도록 ext에 저장
extra["gprUser"] = gprUser
extra["gprToken"] = gprToken

allprojects {
    group = "com.bbobbogi"
    version = findProperty("version")?.toString()?.takeIf { it != "unspecified" }
        ?: "0.0.1-SNAPSHOT"
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc-repo"
            content {
                excludeGroup("io.github.bbobbogi")
            }
        }
        maven("https://oss.sonatype.org/content/groups/public/") {
            content {
                excludeGroup("io.github.bbobbogi")
            }
        }
        maven("https://repo.essentialsx.net/releases/") {
            content {
                excludeGroup("io.github.bbobbogi")
            }
        }
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
            content {
                excludeGroup("io.github.bbobbogi")
            }
        }
        maven("https://jitpack.io") {
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

    val targetJavaVersion = 21
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension>("kotlin") {
            jvmToolchain(targetJavaVersion)
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
            }
        }
    }
}
