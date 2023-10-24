

plugins {
    id("common")
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
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
    implementation("com.github.navikt:dagpenger-events:2023081713361692272216.01ab7c590338")
    implementation("com.github.navikt:dagpenger-streams:20230831.f3d785")
    // gRpc
    implementation("com.github.navikt:dp-inntekt:2023.10.19-13.33.da5e8a6be9b1")

    // trengs for api key ApiKeyVerifier
    implementation("commons-codec:commons-codec:1.16.0")

    // kafka
    implementation("org.apache.kafka:kafka-streams:3.3.1")

    implementation("io.getunleash:unleash-client-java:8.4.0")

    // ktor http client
    implementation(libs.dp.biblioteker.oauth2.klient)
    implementation(libs.dp.biblioteker.ktor.klient.metrics)

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
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.21.0")
    implementation("org.apache.logging.log4j:log4j-layout-template-json:2.20.0")

    // prometheus
    implementation("io.prometheus:simpleclient_common:0.16.0")
    implementation("io.prometheus:simpleclient_hotspot:0.16.0")
    implementation("io.prometheus:simpleclient_hotspot:0.16.0")

    // sanity check mot jackson json
    testImplementation("com.squareup.moshi:moshi-adapters:1.14.0")

    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.testcontainer.postgresql)
    testImplementation(libs.mockk)
    testImplementation("org.apache.kafka:kafka-streams-test-utils:3.3.1")
    testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
}

tasks.named("shadowJar") {
    dependsOn("test")
}

tasks.named("compileKotlin") {
    dependsOn("spotlessCheck")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer::class.java)
}
