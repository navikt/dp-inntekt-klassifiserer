package no.nav.dagpenger.inntekt.klassifiserer

import no.nav.dagpenger.events.inntekt.v1.Avvik
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.events.inntekt.v1.Postering
import no.nav.dagpenger.events.inntekt.v1.SpesifisertInntekt
import java.math.BigDecimal
import java.time.YearMonth

fun mapToKlassifisertInntekt(spesifisertInntekt: SpesifisertInntekt) = Inntekt(
    inntektsId = spesifisertInntekt.inntektId.id,
    inntektsListe = getKlassifisertInntektsListe(spesifisertInntekt),
    manueltRedigert = spesifisertInntekt.manueltRedigert,
    sisteAvsluttendeKalenderMåned = spesifisertInntekt.sisteAvsluttendeKalenderMåned
)

fun getKlassifisertInntektsListe(spesifisertInntekt: SpesifisertInntekt) =
    klassifiserInntekt(spesifisertInntekt).flatten()

fun klassifiserInntekt(spesifisertInntekt: SpesifisertInntekt) =
    spesifisertInntekt.posteringer
        .groupBy(Postering::posteringsMåned)
        .map { (årmåned, posteringer) ->
            mapKlassifisertPosteringTilKlassifisertInntektMåned(
                årmåned,
                posteringer,
                getAvvikPerMåned(spesifisertInntekt)
            )
        }

fun getAvvikPerMåned(spesifisertInntekt: SpesifisertInntekt) =
    spesifisertInntekt.avvik.groupBy(Avvik::avvikPeriode)

fun mapKlassifisertPosteringTilKlassifisertInntektMåned(
    årmåned: YearMonth,
    posteringer: List<Postering>,
    avvikMåneder: Map<YearMonth, List<Avvik>>
) =
    posteringer.groupBy { klassifiserPostering(it.posteringsType) }
        .map { (klasse, posteringer) ->
            KlassifisertInntektMåned(
                årMåned = årmåned,
                klassifiserteInntekter = listOf(
                    KlassifisertInntekt(
                        beløp = sumPosteringsBeløp(posteringer),
                        inntektKlasse = klasse
                    )
                ),
                harAvvik = avvikMåneder.containsKey(årmåned)
            )
        }

fun sumPosteringsBeløp(posteringer: List<Postering>): BigDecimal {
    return posteringer.fold(BigDecimal.ZERO) { sum, postering -> sum + postering.beløp }
}

class KlassifiseringsException(message: String) : RuntimeException(message)
