package no.nav.dagpenger.inntekt.klassifiserer

import java.time.LocalDateTime
import java.util.Properties
import no.nav.dagpenger.events.Packet
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
    private val inntektKlassifiserer: InntektKlassifiserer,
    private val healthCheck: HealthCheck
) : River(configuration.kafka.behovTopic) {

    override val healthChecks: List<HealthCheck> = listOf(healthCheck)
    override val SERVICE_APP_ID: String = configuration.applicationConfig.id

    companion object {
        const val INNTEKT = "inntektV1"
        const val AKTØRID = "aktørId"
        const val VEDTAKID = "vedtakId"
        const val MANUELT_GRUNNLAG = "manueltGrunnlag"
        const val BEREGNINGSDATO = "beregningsDato"
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

        if (started?.isBefore(LocalDateTime.now().minusSeconds(30)) == true) {
            throw RuntimeException("Denne pakka er for gammal!")
        } else {
            val aktørId = packet.getStringValue(AKTØRID)
            val vedtakId = packet.getIntValue(VEDTAKID)
            val beregningsDato = packet.getLocalDate(BEREGNINGSDATO)
            val klassifisertInntekt = inntektKlassifiserer.getInntekt(aktørId, vedtakId, beregningsDato)
            packet.putValue(INNTEKT, klassifisertInntekt)
            return packet
        }
    }
}

fun main() {
    val configuration = Configuration()

    val unleash = setupUnleash(configuration.applicationConfig.unleashUrl)

    val apiKeyVerifier = ApiKeyVerifier(configuration.applicationConfig.inntektApiSecret)
    val apiKey = apiKeyVerifier.generate(configuration.applicationConfig.inntektApiKey)

    val inntektKlassifiserer = InntektKlassifiserer(
        inntektHttpClient = SpesifisertInntektHttpClient(
            configuration.applicationConfig.inntektApiUrl,
            apiKey
        ))

    val application = Application(configuration = configuration, inntektKlassifiserer = inntektKlassifiserer, healthCheck = RapidHealthCheck as HealthCheck)
    RapidApplication.create(
        Configuration().rapidApplication
    ).apply {
        LøsningService(
            this,
            inntektKlassifiserer = inntektKlassifiserer
        )
    }.also {
        it.register(RapidHealthCheck)
    }.start()

    application.start()
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
        false -> HealthStatus.UP
    }
}
