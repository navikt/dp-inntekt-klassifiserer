package no.nav.dagpenger.inntekt.klassifiserer

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate

class LøsningService(
    private val rapidsConnection: RapidsConnection,
    private val inntektKlassifiserer: InntektKlassifiserer
) : River.PacketListener {

    private val log = KotlinLogging.logger {}
    private val sikkerlogg = KotlinLogging.logger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate { it.demandAll("@behov", listOf(INNTEKT)) }
            validate { it.rejectKey("@løsning") }
            validate { it.requireKey("@id") }
            validate { it.requireKey(BEREGNINGSDATO) }
            validate { it.requireKey(AKTØRID) }
        }.register(this)
    }

    companion object {
        const val INNTEKT: String = "Inntekt"
        const val BEREGNINGSDATO: String = "beregningsdato"
        const val AKTØRID: String = "aktørId"
        private val logger = KotlinLogging.logger {}
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val aktørId = packet[AKTØRID].asText()
        val vedtakId = -1337 // @todo - gjøre inntektapi uavhengig av vedtak id
        val beregningsDato = packet[BEREGNINGSDATO].asLocalDate()

        try {
            val inntekt = inntektKlassifiserer.getInntekt(aktørId, vedtakId, beregningsDato)
            packet["@løsning"] = mapOf(
                INNTEKT to inntekt
            )
            context.send(packet.toJson())
            logger.info { "løser behov for ${packet["@id"].asText()}" }
        } catch (err: Exception) {
            logger.error(err) { "feil ved innhenting av inntekt: ${err.message} for ${packet["@id"].asText()}" }
        }
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        log.info { problems.toString() }
        sikkerlogg.info { problems.toExtendedReport() }
    }
}
