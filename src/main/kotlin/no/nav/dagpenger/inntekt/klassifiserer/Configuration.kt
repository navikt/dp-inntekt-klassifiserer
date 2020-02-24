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

private val localProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "LOCAL",
        "application.httpPort" to "8080",
        "kafka.bootstrapServer" to "localhost:9092",
        "srvdp.inntekt.klassifiserer.username" to "srvdp-inntekt-kl",
        "srvdp.inntekt.klassifiserer.password" to "srvdp-passord",
        "dp.inntekt.api.key" to "dp-datalaster-inntekt",
        "dp.inntekt.api.secret" to "secret",
        "dp.inntekt.api.url" to "http://localhost/",
        "behov.topic" to Topics.DAGPENGER_BEHOV_PACKET_EVENT.name
    )
)

private val devProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "DEV",
        "application.httpPort" to "8080",
        "kafka.bootstrapServer" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443",
        "dp.inntekt.api.url" to "http://dp-inntekt-api//",
        "behov.topic" to Topics.DAGPENGER_BEHOV_PACKET_EVENT.name
    )
)

private val prodProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "PROD",
        "application.httpPort" to "8080",
        "dp.inntekt.api.url" to "http://dp-inntekt-api/",
        "kafka.bootstrapServer" to "a01apvl00145.adeo.no:8443,a01apvl00146.adeo.no:8443,a01apvl00147.adeo.no:8443,a01apvl00149.adeo.no:8443",
        "behov.topic" to Topics.DAGPENGER_BEHOV_PACKET_EVENT.name
    )
)

data class Configuration(
    val application: Application = Application(),
    val kafka: Kafka = Kafka()
)

data class Kafka(
    val bootstrapServer: String = config()[Key("kafka.bootstrapServer", stringType)],
    val username: String = config()[Key("srvdp.inntekt.klassifiserer.username", stringType)],
    val password: String = config()[Key("srvdp.inntekt.klassifiserer.password", stringType)],
    val behovTopic: Topic<String, Packet> = Topics.DAGPENGER_BEHOV_PACKET_EVENT.copy(
        name = config()[Key("behov.topic", stringType)]
    )
)

data class Application(
    val id: String = config().getOrElse(Key("application.id", stringType), "dp-inntekt-klassifiserer"),
    val httpPort: Int = config()[Key("application.httpPort", intType)],
    val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
    val inntektApiUrl: String = config()[Key("dp.inntekt.api.url", stringType)],
    val inntektApiKey: String = config()[Key("dp.inntekt.api.key", stringType)],
    val inntektApiSecret: String = config()[Key("dp.inntekt.api.secret", stringType)]
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
