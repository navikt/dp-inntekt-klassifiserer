package no.nav.dagpenger.inntekt.klassifiserer

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.inntekt.klassifiserer.PacketParser.aktørId
import no.nav.dagpenger.inntekt.klassifiserer.PacketParser.beregningsdato
import no.nav.dagpenger.inntekt.klassifiserer.PacketParser.hentRegelkontekst
import no.nav.dagpenger.inntekt.klassifiserer.PacketParser.inntektsId
import no.nav.dagpenger.inntekt.klassifiserer.PacketParser.systemStarted
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger { }
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class InntektBehovløser(rapidsConnection: RapidsConnection, private val inntektClient: InntektHttpClient) :
    River.PacketListener {
    companion object {
        const val BEHOV_ID = "behovId"
        const val SYSTEM_STARTED = "system_started"
        const val INNTEKT = "inntektV1"
        const val AKTØRID = "aktørId"
        const val MANUELT_GRUNNLAG = "manueltGrunnlag"
        const val FORRIGE_GRUNNLAG = "forrigeGrunnlag"
        const val BEREGNINGSDATO = "beregningsDato"
        const val INNTEKT_ID = "inntektsId"
        const val KONTEKST_ID = "kontekstId"
        const val KONTEKST_TYPE = "kontekstType"
        const val PROBLEM = "system_problem"
        val rapidFilter: River.() -> Unit = {
            validate { it.interestedIn(BEHOV_ID) }
            validate {
                it.interestedIn(
                    INNTEKT_ID,
                    BEREGNINGSDATO,
                    AKTØRID,
                    SYSTEM_STARTED,
                    KONTEKST_ID,
                    KONTEKST_TYPE,
                )
            }
            validate { it.rejectKey(INNTEKT, MANUELT_GRUNNLAG, FORRIGE_GRUNNLAG) }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behovId: String? = packet[BEHOV_ID].asText()
        val callId = (behovId ?: UUID.randomUUID()).toString()
        val started: LocalDateTime? = packet.systemStarted()
        val inntektsId: String? = packet.inntektsId()
        val regelkontekst: RegelKontekst? = packet.hentRegelkontekst()
        withLoggingContext(
            "callId" to callId,
            "behovId" to behovId,
            "kontekstType" to regelkontekst?.type,
            "kontekstId" to regelkontekst?.id,
        ) {
            sikkerlogg.info { "Mottok pakke: ${packet.toJson()}" }
            if (started?.isBefore(LocalDateTime.now().minusSeconds(30)) == true) {
                throw RuntimeException("Denne pakka er for gammal!")
            }
            val klassifisertInntekt =
                when (inntektsId != null) {
                    true -> {
                        logger.info { "Henter inntekt basert på inntektsId: $inntektsId" }
                        runBlocking { inntektClient.getKlassifisertInntekt(inntektsId, callId) }
                    }

                    else -> {
                        val aktørId: String = packet.aktørId() ?: throw IllegalArgumentException("Mangler aktørId")
                        requireNotNull(regelkontekst) { "Må ha en kontekst for å hente inntekt" }

                        val beregningsdato: LocalDate =
                            packet.beregningsdato() ?: throw IllegalArgumentException("Mangler beregningsdato")
                        try {
                            runBlocking {
                                inntektClient.getKlassifisertInntekt(
                                    aktørId,
                                    regelkontekst,
                                    beregningsdato,
                                    null,
                                    callId,
                                )
                            }
                        } catch (e: InntektApiHttpClientException) {
                            logger.error(e) { "Kunne ikke hente inntekt fra dp-inntekt-api" }
                            packet[PROBLEM] = e.problem.toMap()
                            context.publish(packet.toJson())
                            return
                        }
                    }
                }
            logger.info { "Hentet med inntektsId: ${klassifisertInntekt.inntektsId}" }
            packet[INNTEKT] = klassifisertInntekt.toMap()
            context.publish(packet.toJson())
            sikkerlogg.info { "Sendte løsning: ${packet.toJson()}" }
        }
    }
}
