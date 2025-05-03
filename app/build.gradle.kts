
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "tk.vhhg"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.firebase.admin)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.koin)
    implementation(project(":auth"))
    implementation(project(":tables"))
    implementation(project(":rooms"))
    implementation(project(":imitation-model"))
    implementation(project(":autocontrol"))
    implementation(project(":pushfcm"))


    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(kotlin("test"))
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation(libs.ktor.serialization.kotlinx.json)
    testImplementation("io.ktor:ktor-client-logging:2.3.13")
    testImplementation(libs.paho)
}

kotlin {
    jvmToolchain(23)
}
