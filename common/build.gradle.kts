plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.kotlin.stdlib)
    api(libs.kotlinx.serialization.json)
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}
