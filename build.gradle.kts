import java.util.Properties

plugins {
    kotlin("jvm") version "2.0.0" apply false
    id("com.gradleup.shadow") version "8.3.0" apply false
    id("xyz.jpenilla.run-paper") version "2.3.1" apply false
}

// GitHub Packages 인증 정보 (local.properties 또는 환경변수에서 읽기)
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

val gprUser: String? = localProperties.getProperty("gpr.user")
    ?: findProperty("gpr.user") as String?
    ?: System.getenv("GITHUB_ACTOR")

val gprToken: String? = localProperties.getProperty("gpr.token")
    ?: findProperty("gpr.token") as String?
    ?: System.getenv("GITHUB_TOKEN")

// 하위 프로젝트에서 사용할 수 있도록 ext에 저장
extra["gprUser"] = gprUser
extra["gprToken"] = gprToken

allprojects {
    group = "com.bbobbogi"
    version = "1.0.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}
