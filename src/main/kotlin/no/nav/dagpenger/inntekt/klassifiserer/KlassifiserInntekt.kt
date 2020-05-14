package no.nav.dagpenger.inntekt.klassifiserer

import com.github.rawls238.scientist4j.Experiment
import com.github.rawls238.scientist4j.metrics.MicrometerMetricsProvider
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.CollectorRegistry
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.BiFunction
import mu.KotlinLogging
import no.nav.dagpenger.events.inntekt.v1.Avvik
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.events.inntekt.v1.SpesifisertInntekt

class InntektKlassifiserer(private val inntektHttpClient: InntektHttpClient) {

    companion object {
        private fun toString(inntekt: Inntekt) = """ ${inntekt.inntektsId} 
        | ${inntekt.inntektsListe} 
        | ${inntekt.sisteAvsluttendeKalenderMåned}
        | ${inntekt.manueltRedigert}
    """.trimMargin()

        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
        private val executorService: ExecutorService = Executors.newFixedThreadPool(5)
        private val comparator = BiFunction<Inntekt, Inntekt, Boolean> { control: Inntekt, candidate: Inntekt ->
            val match: Boolean = control.manueltRedigert == candidate.manueltRedigert &&
                control.inntektsId == candidate.inntektsId &&
                control.inntektsListe == candidate.inntektsListe &&
                control.sisteAvsluttendeKalenderMåned == candidate.sisteAvsluttendeKalenderMåned

            if (!match) {
                sikkerlogg.info {
                    """
                       Match $match 
                       Control ${toString(control)} do not match
                       Candidate ${toString(candidate)}
                   """.trimIndent()
                }
            }

            match
        }

        val experiment: Experiment<Inntekt> = Experiment(
            "klassifiserer",
            emptyMap(),
            false,
            MicrometerMetricsProvider(PrometheusMeterRegistry(PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM)),
            comparator,
            executorService
        )
    }

    fun getInntekt(
        aktørId: String,
        vedtakId: String,
        beregningsDato: LocalDate,
        fødselsnummer: String? = null
    ): Inntekt {
        return klassifiserOgMapInntekt(
            inntektHttpClient.getSpesifisertInntekt(
                aktørId = aktørId,
                vedtakId = vedtakId,
                beregningsDato = beregningsDato,
                fødselsnummer = fødselsnummer
            )
        )
        // return experiment.runAsync(
        //     {
        //         klassifiserOgMapInntekt(
        //             inntektHttpClient.getSpesifisertInntekt(
        //                 aktørId = aktørId,
        //                 vedtakId = vedtakId,
        //                 beregningsDato = beregningsDato,
        //                 fødselsnummer = fødselsnummer
        //             )
        //         )
        //     },
        //     {
        //         inntektHttpClient.getKlassifisertInntekt(
        //             aktørId = aktørId,
        //             vedtakId = vedtakId,
        //             beregningsDato = beregningsDato,
        //             fødselsnummer = fødselsnummer
        //         )
        //     }
        //
        // )
    }
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
