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
    implementation(project(":composeApp"))
    // :shared is not transitive from :composeApp — required for ApiRoutes
    implementation(project(":shared"))
    implementation(libs.androidxActivityCompose)
}
