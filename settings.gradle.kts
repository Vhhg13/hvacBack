plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "hvac"
include(":app")
include(":auth")
include("imitation-model")
include("tables")
include("rooms")
include("autocontrol")
include("pushfcm")
