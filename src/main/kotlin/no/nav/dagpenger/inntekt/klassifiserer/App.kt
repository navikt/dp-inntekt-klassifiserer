package no.nav.dagpenger.inntekt.klassifiserer

import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.streams.River
import org.apache.kafka.streams.kstream.Predicate

private val LOGGER = KotlinLogging.logger {}

class App : River() {

    override val SERVICE_APP_ID: String = "dagpenger-inntekt-datasamler"

    companion object {
        const val SPESIFISERT_INNTEKT = "spesifisertInntektV1"
        const val INNTEKT = "inntektV1"
    }

    override fun filterPredicates(): List<Predicate<String, Packet>> {
        return listOf(
            Predicate { _, packet -> packet.hasField(SPESIFISERT_INNTEKT) },
            Predicate { _, packet -> !packet.hasField(INNTEKT) }
        )
    }

    override fun onPacket(packet: Packet): Packet {
        val spesifisertInntekt = packet.getObjectValue(SPESIFISERT_INNTEKT) {
                serialized -> checkNotNull(spesifisertInntektJsonAdapter.fromJsonValue(serialized))
        }

        val klassifisertInntekt = klassifiserInntekt(spesifisertInntekt)
        packet.putValue(INNTEKT, klassifisertInntekt)

        return packet
    }
}

fun main() {
    val configuration = Configuration()
    val inntektKlassifiserer = App()
}
