import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary) // composeApp debe ser library
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.composeUiToolingPreview)
            implementation(libs.androidxActivityCompose)
        }

        commonMain.dependencies {
            implementation(libs.composeRuntime)
            implementation(libs.composeFoundation)
            implementation(libs.composeMaterial3)
            implementation(libs.composeUi)
            implementation(libs.composeComponentsResources)
            implementation(libs.composeUiToolingPreview)

            implementation(libs.androidxLifecycleViewmodelCompose)
            implementation(libs.androidxLifecycleRuntimeCompose)

            implementation(libs.kotlinxDatetime)

            implementation(project.dependencies.platform(libs.koinBom))
            implementation(libs.koinCompose)
            implementation(libs.koinComposeViewmodelNavigation)

            // Si realmente usas el módulo shared:
            // implementation(project(":shared"))
        }

        commonTest.dependencies {
            // en tu TOML está definido como "kotlin-test"
            implementation(libs.kotlin.test)
            // si te da issue, usa:
            // implementation(libs.kotlin.testJunit)
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

    // en tu TOML NO existen android.compileSdk/minSdk/targetSdk,
    // existen androidCompileSdk/androidMinSdk/androidTargetSdk
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
        // targetSdk en library está deprecado, pero si quieres dejarlo:
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

dependencies {
    debugImplementation(libs.composeUiTooling)
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