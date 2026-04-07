plugins {
    kotlin("jvm") version "2.3.20" apply false
    id("com.gradleup.shadow") version "9.4.1" apply false
}

val javaVersion: String by project
val kotlinVersion: String by project

allprojects {
    group = "io.github.dawciobiel"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    configure<org.gradle.api.plugins.JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(javaVersion.toInt())
        }
    }

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(javaVersion.toInt())
    }
}
