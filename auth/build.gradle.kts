plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.github.johnrengelman.shadow") version "8.1.1"
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "tk.vhhg"
version = "unspecified"


repositories {
    mavenCentral()
}

dependencies {

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.koin)
    implementation(libs.bcrypt)
    implementation(libs.ktor.server.auth.jwt)

    testImplementation(kotlin("test"))

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}