import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kover)
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
            iosX64(), iosArm64(), iosSimulatorArm64()
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

            implementation(libs.ktorClientCore)
            implementation(libs.ktorClientContentNegotiation)
            implementation(libs.ktorSerializationKotlinxJson)
            implementation(libs.kotlinxSerializationJson)
        }

        androidMain.dependencies {
            implementation(libs.ktorClientAndroid)
        }

        jvmMain.dependencies {
            implementation(libs.ktorClientCio)
        }

        if (!isWindows) {
            iosMain.dependencies {
                implementation(libs.ktorClientDarwin)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinxCoroutinesTest)
            implementation(libs.ktorClientMock)
        }

        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinxCoroutinesTest)
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

kover {
    reports {
        filters {
            excludes {
                packages("crocalert.app.shared.data.dto", "crocalert.app.shared.data.mapper")
            }
        }
        verify {
            rule("Minimum line coverage") {
                bound { minValue = 70 }
            }
        }
    }
}