plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

dependencies {
    implementation(project(":common"))

    // Paper API
    compileOnly(libs.paper.api)
    implementation(libs.kotlin.stdlib)

    // ChzzkMultipleUser modules
    compileOnly(libs.chzzk.common)
    compileOnly(libs.chzzk.database)
    compileOnly(libs.chzzk.feature.integration)

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

tasks {
    runServer {
        minecraftVersion("1.20.4")
    }

    test {
        useJUnitPlatform()
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

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}
