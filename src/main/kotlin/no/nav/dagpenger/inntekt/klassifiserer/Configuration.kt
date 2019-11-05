package no.nav.dagpenger.inntekt.klassifiserer

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

private val localProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "LOCAL",
        "application.httpPort" to "8080",
        "kafka.bootstrapServer" to "localhost:9092",
        "srvdp.inntekt.klassifiserer.username" to "srvdp-inntekt-kl",
        "srvdp.inntekt.klassifiserer.password" to "srvdp-passord"
    )
)

private val devProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "DEV",
        "application.httpPort" to "8080",
        "kafka.bootstrapServer" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443"
    )
)

private val prodProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "PROD",
        "application.httpPort" to "8080",
        "kafka.bootstrapServer" to "a01apvl00145.adeo.no:8443,a01apvl00146.adeo.no:8443,a01apvl00147.adeo.no:8443,a01apvl00149.adeo.no:8443"
    )
)

data class Configuration(
    val application: Application = Application(),
    val kafka: Kafka = Kafka()
)

data class Kafka(
    val bootstrapServer: String = config()[Key("kafka.bootstrapServer", stringType)],
    val username: String = config()[Key("srvdp.inntekt.klassifiserer.username", stringType)],
    val password: String = config()[Key("srvdp.inntekt.klassifiserer.password", stringType)]
)

data class Application(
    val httpPort: Int = config()[Key("application.httpPort", intType)],
    val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) }
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
