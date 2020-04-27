package no.nav.dagpenger.inntekt.klassifiserer

import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import no.nav.dagpenger.events.inntekt.v1.Aktør
import no.nav.dagpenger.events.inntekt.v1.AktørType
import no.nav.dagpenger.events.inntekt.v1.Avvik
import no.nav.dagpenger.events.inntekt.v1.InntektId
import no.nav.dagpenger.events.inntekt.v1.InntektKlasse
import no.nav.dagpenger.events.inntekt.v1.Postering
import no.nav.dagpenger.events.inntekt.v1.PosteringsType
import no.nav.dagpenger.events.inntekt.v1.SpesifisertInntekt
import org.junit.jupiter.api.Test

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
        assertEquals(
            spesifisertInntekt.sisteAvsluttendeKalenderMåned,
            klassifisertInntekt.sisteAvsluttendeKalenderMåned
        )
        assertEquals(0, klassifisertInntekt.inntektsListe.size)
    }

    @Test
    fun `Ta med alle måneder med inntekter`() {
        val testDataJson = klassifiserOgMapInntektTest::class.java
            .getResource("/test-data/spesifisert-inntekt-mange-posteringer.json").readText()
        val spesifisertInntekt = spesifisertInntektJsonAdapter.fromJson(testDataJson)!!

        val sum = spesifisertInntekt.posteringer.fold(BigDecimal.ZERO) { acc, postering -> acc + postering.beløp }
        val spesifiserteMåneder = spesifisertInntekt.posteringer.groupBy { it.posteringsMåned }.keys

        val klassifisertInntekt = klassifiserOgMapInntekt(spesifisertInntekt)

        val klassifisertSum =
            klassifisertInntekt.inntektsListe.fold(BigDecimal.ZERO) { total, klassifisertInntektMåned ->
                total + klassifisertInntektMåned.klassifiserteInntekter.fold(BigDecimal.ZERO) { totalMåned, klassifisertInntekt ->
                    totalMåned + klassifisertInntekt.beløp
                }
            }

        val klassifiserteMåneder = klassifisertInntekt.inntektsListe.map { it.årMåned }.toSet()

        assertEquals(spesifiserteMåneder, klassifiserteMåneder, "Månedene med inntekt er forandret")
        assertEquals(sum, klassifisertSum, "Sum av inntekter er forandret")
    }

    @Test
    fun `Inntekt grupperes riktig i måneder`() {
        val testDataJson = klassifiserOgMapInntektTest::class.java
            .getResource("/test-data/spesifisert-inntekt-flere-klasser.json").readText()
        val spesifisertInntekt = spesifisertInntektJsonAdapter.fromJson(testDataJson)!!

        val klassifisertInntekt = klassifiserOgMapInntekt(spesifisertInntekt)

        val firstMonth = klassifisertInntekt.inntektsListe.first()

        val inntektsKlasser = firstMonth.klassifiserteInntekter.map { it.inntektKlasse }.toSet()

        firstMonth.klassifiserteInntekter.size shouldBe 3
        inntektsKlasser.size shouldBe 3
        inntektsKlasser shouldBe setOf(
            InntektKlasse.ARBEIDSINNTEKT,
            InntektKlasse.DAGPENGER,
            InntektKlasse.SYKEPENGER
        )
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
}

fun createTestSpesifisertInntekt(posteringsTyper: List<PosteringsType>): SpesifisertInntekt {
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

    return SpesifisertInntekt(
        inntektId = InntektId("01DMNAADXVEZXGJRQJTZ6DWWNV"),
        avvik = emptyList(),
        posteringer = posteringer,
        ident = Aktør(AktørType.AKTOER_ID, "12345"),
        manueltRedigert = false,
        timestamp = LocalDateTime.of(2019, 3, 5, 1, 1),
        sisteAvsluttendeKalenderMåned = YearMonth.of(2019, 4)
    )
}
