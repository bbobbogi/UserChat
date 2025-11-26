plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.resource.factory.velocity)
}

velocityPluginJson {
    main = "com.bbobbogi.userchat.velocity.UserChatVelocity"
    id = "userchat"
    name = "UserChat"
    version = project.version.toString()
    description = "거리 기반 채팅, 전체 채팅, 귓속말 시스템 (Velocity)"
    authors.add("dmf")

    dependency("chzzkmultipleuser", true)
}

dependencies {
    implementation(project(":common"))

    // Velocity API
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.junit.jupiter)
}

tasks {
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
}
