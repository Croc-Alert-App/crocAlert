import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
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
                baseName = "ComposeApp"
                isStatic = true
            }
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidxActivityCompose)


            implementation(libs.androidxLifecycleViewmodelCompose)
            implementation(libs.androidxLifecycleRuntimeCompose)


            implementation("io.insert-koin:koin-androidx-compose:3.5.6")
        }

        commonMain.dependencies {
            implementation(libs.composeRuntime)
            implementation(libs.composeFoundation)
            implementation(libs.composeMaterial3)
            implementation(libs.composeUi)
            implementation(libs.composeComponentsResources)

            implementation(libs.kotlinxDatetime)


            implementation(project.dependencies.platform(libs.koinBom))
            implementation(libs.koinCore)
            implementation(libs.koinCompose)


        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinxDatetime)
            implementation(libs.kotlinxCoroutinesCore)
            implementation(libs.composeRuntime)
            implementation(libs.composeFoundation)
            implementation("io.ktor:ktor-client-core:2.3.12")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
            implementation("io.ktor:ktor-client-cio:2.3.12")
        }
    }
}

android {
    namespace = "crocalert.app"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()

    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

/**
 * ✅ Tooling/Preview SOLO para Android.
 * Se define aquí (a nivel módulo) para evitar que Gradle lo intente resolver para iOS/JVM.
 */
dependencies {
    // Solo Android: preview + tooling
    implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.6.11")
    debugImplementation("org.jetbrains.compose.ui:ui-tooling:1.6.11")
}

compose.desktop {
    application {
        mainClass = "crocalert.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "crocalert.app"
            packageVersion = "1.0.0"
        }
    }
}