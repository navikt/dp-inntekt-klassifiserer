package no.nav.dagpenger.inntekt.klassifiserer

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import no.nav.dagpenger.events.inntekt.v1.Avvik
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.events.inntekt.v1.SpesifisertInntekt

class InntektKlassifiserer(private val inntektHttpClient: SpesifisertInntektHttpClient) {
    fun getInntekt(
        aktørId: String,
        vedtakId: Int,
        beregningsDato: LocalDate
    ): Inntekt = klassifiserOgMapInntekt(inntektHttpClient.getSpesifisertInntekt(aktørId = aktørId, vedtakId = vedtakId, beregningsDato = beregningsDato))
}

internal fun klassifiserOgMapInntekt(spesifisertInntekt: SpesifisertInntekt): Inntekt {
    val klassifisertePosteringer = klassifiserPosteringerMedHyreendring(spesifisertInntekt.posteringer)

    val avvikMåneder = spesifisertInntekt.avvik.groupBy { it.avvikPeriode }

    val klassifisertInntektMåneder = mapTilKlassifisertInntektMåneder(klassifisertePosteringer, avvikMåneder)

    return Inntekt(
        inntektsId = spesifisertInntekt.inntektId.id,
        inntektsListe = klassifisertInntektMåneder,
        manueltRedigert = spesifisertInntekt.manueltRedigert,
        sisteAvsluttendeKalenderMåned = spesifisertInntekt.sisteAvsluttendeKalenderMåned
    )
}

private fun mapTilKlassifisertInntektMåneder(klassifisertePosteringer: List<KlassifisertPostering>, avvikMåneder: Map<YearMonth, List<Avvik>>) =
    klassifisertePosteringer
        .groupBy { klassifisertPostering -> klassifisertPostering.postering.posteringsMåned }
        .map { (årmåned, klassifisertePosteringerForMåned) ->
            mapTilKlassifisertInntektMåned(årmåned, klassifisertePosteringerForMåned, avvikMåneder)
        }

private fun mapTilKlassifisertInntektMåned(
    årmåned: YearMonth,
    klassifisertePosteringer: List<KlassifisertPostering>,
    avvikMåneder: Map<YearMonth, List<Avvik>>
) = KlassifisertInntektMåned(
    årMåned = årmåned,
    harAvvik = avvikMåneder.containsKey(årmåned),
    klassifiserteInntekter = klassifisertePosteringer
        .groupBy { (_, inntektKlasse) -> inntektKlasse }
        .map { (klasse, klassifisertePosteringerForKlasse) ->
            KlassifisertInntekt(
                beløp = klassifisertePosteringerForKlasse.fold(BigDecimal.ZERO) { sum, postering -> sum + postering.postering.beløp },
                inntektKlasse = klasse
            )
        }
)
