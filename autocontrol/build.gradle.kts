plugins {
    kotlin("jvm")
}

group = "tk.vhhg"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(project(":tables"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.codehaus.groovy:groovy-all:3.0.23")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation(libs.paho)
    implementation(libs.ktor.koin)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}