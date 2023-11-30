package no.nav.dagpenger.inntekt.klassifiserer

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import io.getunleash.DefaultUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import java.net.InetAddress

internal object Config {

    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "RAPID_APP_NAME" to "dp-inntekt-klassifiserer",
                "KAFKA_CONSUMER_GROUP_ID" to "dp-inntekt-klassifiserer-v1",
                "KAFKA_RAPID_TOPIC" to "teamdagpenger.regel.v1",
                "KAFKA_RESET_POLICY" to "latest",
                "UNLEASH_SERVER_API_URL" to "https://localhost:1234",
                "UNLEASH_SERVER_API_TOKEN" to "tøken",
                "DP_INNTEKT_API_KEY" to "dp-datalaster-inntekt",
                "DP_INNTEKT_API_SECRET" to "secret",
                "DP_INNTEKT_API_URL" to "HTTP://LOCALHOST/",
            ),
        )
    private val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val config: Map<String, String> =
        properties.list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }

    val oauth2Client: CachedOauth2Client by lazy {
        val azureAd = OAuth2Config.AzureAd(properties)
        CachedOauth2Client(
            tokenEndpointUrl = azureAd.tokenEndpointUrl,
            authType = azureAd.clientSecret(),
            httpClient =
            HttpClient {
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
            },
        )
    }

    val unleash: Unleash by lazy {
        DefaultUnleash(
            UnleashConfig.builder()
                .appName("dp-regel-grunnlag")
                .instanceId(runCatching { InetAddress.getLocalHost().hostName }.getOrElse { "ukjent" })
                .unleashAPI(properties[Key("UNLEASH_SERVER_API_URL", stringType)] + "/api/")
                .apiKey(properties[Key("UNLEASH_SERVER_API_TOKEN", stringType)])
                .environment(
                    when (System.getenv("NAIS_CLUSTER_NAME").orEmpty()) {
                        "prod-fss" -> "production"
                        else -> "development"
                    },
                ).build(),
        )
    }
}
