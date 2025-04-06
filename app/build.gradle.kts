
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
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(kotlin("test"))
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation(libs.ktor.serialization.kotlinx.json)
    testImplementation("io.ktor:ktor-client-logging:2.3.13")

    implementation(libs.ktor.koin)
    implementation("io.insert-koin:koin-ktor:4.0.2")

    implementation(project(":auth"))
    implementation(project(":tables"))
    implementation(project(":rooms"))
    implementation(project(":imitation-model"))
}
kotlin {
    jvmToolchain(23)
}
