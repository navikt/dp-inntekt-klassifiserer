

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
    implementation("com.github.navikt:dp-inntekt:2020.05.18-11.33.279ab2f32a2c")

    // kafka
    implementation(Kafka.streams)

    // ktor
    implementation(Ktor.serverNetty)

    // ktor http client
    implementation(Dagpenger.Biblioteker.Ktor.Client.metrics)
    implementation("com.github.navikt.dp-biblioteker:oauth2-klient:2022.02.05-16.32.da1deab37b31")
    implementation(Ktor.library("client-auth-jvm"))
    implementation(Ktor.library("client-cio"))
    implementation(Ktor.library("client-core"))
    implementation(Ktor.library("client-logging-jvm"))
    implementation(Ktor.library("client-jackson"))
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
    testImplementation(Ktor.ktorTest) {
        exclude("org.jetbrains.kotlin", "kotlin-test-junit")
    }
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
