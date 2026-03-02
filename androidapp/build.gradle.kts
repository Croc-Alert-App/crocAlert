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

    // ✅ IMPORTANTE: NO declarar iOS en Windows
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

            // Lifecycle SOLO Android
            implementation(libs.androidxLifecycleViewmodelCompose)
            implementation(libs.androidxLifecycleRuntimeCompose)

            // Koin Android Compose (NO navigation artifact raro)
            implementation("io.insert-koin:koin-androidx-compose:3.5.6")
        }

        commonMain.dependencies {
            implementation(libs.composeRuntime)
            implementation(libs.composeFoundation)
            implementation(libs.composeMaterial3)
            implementation(libs.composeUi)
            implementation(libs.composeComponentsResources)

            implementation(libs.kotlinxDatetime)

            // Koin multiplataforma
            implementation(project.dependencies.platform(libs.koinBom))
            implementation(libs.koinCore)
            implementation(libs.koinCompose)

            // ❌ ELIMINADO:
            // implementation(libs.koinComposeViewmodelNavigation)
            // implementation(libs.composeUiToolingPreview)
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
        }
    }
}

android {
    namespace = "crocalert.app"

    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
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
 * Tooling SOLO Android
 * Nunca en commonMain
 */
dependencies {
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