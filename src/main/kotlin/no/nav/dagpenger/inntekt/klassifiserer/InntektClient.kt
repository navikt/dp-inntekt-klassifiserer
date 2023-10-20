package no.nav.dagpenger.inntekt.klassifiserer

import io.getunleash.Unleash
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.inntekt.rpc.InntektHenter
import java.time.LocalDate

internal class InntektClient(
    private val httpClient: InntektHttpClient,
    private val inntektHenter: InntektHenter,
    private val unleash: Unleash,
) {
    suspend fun getKlassifisertInntekt(
        aktørId: String,
        regelkontekst: RegelKontekst,
        beregningsDato: LocalDate,
        fødselsnummer: String?,
        callId: String? = null,
    ): Inntekt {
        return httpClient.getKlassifisertInntekt(
            aktørId,
            regelkontekst,
            beregningsDato,
            fødselsnummer,
            callId,
        )
    }

    suspend fun getKlassifisertInntekt(
        inntektId: String,
        callId: String,
    ): Inntekt {
        return if (unleash.isEnabled("dp-inntekt-klassifiserer.http-client")) {
            httpClient.getKlassifisertInntekt(inntektId, callId)
        } else {
            inntektHenter.hentKlassifisertInntekt(inntektId)
        }
    }
}
