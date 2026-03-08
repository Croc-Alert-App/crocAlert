import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    kotlin("plugin.serialization") version "2.0.21"
}

kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    if (!isWindows) {
        listOf(
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "Shared"
                isStatic = true
            }
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinxCoroutinesCore)
            implementation(libs.kotlinxDatetime)

            implementation("io.ktor:ktor-client-core:2.3.12")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
        }

        androidMain.dependencies {
            implementation("io.ktor:ktor-client-android:2.3.12")
        }

        jvmMain.dependencies {
            implementation("io.ktor:ktor-client-cio:2.3.12")
        }

        if (!isWindows) {
            getByName("iosMain").dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.12")
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "crocalert.app.shared"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}