package no.nav.dagpenger.inntekt.klassifiserer

import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.AKTØRID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.BEHOV_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.BEREGNINGSDATO
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.INNTEKT_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.KONTEKST_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.KONTEKST_TYPE
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull

object PacketParser {
    fun JsonMessage.beregningsdato() =
        when (this.harVerdi(BEREGNINGSDATO)) {
            true -> this[BEREGNINGSDATO].asLocalDate()
            false -> null
        }

    fun JsonMessage.inntektsId() =
        when (this.harVerdi(INNTEKT_ID)) {
            true -> this[INNTEKT_ID].asText()
            false -> null
        }

    fun JsonMessage.aktørId() =
        when (this.harVerdi(AKTØRID)) {
            true -> this[AKTØRID].asText()
            false -> null
        }

    fun JsonMessage.behovId() =
        when (this.harVerdi(BEHOV_ID)) {
            true -> this[BEHOV_ID].asText()
            false -> null
        }

    fun JsonMessage.hentRegelkontekst(): RegelKontekst? {
        val kontekstId: String? =
            when (this.harVerdi(KONTEKST_ID)) {
                true -> this[KONTEKST_ID].asText()
                false -> null
            }
        val kontekstType: String? =
            when (this.harVerdi(KONTEKST_TYPE)) {
                true -> this[KONTEKST_TYPE].asText()
                false -> null
            }
        return if (kontekstId != null && kontekstType != null) {
            RegelKontekst(id = kontekstId, type = kontekstType)
        } else {
            null
        }
    }

    private fun JsonMessage.harVerdi(field: String) = !this[field].isMissingOrNull()
}
