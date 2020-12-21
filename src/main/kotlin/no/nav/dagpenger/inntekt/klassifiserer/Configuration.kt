package no.nav.dagpenger.inntekt.klassifiserer

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.streams.Topic
import no.nav.dagpenger.streams.Topics

private const val TOPIC = "privat-dagpenger-behov-v2"

private val localProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "LOCAL",
        "application.httpPort" to "8080",
        "behov.topic" to Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
        "dp.inntekt.api.key" to "dp-datalaster-inntekt",
        "dp.inntekt.api.secret" to "secret",
        "dp.inntekt.api.url" to "http://localhost/",
        "inntekt.grpc.address" to "localhost",
        "kafka.bootstrap.servers" to "localhost:9092",
        "kafka.topic" to TOPIC,
        "kafka.reset.policy" to "earliest",
        "nav.truststore.path" to "",
        "nav.truststore.password" to "changeme",
        "srvdp.inntekt.klassifiserer.username" to "srvdp-inntekt-kl",
        "srvdp.inntekt.klassifiserer.password" to "srvdp-passord",
    )
)

private val devProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "DEV",
        "application.httpPort" to "8080",
        "behov.topic" to Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
        "dp.inntekt.api.url" to "http://dp-inntekt-api.teamdagpenger/",
        "inntekt.grpc.address" to "dp-inntekt-api-grpc.teamdagpenger.svc.nais.local",
        "kafka.bootstrap.servers" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443",
        "kafka.topic" to TOPIC,
        "kafka.reset.policy" to "earliest",

    )
)

private val prodProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "PROD",
        "application.httpPort" to "8080",
        "behov.topic" to Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
        "dp.inntekt.api.url" to "http://dp-inntekt-api.teamdagpenger/",
        "inntekt.grpc.address" to "dp-inntekt-api-grpc.teamdagpenger.svc.nais.local",
        "kafka.bootstrap.servers" to "a01apvl00145.adeo.no:8443,a01apvl00146.adeo.no:8443,a01apvl00147.adeo.no:8443,a01apvl00149.adeo.no:8443",
        "kafka.topic" to TOPIC,
        "kafka.reset.policy" to "earliest",
    )
)

data class Configuration(
    val applicationConfig: ApplicationConfig = ApplicationConfig(),
    val kafka: Kafka = Kafka(),
    val rapidApplication: Map<String, String> = mapOf(
        "RAPID_APP_NAME" to applicationConfig.id,
        "KAFKA_BOOTSTRAP_SERVERS" to config()[Key("kafka.bootstrap.servers", stringType)],
        "KAFKA_CONSUMER_GROUP_ID" to "dp-inntekt-klassifiserer-rapid",
        "KAFKA_RAPID_TOPIC" to config()[Key("kafka.topic", stringType)],
        "KAFKA_RESET_POLICY" to config()[Key("kafka.reset.policy", stringType)],
        "NAV_TRUSTSTORE_PATH" to config()[Key("nav.truststore.path", stringType)],
        "NAV_TRUSTSTORE_PASSWORD" to config()[Key("nav.truststore.password", stringType)],
        "HTTP_PORT" to "8088" // @todo - to avoid port clash with dagpenger River
    ) + System.getenv().filter { it.key.startsWith("NAIS_") }
)

data class Kafka(
    val bootstrapServer: String = config()[Key("kafka.bootstrap.servers", stringType)],
    val username: String = config()[Key("srvdp.inntekt.klassifiserer.username", stringType)],
    val password: String = config()[Key("srvdp.inntekt.klassifiserer.password", stringType)],
    val behovTopic: Topic<String, Packet> = Topics.DAGPENGER_BEHOV_PACKET_EVENT.copy(
        name = config()[Key("behov.topic", stringType)]
    )
)

data class ApplicationConfig(
    val id: String = config().getOrElse(Key("application.id", stringType), "dp-inntekt-klassifiserer"),
    val httpPort: Int = config()[Key("application.httpPort", intType)],
    val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
    val inntektApiUrl: String = config()[Key("dp.inntekt.api.url", stringType)],
    val inntektApiKey: String = config()[Key("dp.inntekt.api.key", stringType)],
    val inntektApiSecret: String = config()[Key("dp.inntekt.api.secret", stringType)],
    val inntektGrpcAddress: String = config()[Key("inntekt.grpc.address", stringType)]
)

enum class Profile {
    LOCAL, DEV, PROD
}

private fun config() = when (getEnvOrProp("NAIS_CLUSTER_NAME")) {
    "dev-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties
    "prod-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding prodProperties
    else -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties
}

fun getEnvOrProp(propName: String): String? {
    return System.getenv(propName) ?: System.getProperty(propName)
}
