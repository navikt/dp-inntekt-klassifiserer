package no.nav.dagpenger.inntekt.klassifiserer

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import no.nav.dagpenger.streams.PacketDeserializer
import no.nav.dagpenger.streams.PacketSerializer
import no.nav.dagpenger.streams.Topic
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.common.serialization.Serdes

private val localProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "LOCAL",
        "application.httpPort" to "8080",
        "behov.topic" to Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
        "dp.inntekt.api.key" to "dp-datalaster-inntekt",
        "dp.inntekt.api.secret" to "secret",
        "dp.inntekt.api.url" to "http://localhost/",
        "inntekt.grpc.address" to "localhost",
        "KAFKA_BROKERS" to "localhost:9092",
        "kafka.reset.policy" to "earliest"
    )
)

private val devProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "DEV",
        "application.httpPort" to "8080",
        "dp.inntekt.api.url" to "http://dp-inntekt-api.teamdagpenger/",
        "inntekt.grpc.address" to "dp-inntekt-api-grpc.teamdagpenger.svc.nais.local",
        "kafka.bootstrap.servers" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443",
        "kafka.reset.policy" to "earliest",
    )
)

private val prodProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "PROD",
        "application.httpPort" to "8080",
        "dp.inntekt.api.url" to "http://dp-inntekt-api.teamdagpenger/",
        "inntekt.grpc.address" to "dp-inntekt-api-grpc.teamdagpenger.svc.nais.local",
        "kafka.reset.policy" to "earliest",
    )
)

object Configuration {
    val applicationConfig: ApplicationConfig = ApplicationConfig()
    val kafka: Kafka = Kafka()

    val oauth2Client: CachedOauth2Client by lazy {
        val azureAd = OAuth2Config.AzureAd(config())
        CachedOauth2Client(
            tokenEndpointUrl = azureAd.tokenEndpointUrl,
            authType = azureAd.clientSecret(),
            httpClient = HttpClient() {
                install(ContentNegotiation) {
                    jackson {
                        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    }
                }
                engine {
                    System.getenv("HTTP_PROXY")?.let {
                        this.proxy = ProxyBuilder.http(it)
                    }
                }
            }
        )
    }

    val dpInntektApiScope by lazy { config()[Key("DP_INNTEKT_API_SCOPE", stringType)] }
}

data class Kafka(
    val aivenBrokers: String = config()[Key("KAFKA_BROKERS", stringType)],
    val regelTopic: Topic<String, Packet> = Topic(
        name = "teamdagpenger.regel.v1",
        keySerde = Serdes.String(),
        valueSerde = Serdes.serdeFrom(PacketSerializer(), PacketDeserializer())
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
