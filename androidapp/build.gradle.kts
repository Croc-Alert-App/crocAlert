plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.androidapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.androidapp"
        minSdk = 24
        targetSdk = 34
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
    implementation("androidx.activity:activity-compose:1.9.0")
}
