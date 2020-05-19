package no.nav.dagpenger.inntekt.klassifiserer

import java.time.LocalDateTime
import java.util.Properties
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.inntekt.rpc.InntektHenter
import no.nav.dagpenger.inntekt.rpc.InntektHenterWrapper
import no.nav.dagpenger.ktor.auth.ApiKeyVerifier
import no.nav.dagpenger.streams.HealthCheck
import no.nav.dagpenger.streams.HealthStatus
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.River
import no.nav.dagpenger.streams.streamConfig
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.streams.kstream.Predicate

class Application(
    private val configuration: Configuration,
    private val inntektHttpClient: InntektHttpClient,
    private val healthCheck: HealthCheck,
    private val inntektHenter: InntektHenter
) : River(configuration.kafka.behovTopic) {

    override val healthChecks: List<HealthCheck> = listOf(healthCheck)
    override val SERVICE_APP_ID: String = configuration.applicationConfig.id

    companion object {
        const val INNTEKT = "inntektV1"
        const val AKTØRID = "aktørId"
        const val VEDTAKID = "vedtakId"
        const val MANUELT_GRUNNLAG = "manueltGrunnlag"
        const val BEREGNINGSDATO = "beregningsDato"
        const val INNTEKTS_ID = "inntektsId"
    }

    override fun getConfig(): Properties {
        return streamConfig(
            SERVICE_APP_ID,
            configuration.kafka.bootstrapServer,
            KafkaCredential(configuration.kafka.username, configuration.kafka.password)
        )
    }

    override fun filterPredicates(): List<Predicate<String, Packet>> {
        return listOf(
            Predicate { _, packet -> !packet.hasField(INNTEKT) },
            Predicate { _, packet -> !packet.hasField(MANUELT_GRUNNLAG) }
        )
    }

    override fun onPacket(packet: Packet): Packet {
        val started: LocalDateTime? =
            packet.getNullableStringValue("system_started")
                ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

        val inntektsId = packet.getNullableStringValue(INNTEKTS_ID)
        if (started?.isBefore(LocalDateTime.now().minusSeconds(30)) == true) {
            throw RuntimeException("Denne pakka er for gammal!")
        } else {

            val klassifisertInntekt = when (inntektsId) {
                is String -> runBlocking { inntektHenter.hentKlassifisertInntekt(inntektsId) }
                else -> {
                    val aktørId = packet.getStringValue(AKTØRID)
                    val vedtakId = packet.getIntValue(VEDTAKID)
                    val beregningsDato = packet.getLocalDate(BEREGNINGSDATO)
                    inntektHttpClient.getKlassifisertInntekt(aktørId, vedtakId.toString(), beregningsDato, null)
                }
            }

            packet.putValue(INNTEKT, klassifisertInntekt)
            return packet
        }
    }
}

fun main() {
    val configuration = Configuration()

    val apiKeyVerifier = ApiKeyVerifier(configuration.applicationConfig.inntektApiSecret)
    val apiKey = apiKeyVerifier.generate(configuration.applicationConfig.inntektApiKey)

    val inntektGrpcClient = InntektHenterWrapper(
        serveraddress = configuration.applicationConfig.inntektGrpcAddress,
        apiKey = apiKey
    )

    Runtime.getRuntime().addShutdownHook(Thread {
        inntektGrpcClient.close()
    })

    val inntektHttpClient = InntektHttpClient(
            configuration.applicationConfig.inntektApiUrl,
            apiKey
        )

    Application(
        configuration = configuration,
        inntektHttpClient = inntektHttpClient,
        inntektHenter = inntektGrpcClient,
        healthCheck = RapidHealthCheck as HealthCheck
    ).start()

    RapidApplication.create(
        Configuration().rapidApplication
    ).apply {
        LøsningService(
            this,
            inntektHttpClient = inntektHttpClient
        )
    }.also {
        it.register(RapidHealthCheck)
    }.start()
}

object RapidHealthCheck : RapidsConnection.StatusListener, HealthCheck {
    var healthy: Boolean = false

    override fun onStartup(rapidsConnection: RapidsConnection) {
        healthy = true
    }

    override fun onReady(rapidsConnection: RapidsConnection) {
        healthy = true
    }

    override fun onNotReady(rapidsConnection: RapidsConnection) {
        healthy = false
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        healthy = false
    }

    override fun status(): HealthStatus = when (healthy) {
        true -> HealthStatus.UP
        false -> HealthStatus.DOWN
    }
}
