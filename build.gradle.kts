

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
}

application {
    applicationName = "dp-inntekt-klassifiserer"
    mainClass.set("no.nav.dagpenger.inntekt.klassifiserer.ApplicationKt")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // Dagpenger
    implementation(Dagpenger.Streams)
    implementation(Dagpenger.Events)
    implementation(Dagpenger.Biblioteker.ktorUtils)

    // gRpc
    implementation("com.github.navikt:dp-inntekt:2023.01.11-12.56.a8fde01194fb")

    // kafka
    implementation(Kafka.streams)

    // ktor
    implementation(Ktor2.Server.library("netty"))

    // ktor http client
    implementation(Dagpenger.Biblioteker.Ktor.Client.metrics)
    implementation("com.github.navikt.dp-biblioteker:oauth2-klient:2022.02.05-16.32.da1deab37b31")
    implementation(Ktor2.Client.library("auth-jvm"))
    implementation(Ktor2.Client.library("cio"))
    implementation(Ktor2.Client.library("core"))
    implementation(Ktor2.Client.library("logging-jvm"))
    implementation(Ktor2.Client.library("content-negotiation"))
    implementation(Ktor2.Client.library("jackson"))
    implementation("io.ktor:ktor-serialization-jackson:${Ktor2.version}")
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
    testImplementation(Ktor2.Server.library("test-host"))
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
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer::class.java)
}
