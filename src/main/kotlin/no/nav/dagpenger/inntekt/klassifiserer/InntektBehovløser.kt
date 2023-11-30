package no.nav.dagpenger.inntekt.klassifiserer

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

class InntektBehovløser(rapidsConnection: RapidsConnection) : River.PacketListener {
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
        val rapidFilter: River.() -> Unit = {
            validate { it.interestedIn(BEHOV_ID) }
            validate { it.interestedIn(INNTEKT_ID, BEREGNINGSDATO, AKTØRID, SYSTEM_STARTED, KONTEKST_ID, KONTEKST_TYPE) }
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
        val beregningsdato: LocalDate? = packet.beregningsdato()
        val inntektsId: String? = packet.inntektsId()
        val aktørId: String? = packet.aktørId()
        val regelkontekst: RegelKontekst? = packet.hentRegelkontekst()
    }
}
