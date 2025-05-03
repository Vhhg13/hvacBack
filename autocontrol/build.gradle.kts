plugins {
    kotlin("jvm")
}

group = "tk.vhhg"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.coroutines)
    implementation(libs.groovy)
    implementation(libs.apache.commons.math)
    implementation(libs.paho)
    implementation(libs.ktor.koin)
    implementation(project(":tables"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}