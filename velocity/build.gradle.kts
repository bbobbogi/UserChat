plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    implementation(project(":common"))

    // Velocity API
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib")
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
