plugins {
    kotlin("jvm")
}

group = "tk.vhhg"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.paho)
    implementation(libs.kotlin.coroutines)
    implementation(libs.firebase.admin)
    implementation(libs.ktor.koin)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(project(":tables"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}