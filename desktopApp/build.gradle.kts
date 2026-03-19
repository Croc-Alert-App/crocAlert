import org.jetbrains.compose.desktop.application.dsl.TargetFormat

group = "crocalert.app"
version = "1.0.0"

plugins {
    kotlin("jvm")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":composeApp"))
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
}

compose.desktop {
    application {
        mainClass = "crocalert.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "CrocAlert"
            packageVersion = "1.0.0"
            vendor = "CrocAlert"
            description = "CrocAlert desktop application"
        }
    }
}
