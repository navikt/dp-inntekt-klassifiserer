

plugins {
    application
    kotlin("jvm")
    id("dagpenger.common")
    id("dagpenger.spotless")
    id(Shadow.shadow) version Shadow.version
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
    implementation(kotlin("stdlib-jdk8"))

    // Dagpenger
    implementation("com.github.navikt:dagpenger-events:2023081713361692272216.01ab7c590338")
    implementation("com.github.navikt:dagpenger-streams:20230831.f3d785")
    // gRpc
    implementation("com.github.navikt:dp-inntekt:2023.10.19-13.33.da5e8a6be9b1")

    // trengs for api key ApiKeyVerifier
    implementation("commons-codec:commons-codec:1.16.0")

    // kafka
    implementation(Kafka.streams)

    implementation("io.getunleash:unleash-client-java:8.4.0")

    // ktor http client
    implementation("com.github.navikt.dp-biblioteker:ktor-client-metrics:2023.04.27-09.33.fcf0798bf943")
    implementation("com.github.navikt.dp-biblioteker:oauth2-klient:2023.04.27-09.33.fcf0798bf943")
    implementation(Ktor2.Client.library("auth-jvm"))
    implementation(Ktor2.Client.library("cio"))
    implementation(Ktor2.Client.library("core"))
    implementation(Ktor2.Client.library("logging-jvm"))
    implementation(Ktor2.Client.library("content-negotiation"))
    implementation(Ktor2.Client.library("jackson"))
    implementation("io.ktor:ktor-serialization-jackson:${Ktor2.version}")

    // json
    implementation(Jackson.core)
    implementation(Jackson.jsr310)

    // Miljøkonfigurasjon
    implementation(Konfig.konfig)

    // Logging
    implementation(Kotlin.Logging.kotlinLogging)
    implementation(Log4j2.api)
    implementation(Log4j2.core)
    implementation(Log4j2.slf4j)
    implementation(Log4j2.library("layout-template-json"))

    // prometheus
    implementation(Prometheus.common)
    implementation(Prometheus.hotspot)
    implementation(Prometheus.log4j2)

    // testing
    testImplementation(kotlin("test"))
    testImplementation(Junit5.api)

    // sanity check mot jackson json
    testImplementation(Moshi.moshiAdapters)

    testRuntimeOnly(Junit5.engine)
    testImplementation(KoTest.runner)
    testImplementation(KoTest.assertions)
    testImplementation(TestContainers.postgresql)
    testImplementation(Mockk.mockk)
    testImplementation(Kafka.streamTestUtils)
    testImplementation(Wiremock.standalone)
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
