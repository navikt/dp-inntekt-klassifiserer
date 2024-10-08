

plugins {
    id("common")
    application
    alias(libs.plugins.shadow.jar)
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://jitpack.io")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

application {
    applicationName = "dp-inntekt-klassifiserer"
    mainClass.set("no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt")
}

dependencies {
    // Dagpenger
    implementation("com.github.navikt:dp-inntekt-kontrakter:1_20231220.55a8a9")

    // ktor http client
    implementation(libs.dp.biblioteker.oauth2.klient)
    implementation(libs.dp.biblioteker.ktor.klient.metrics)
    implementation(libs.rapids.and.rivers)

    implementation(libs.ktor.client.auth.jvm)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging.jvm)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.jackson)
    implementation("io.ktor:ktor-serialization-jackson:${libs.versions.ktor.get()}")

    // json
    implementation(libs.jackson.core)
    implementation(libs.jackson.datatype.jsr310)

    // Miljøkonfigurasjon
    implementation(libs.konfig)

    // Logging
    implementation(libs.kotlin.logging)

    // prometheus
    implementation("io.prometheus:simpleclient_common:0.16.0")
    implementation("io.prometheus:simpleclient_hotspot:0.16.0")
    implementation("io.prometheus:simpleclient_hotspot:0.16.0")

    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.testcontainer.postgresql)
    testImplementation(libs.mockk)
    testImplementation("com.github.tomakehurst:wiremock-standalone:3.0.1")
}

tasks.named("shadowJar") {
    dependsOn("test")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer::class.java)
}
