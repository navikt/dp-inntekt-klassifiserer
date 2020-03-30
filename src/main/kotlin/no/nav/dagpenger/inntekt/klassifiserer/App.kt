package no.nav.dagpenger.inntekt.klassifiserer

import mu.KotlinLogging
import no.finn.unleash.Unleash
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.ktor.auth.ApiKeyVerifier
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.River
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.kstream.Predicate
import java.lang.RuntimeException
import java.time.LocalDateTime
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class App(
    private val configuration: Configuration,
    private val spesifisertInntektHttpClient: SpesifisertInntektHttpClient,
    private val unleash: Unleash
) : River(configuration.kafka.behovTopic) {

    override val SERVICE_APP_ID: String = configuration.application.id

    companion object {
        const val SPESIFISERT_INNTEKT = "spesifisertInntektV1"
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
            val spesifisertInntekt = spesifisertInntektHttpClient.getSpesifisertInntekt(aktørId, vedtakId, beregningsDato)

            val klassifisertInntekt = klassifiserOgMapInntekt(spesifisertInntekt, unleash)

            packet.putValue(INNTEKT, klassifisertInntekt)

            return packet
        }
    }
}

fun main() {
    val configuration = Configuration()

    val unleash = setupUnleash(configuration.application.unleashUrl)

    val apiKeyVerifier = ApiKeyVerifier(configuration.application.inntektApiSecret)
    val apiKey = apiKeyVerifier.generate(configuration.application.inntektApiKey)

    val spesifisertInntektHttpClient = SpesifisertInntektHttpClient(
        configuration.application.inntektApiUrl,
        apiKey
    )

    val inntektKlassifiserer = App(configuration, spesifisertInntektHttpClient, unleash)
    inntektKlassifiserer.start()
}
