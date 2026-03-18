plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "io.github.androidapp"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.androidapp"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // composeApp is now an androidLibrary — valid as a dependency
    implementation(project(":composeApp"))
    // shared is implementation-scoped in :composeApp so must be added here for ApiRoutes access
    implementation(project(":shared"))
    // activity-compose provides setContent / enableEdgeToEdge for this app module
    implementation(libs.androidxActivityCompose)
}
