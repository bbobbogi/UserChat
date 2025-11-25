plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    implementation(project(":common"))

    // Velocity API
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)

    implementation(libs.kotlin.stdlib)
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("kotlinx.serialization", "com.bbobbogi.userchat.libs.kotlinx.serialization")
    }

    build {
        dependsOn(shadowJar)
    }
}
