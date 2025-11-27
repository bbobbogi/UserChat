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
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)

    // Velocity API
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.junit.jupiter)
}

base {
    archivesName.set("UserChat-Velocity")
}

tasks {
    test {
        useJUnitPlatform()
    }

    build {
        dependsOn(shadowJar)
    }
}
