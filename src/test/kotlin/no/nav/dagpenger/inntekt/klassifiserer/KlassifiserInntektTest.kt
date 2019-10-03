package no.nav.dagpenger.inntekt.klassifiserer

import no.nav.dagpenger.events.inntekt.v1.Aktør
import no.nav.dagpenger.events.inntekt.v1.AktørType
import no.nav.dagpenger.events.inntekt.v1.Avvik
import no.nav.dagpenger.events.inntekt.v1.InntektId
import no.nav.dagpenger.events.inntekt.v1.InntektKlasse
import no.nav.dagpenger.events.inntekt.v1.Postering
import no.nav.dagpenger.events.inntekt.v1.PosteringsType
import no.nav.dagpenger.events.inntekt.v1.SpesifisertInntekt
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class klassifiserOgMapInntektTest {

    @Test
    fun `Klassifiserer tom inntekt`() {
        val spesifisertInntekt = SpesifisertInntekt(
            inntektId = InntektId("01DMNAADXVEZXGJRQJTZ6DWWNV"),
            avvik = emptyList(),
            posteringer = emptyList(),
            ident = Aktør(AktørType.AKTOER_ID, "12345"),
            manueltRedigert = false,
            timestamp = LocalDateTime.of(2019, 3, 5, 1, 1),
            sisteAvsluttendeKalenderMåned = YearMonth.of(2019, 4)
        )

        val klassifisertInntekt = klassifiserOgMapInntekt(spesifisertInntekt)

        assertEquals(spesifisertInntekt.inntektId.id, klassifisertInntekt.inntektsId)
        assertEquals(spesifisertInntekt.manueltRedigert, klassifisertInntekt.manueltRedigert)
        assertEquals(spesifisertInntekt.sisteAvsluttendeKalenderMåned, klassifisertInntekt.sisteAvsluttendeKalenderMåned)
        assertEquals(0, klassifisertInntekt.inntektsListe.size)
    }

    @Test
    fun `Ta med alle måneder med inntekter`() {
        val testDataJson = klassifiserOgMapInntektTest::class.java
            .getResource("/test-data/example-spesifisert-inntekt-payload.json").readText()
        val spesifisertInntekt = spesifisertInntektJsonAdapter.fromJson(testDataJson)!!

        val sum = spesifisertInntekt.posteringer.fold(BigDecimal.ZERO) { acc, postering -> acc + postering.beløp }
        val spesifiserteMåneder = spesifisertInntekt.posteringer.groupBy { it.posteringsMåned }.keys

        val klassifisertInntekt = klassifiserOgMapInntekt(spesifisertInntekt)

        val klassifisertSum = klassifisertInntekt.inntektsListe.fold(BigDecimal.ZERO) { total, klassifisertInntektMåned ->
            total + klassifisertInntektMåned.klassifiserteInntekter.fold(BigDecimal.ZERO) {
                totalMåned, klassifisertInntekt -> totalMåned + klassifisertInntekt.beløp
            }
        }

        val klassifiserteMåneder = klassifisertInntekt.inntektsListe.map { it.årMåned }.toSet()

        assertEquals(spesifiserteMåneder, klassifiserteMåneder, "Månedene med inntekt er forandret")
        assertEquals(sum, klassifisertSum, "Sum av inntekter er forandret")
    }

    @Test
    fun `Ta med avvik`() {
        val posteringer = listOf(
            Postering(
                posteringsMåned = YearMonth.of(2019, 3),
                beløp = BigDecimal(102),
                fordel = "fordel",
                inntektskilde = "kilde",
                inntektsstatus = "status",
                inntektsperiodetype = "periodetype",
                utbetaltIMåned = YearMonth.of(2019, 3),
                posteringsType = PosteringsType.L_TIMELØNN
            ),
            Postering(
                posteringsMåned = YearMonth.of(2019, 4),
                beløp = BigDecimal(300),
                fordel = "fordel",
                inntektskilde = "kilde",
                inntektsstatus = "status",
                inntektsperiodetype = "periodetype",
                utbetaltIMåned = YearMonth.of(2019, 4),
                posteringsType = PosteringsType.L_TIMELØNN
            ),
            Postering(
                posteringsMåned = YearMonth.of(2019, 5),
                beløp = BigDecimal(550),
                fordel = "fordel",
                inntektskilde = "kilde",
                inntektsstatus = "status",
                inntektsperiodetype = "periodetype",
                utbetaltIMåned = YearMonth.of(2019, 5),
                posteringsType = PosteringsType.L_TIMELØNN
            )
        )

        val avvik = listOf(
            Avvik(
                ident = Aktør(AktørType.AKTOER_ID, "12345"),
                opplysningspliktig = Aktør(AktørType.AKTOER_ID, "12345"),
                avvikPeriode = YearMonth.of(2019, 4),
                tekst = "avvik"
            ),
            Avvik(
                ident = Aktør(AktørType.AKTOER_ID, "12345"),
                opplysningspliktig = Aktør(AktørType.AKTOER_ID, "12345"),
                avvikPeriode = YearMonth.of(2019, 5),
                tekst = "avvik"
            )
        )

        val spesifisertInntekt = SpesifisertInntekt(
            inntektId = InntektId("01DMNAADXVEZXGJRQJTZ6DWWNV"),
            avvik = avvik,
            posteringer = posteringer,
            ident = Aktør(AktørType.AKTOER_ID, "12345"),
            manueltRedigert = false,
            timestamp = LocalDateTime.of(2019, 3, 5, 1, 1),
            sisteAvsluttendeKalenderMåned = YearMonth.of(2019, 5)
        )

        val klassifisertInntekt = klassifiserOgMapInntekt(spesifisertInntekt)

        val klassifiserteMåneder = klassifisertInntekt.inntektsListe.associateBy { it.årMåned }

        assertFalse(klassifiserteMåneder.getValue(YearMonth.of(2019, 3)).harAvvik ?: false)
        assertTrue(klassifiserteMåneder.getValue(YearMonth.of(2019, 4)).harAvvik ?: false)
        assertTrue(klassifiserteMåneder.getValue(YearMonth.of(2019, 5)).harAvvik ?: false)
    }

    @Test
    fun `Klassifiser arbeidsinntekt`() {

        val arbeidsPosteringsTyper = listOf(
            PosteringsType.L_SKATTEPLIKTIG_PERSONALRABATT,
            PosteringsType.L_TIPS,
            PosteringsType.L_AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS,
            PosteringsType.L_ANNET,
            PosteringsType.L_ARBEIDSOPPHOLD_KOST,
            PosteringsType.L_ARBEIDSOPPHOLD_LOSJI,
            PosteringsType.L_BEREGNET_SKATT,
            PosteringsType.L_BESØKSREISER_HJEMMET_ANNET,
            PosteringsType.L_BESØKSREISER_HJEMMET_KILOMETERGODTGJØRELSE_BIL,
            PosteringsType.L_BETALT_UTENLANDSK_SKATT,
            PosteringsType.L_BIL,
            PosteringsType.L_BOLIG,
            PosteringsType.L_BONUS,
            PosteringsType.L_BONUS_FRA_FORSVARET,
            PosteringsType.L_ELEKTRONISK_KOMMUNIKASJON,
            PosteringsType.L_FAST_BILGODTGJØRELSE,
            PosteringsType.L_FAST_TILLEGG,
            PosteringsType.L_FASTLØNN,
            PosteringsType.L_FERIEPENGER,
            PosteringsType.L_FOND_FOR_IDRETTSUTØVERE,
            PosteringsType.Y_FORELDREPENGER,
            PosteringsType.L_HELLIGDAGSTILLEGG,
            PosteringsType.L_HONORAR_AKKORD_PROSENT_PROVISJON,
            PosteringsType.L_HYRETILLEGG,
            PosteringsType.L_INNBETALING_TIL_UTENLANDSK_PENSJONSORDNING,
            PosteringsType.L_KILOMETERGODTGJØRELSE_BIL,
            PosteringsType.L_KOMMUNAL_OMSORGSLØNN_OG_FOSTERHJEMSGODTGJØRELSE,
            PosteringsType.L_KOST_DAGER,
            PosteringsType.L_KOST_DØGN,
            PosteringsType.L_KOSTBESPARELSE_I_HJEMMET,
            PosteringsType.L_LOSJI,
            PosteringsType.L_IKKE_SKATTEPLIKTIG_LØNN_FRA_UTENLANDSK_DIPLOM_KONSUL_STASJON,
            PosteringsType.L_LØNN_FOR_BARNEPASS_I_BARNETS_HJEM,
            PosteringsType.L_LØNN_TIL_PRIVATPERSONER_FOR_ARBEID_I_HJEMMET,
            PosteringsType.L_LØNN_UTBETALT_AV_VELDEDIG_ELLER_ALLMENNYTTIG_INSTITUSJON_ELLER_ORGANISASJON,
            PosteringsType.L_LØNN_TIL_VERGE_FRA_FYLKESMANNEN,
            PosteringsType.L_OPSJONER,
            PosteringsType.L_OVERTIDSGODTGJØRELSE,
            PosteringsType.L_REISE_ANNET,
            PosteringsType.L_REISE_KOST,
            PosteringsType.L_REISE_LOSJI,
            PosteringsType.L_RENTEFORDEL_LÅN,
            PosteringsType.L_SKATTEPLIKTIG_DEL_FORSIKRINGER,
            PosteringsType.L_SLUTTVEDERLAG,
            PosteringsType.L_SMUSSTILLEGG,
            PosteringsType.L_STIPEND,
            PosteringsType.L_STYREHONORAR_OG_GODTGJØRELSE_VERV,
            PosteringsType.Y_SVANGERSKAPSPENGER,
            PosteringsType.L_TIMELØNN,
            PosteringsType.L_TREKK_I_LØNN_FOR_FERIE,
            PosteringsType.L_UREGELMESSIGE_TILLEGG_KNYTTET_TIL_ARBEIDET_TID,
            PosteringsType.L_UREGELMESSIGE_TILLEGG_KNYTTET_TIL_IKKE_ARBEIDET_TID,
            PosteringsType.L_YRKEBIL_TJENESTLIGBEHOV_KILOMETER,
            PosteringsType.L_YRKEBIL_TJENESTLIGBEHOV_LISTEPRIS
        )

        val spesifisertInntekt = createTestSpesifisertInntekt(arbeidsPosteringsTyper)

        val klassifisertInntekt = klassifiserOgMapInntekt(spesifisertInntekt)

        val klasseCount = klassifisertInntekt.inntektsListe
            .flatMap { it.klassifiserteInntekter }
            .groupingBy { it.inntektKlasse }
            .eachCount()

        assertEquals(1, klasseCount.size, "Alle posteringene er ikke klassifisert til samme klasse")
        assertEquals(1, klasseCount[InntektKlasse.ARBEIDSINNTEKT], "Ikke klassifisert som arbeidsinntekt")
    }

    @Test
    fun `Klassifiser dagpenger`() {

        val dagpengerPosteringer = listOf(
            PosteringsType.Y_DAGPENGER_VED_ARBEIDSLØSHET
        )

        val spesifisertInntekt = createTestSpesifisertInntekt(dagpengerPosteringer)

        val klassifisertInntekt = klassifiserOgMapInntekt(spesifisertInntekt)

        val klasseCount = klassifisertInntekt.inntektsListe
            .flatMap { it.klassifiserteInntekter }
            .groupingBy { it.inntektKlasse }
            .eachCount()

        assertEquals(1, klasseCount[InntektKlasse.DAGPENGER], "Ikke klassifisert som dagpenger")
    }

    @Test
    fun `Klassifiser sykepenger`() {

        val sykepengerPosteringer = listOf(
            PosteringsType.Y_SYKEPENGER
        )

        val spesifisertInntekt = createTestSpesifisertInntekt(sykepengerPosteringer)

        val klassifisertInntekt = klassifiserOgMapInntekt(spesifisertInntekt)

        val klasseCount = klassifisertInntekt.inntektsListe
            .flatMap { it.klassifiserteInntekter }
            .groupingBy { it.inntektKlasse }
            .eachCount()

        assertEquals(1, klasseCount[InntektKlasse.SYKEPENGER], "Ikke klassifisert som sykepenger")
    }

    @Test
    fun `Klassifiser fangst og fiske`() {

        val fangstFiskePosteringer = listOf(
            PosteringsType.L_ANNET_H,
            PosteringsType.L_BONUS_H,
            PosteringsType.L_FAST_TILLEGG_H,
            PosteringsType.L_FASTLØNN_H,
            PosteringsType.L_FERIEPENGER_H,
            PosteringsType.L_HELLIGDAGSTILLEGG_H,
            PosteringsType.L_OVERTIDSGODTGJØRELSE_H,
            PosteringsType.L_SLUTTVEDERLAG_H,
            PosteringsType.L_TIMELØNN_H,
            PosteringsType.L_UREGELMESSIGE_TILLEGG_KNYTTET_TIL_ARBEIDET_TID_H,
            PosteringsType.L_UREGELMESSIGE_TILLEGG_KNYTTET_TIL_IKKE_ARBEIDET_TID_H,
            PosteringsType.L_TREKK_I_LØNN_FOR_FERIE_H,
            PosteringsType.N_LOTT_KUN_TRYGDEAVGIFT,
            PosteringsType.N_VEDERLAG
        )

        val spesifisertInntekt = createTestSpesifisertInntekt(fangstFiskePosteringer)

        val klassifisertInntekt = klassifiserOgMapInntekt(spesifisertInntekt)

        val klasseCount = klassifisertInntekt.inntektsListe
            .flatMap { it.klassifiserteInntekter }
            .groupingBy { it.inntektKlasse }
            .eachCount()

        assertEquals(1, klasseCount.size, "Alle posteringene er ikke klassifisert til samme klasse")
        assertEquals(1, klasseCount[InntektKlasse.FANGST_FISKE], "Ikke klassifisert som fangst og fiske")
    }

    @Test
    fun `Klassifiser dagpenger for fangst og fiske`() {

        val dagpengerFangstFiskePosteringer = listOf(
            PosteringsType.N_DAGPENGER_TIL_FISKER,
            PosteringsType.Y_DAGPENGER_TIL_FISKER_SOM_BARE_HAR_HYRE
        )

        val spesifisertInntekt = createTestSpesifisertInntekt(dagpengerFangstFiskePosteringer)

        val klassifisertInntekt = klassifiserOgMapInntekt(spesifisertInntekt)

        val klasseCount = klassifisertInntekt.inntektsListe
            .flatMap { it.klassifiserteInntekter }
            .groupingBy { it.inntektKlasse }
            .eachCount()

        assertEquals(1, klasseCount.size, "Alle posteringene er ikke klassifisert til samme klasse")
        assertEquals(1, klasseCount[InntektKlasse.DAGPENGER_FANGST_FISKE], "Ikke klassifisert som dagpenger for fangst og fiske")
    }

    @Test
    fun `Klassifiser sykepenger for fangst og fiske`() {

        val sykepengerFangstFiskePosteringer = listOf(
            PosteringsType.N_SYKEPENGER_TIL_FISKER,
            PosteringsType.Y_SYKEPENGER_TIL_FISKER_SOM_BARE_HAR_HYRE
        )

        val spesifisertInntekt = createTestSpesifisertInntekt(sykepengerFangstFiskePosteringer)

        val klassifisertInntekt = klassifiserOgMapInntekt(spesifisertInntekt)

        val klasseCount = klassifisertInntekt.inntektsListe
            .flatMap { it.klassifiserteInntekter }
            .groupingBy { it.inntektKlasse }
            .eachCount()

        assertEquals(1, klasseCount.size, "Alle posteringene er ikke klassifisert til samme klasse")
        assertEquals(1, klasseCount[InntektKlasse.SYKEPENGER_FANGST_FISKE], "Ikke klassifisert som sykepenger for fangst og fiske")
    }

    @Test
    fun `Klassifiser tiltakslønn`() {

        val tiltakslønnPosteringer = listOf(
            PosteringsType.L_ANNET_T,
            PosteringsType.L_BONUS_T,
            PosteringsType.L_FAST_TILLEGG_T,
            PosteringsType.L_FASTLØNN_T,
            PosteringsType.L_FERIEPENGER_T,
            PosteringsType.L_HELLIGDAGSTILLEGG_T,
            PosteringsType.L_OVERTIDSGODTGJØRELSE_T,
            PosteringsType.L_SLUTTVEDERLAG_T,
            PosteringsType.L_TIMELØNN_T,
            PosteringsType.L_UREGELMESSIGE_TILLEGG_KNYTTET_TIL_ARBEIDET_TID_T,
            PosteringsType.L_UREGELMESSIGE_TILLEGG_KNYTTET_TIL_IKKE_ARBEIDET_TID_T,
            PosteringsType.L_TREKK_I_LØNN_FOR_FERIE_T
        )

        val spesifisertInntekt = createTestSpesifisertInntekt(tiltakslønnPosteringer)

        val klassifisertInntekt = klassifiserOgMapInntekt(spesifisertInntekt)

        val klasseCount = klassifisertInntekt.inntektsListe
            .flatMap { it.klassifiserteInntekter }
            .groupingBy { it.inntektKlasse }
            .eachCount()

        assertEquals(1, klasseCount.size, "Alle posteringene er ikke klassifisert til samme klasse")
        assertEquals(1, klasseCount[InntektKlasse.TILTAKSLØNN], "Ikke klassifisert som tiltakslønn")
    }

    private fun createTestSpesifisertInntekt(posteringsTyper: List<PosteringsType>): SpesifisertInntekt {
        val posteringer = posteringsTyper.map {
            Postering(
                posteringsMåned = YearMonth.of(2019, 3),
                beløp = BigDecimal(102),
                fordel = "fordel",
                inntektskilde = "kilde",
                inntektsstatus = "status",
                inntektsperiodetype = "periodetype",
                utbetaltIMåned = YearMonth.of(2019, 5),
                posteringsType = it
            )
        }

        return  SpesifisertInntekt(
            inntektId = InntektId("01DMNAADXVEZXGJRQJTZ6DWWNV"),
            avvik = emptyList(),
            posteringer = posteringer,
            ident = Aktør(AktørType.AKTOER_ID, "12345"),
            manueltRedigert = false,
            timestamp = LocalDateTime.of(2019, 3, 5, 1, 1),
            sisteAvsluttendeKalenderMåned = YearMonth.of(2019, 4)
        )
    }
}