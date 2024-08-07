package no.nav.dagpenger.inntekt.klassifiserer

import no.nav.dagpenger.inntekt.v1.Inntekt
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned

private fun List<KlassifisertInntektMåned>.toMap(): List<Map<String, Any>> =
    this.map { klassifisertInntektMåned ->
        val harAvvik: Boolean = klassifisertInntektMåned.harAvvik ?: false
        mapOf(
            "årMåned" to "${klassifisertInntektMåned.årMåned}",
            "klassifiserteInntekter" to
                klassifisertInntektMåned.klassifiserteInntekter.map {
                    mapOf<String, Any>(
                        "beløp" to "${it.beløp}",
                        "inntektKlasse" to it.inntektKlasse.name,
                    )
                },
            "harAvvik" to harAvvik,
        )
    }

internal fun Inntekt.toMap(): Map<String, Any> {
    val manueltRedigert: Boolean = this.manueltRedigert ?: false
    return mapOf(
        "inntektsId" to inntektsId,
        "inntektsListe" to inntektsListe.toMap(),
        "manueltRedigert" to manueltRedigert,
        "sisteAvsluttendeKalenderMåned" to "$sisteAvsluttendeKalenderMåned",
    )
}
