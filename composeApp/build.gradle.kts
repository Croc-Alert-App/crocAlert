import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "ComposeApp"
                isStatic = true
            }
        }
    }

    jvm {
        mainRun {
            mainClass = "crocalert.app.MainKt"
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidxActivityCompose)
            implementation(libs.composeUiToolingPreview)
            implementation(libs.koinAndroidxCompose)
            implementation(libs.firebaseAuthKtx)
            implementation(libs.kotlinxCoroutinesPlayServices)
        }

        commonMain.dependencies {
            implementation(libs.androidxLifecycleViewmodelCompose)
            implementation(libs.androidxLifecycleRuntimeCompose)
            implementation(libs.navigationCompose)
            implementation(project(":shared"))

            implementation(libs.composeRuntime)
            implementation(libs.composeFoundation)
            implementation(libs.composeMaterial3)
            implementation(libs.composeUi)
            implementation(libs.composeComponentsResources)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinxDatetime)
            implementation(libs.kotlinxCoroutinesCore)
            implementation(libs.qrose)

            implementation(project.dependencies.platform(libs.koinBom))
            implementation(libs.koinCore)
            implementation(libs.koinCompose)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinxCoroutinesTest)
            implementation(libs.turbine)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinxCoroutinesSwing)
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

dependencies {
    debugImplementation(libs.composeUiTooling)
}

kover {
    reports {
        filters {
            excludes {
                packages(
                    "crocalert.app.theme",
                    "crocalert.app.ui.components",
                    "crocalert.app.feature.alerts.data",
                )
                annotatedBy("androidx.compose.runtime.Composable")
            }
        }
        verify {
            rule("Minimum line coverage") {
                bound { minValue = 30 }
            }
        }
    }
}

