pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version "2.0.21"
        kotlin("plugin.serialization") version "2.0.21"
        id("io.ktor.plugin") version "2.3.12"
        id("com.android.application") version "8.3.2"
        id("com.android.library") version "8.3.2"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "crocAlert"

include(":composeApp")
include(":androidapp")
include(":shared")
include(":server")