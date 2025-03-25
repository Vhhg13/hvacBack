plugins {
    kotlin("jvm")
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "tk.vhhg"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.paho)
    implementation(libs.kotlin.coroutines)
    implementation(libs.ktor.server.core)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}