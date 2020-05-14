package no.nav.dagpenger.inntekt.klassifiserer

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate

class LøsningService(
    private val rapidsConnection: RapidsConnection,
    private val inntektHttpClient: InntektHttpClient
) : River.PacketListener {

    private val log = KotlinLogging.logger {}
    private val sikkerlogg = KotlinLogging.logger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate { it.demandAll("@behov", listOf(INNTEKT)) }
            validate { it.rejectKey("@løsning") }
            validate { it.requireKey("@id", BEREGNINGSDATO, AKTØRID, FØDSELSNUNMER, VEDTAK_ID) }
        }.register(this)
    }

    companion object {
        const val INNTEKT: String = "Inntekt"
        const val BEREGNINGSDATO: String = "beregningsdato"
        const val AKTØRID: String = "aktørId"
        const val FØDSELSNUNMER: String = "fødselsnummer"
        const val VEDTAK_ID: String = "vedtakId"
        private val logger = KotlinLogging.logger {}
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val aktørId = packet[AKTØRID].asText()
        val vedtakId = packet[VEDTAK_ID].asText()
        val fødselsnummer = packet[FØDSELSNUNMER].asText()
        val beregningsDato = packet[BEREGNINGSDATO].asLocalDate()

        withLoggingContext(
            "behovId" to packet["@id"].asText(),
            "vedtakId" to packet["vedtakId"].asText()
        ) {
            try {
                val inntekt = inntektHttpClient.getKlassifisertInntekt(
                    aktørId = aktørId,
                    vedtakId = vedtakId,
                    beregningsDato = beregningsDato,
                    fødselsnummer = fødselsnummer
                )
                packet["@løsning"] = mapOf(
                    INNTEKT to inntekt.inntektsId
                )
                context.send(packet.toJson())
                logger.info { "løser behov for ${packet["@id"].asText()}" }
            } catch (err: Exception) {
                logger.error(err) { "feil ved innhenting av inntekt: ${err.message} for ${packet["@id"].asText()}" }
            }
        }
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        log.info { problems.toString() }
        sikkerlogg.info { problems.toExtendedReport() }
    }
}
