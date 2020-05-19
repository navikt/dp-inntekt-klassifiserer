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

    companion object {
        val log = KotlinLogging.logger {}
        val sikkerlogg = KotlinLogging.logger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandAll("@behov", listOf("Inntekt")) }
            validate { it.rejectKey("@løsning") }
            validate { it.requireKey("@id") }
            validate { it.requireKey("beregningsdato", "aktørId", "fødselsnummer", "vedtakId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val aktørId = packet["aktørId"].asText()
        val vedtakId = packet["vedtakId"].asText()
        val fødselsnummer = packet["fødselsnummer"].asText()
        val beregningsDato = packet["beregningsdato"].asLocalDate()

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
                    "Inntekt" to mapOf("id" to inntekt.inntektsId)
                )
                context.send(packet.toJson())
                log.info { "løser behov for ${packet["@id"].asText()}" }
            } catch (err: Exception) {
                log.error(err) { "feil ved innhenting av inntekt: ${err.message} for ${packet["@id"].asText()}" }
            }
        }
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        log.error { problems.toString() }
        sikkerlogg.error { problems.toExtendedReport() }
    }
}
