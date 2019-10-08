package no.nav.dagpenger.inntekt.klassifiserer

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.River
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.kstream.Predicate
import java.util.Properties

private val LOGGER = KotlinLogging.logger {}

class App(private val configuration: Configuration) : River() {

    override val SERVICE_APP_ID: String = "dp-inntekt-klassifiserer"

    companion object {
        const val SPESIFISERT_INNTEKT = "spesifisertInntektV1"
        const val INNTEKT = "inntektV1"
    }

    override fun getConfig(): Properties {
        return streamConfig(
            SERVICE_APP_ID, configuration.kafka.bootstrapServer,
            KafkaCredential(configuration.application.username, configuration.application.password)
        )
    }

    override fun filterPredicates(): List<Predicate<String, Packet>> {
        return listOf(
            Predicate { _, packet -> packet.hasField(SPESIFISERT_INNTEKT) },
            Predicate { _, packet -> !packet.hasField(INNTEKT) }
        )
    }

    override fun onPacket(packet: Packet): Packet {
        val spesifisertInntekt = packet.getObjectValue(SPESIFISERT_INNTEKT) { serialized ->
            checkNotNull(spesifisertInntektJsonAdapter.fromJsonValue(serialized))
        }

        val klassifisertInntekt = klassifiserOgMapInntekt(spesifisertInntekt)

        packet.putValue(INNTEKT, klassifisertInntekt)

        return packet
    }
}

fun main() {
    val configuration = Configuration()
    val inntektKlassifiserer = App(configuration)
    inntektKlassifiserer.start()
}
