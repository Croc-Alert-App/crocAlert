plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kover)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
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

            implementation(libs.sqldelightCoroutines)
        }

        androidMain.dependencies {
            implementation(libs.ktorClientAndroid)
            implementation(libs.sqldelightAndroid)
            implementation(libs.workmanagerKtx)
            implementation(libs.datastorePrefs)
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
            implementation(libs.sqldelightSqlite)
            implementation(libs.turbine)
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

sqldelight {
    databases {
        create("CrocAlertDb") {
            packageName.set("crocalert.app.db")
            version = 3
        }
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