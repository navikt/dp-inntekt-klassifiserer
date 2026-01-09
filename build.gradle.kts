

plugins {
    id("common")
    application
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

application {
    applicationName = "dp-inntekt-klassifiserer"
    mainClass.set("no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt")
}

dependencies {
    // Dagpenger
    implementation("com.github.navikt:dp-inntekt-kontrakter:2_20251211.17f9d7")

    // ktor http client
    val version = "2025.12.19-08.15.2e150cd55270"
    implementation("no.nav.dagpenger:oauth2-klient:$version")
    implementation("no.nav.dagpenger:ktor-client-metrics:$version")
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
    testImplementation(libs.rapids.and.rivers.test)
}
