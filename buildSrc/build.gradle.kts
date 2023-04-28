plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.8.21"
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.0.1")
}
