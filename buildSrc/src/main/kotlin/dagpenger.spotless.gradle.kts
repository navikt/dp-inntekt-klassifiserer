import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.diffplug.spotless")
}

val ktlintVersjon = "1.0.1"
spotless {
    kotlin {
        ktlint(ktlintVersjon)
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(ktlintVersjon)
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn("spotlessApply")
}
