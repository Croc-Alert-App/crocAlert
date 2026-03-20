plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("io.ktor.plugin")
    application
}

group = "crocalert.app"
version = "1.0.0"

application {
    mainClass.set("crocalert.app.ApplicationKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveFileName.set("server-all.jar")
    mergeServiceFiles()
}

dependencies {
    implementation(project(":shared")) // o projects.shared si tienes typesafe accessors bien configurado
    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("io.ktor:ktor-server-netty:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-server-status-pages:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("com.google.firebase:firebase-admin:9.2.0")
    testImplementation("io.ktor:ktor-server-test-host:2.3.12")
    testImplementation(kotlin("test-junit"))
}