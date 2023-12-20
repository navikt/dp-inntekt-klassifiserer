package no.nav.dagpenger.inntekt.klassifiserer

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.inntekt.v1.Inntekt
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.MathContext
import java.time.YearMonth

class InntektJsonMapperTest {
    @Test
    fun `test json mappiong`() {
        val mathContext = MathContext(4)
        Inntekt(
            inntektsId = "123",
            inntektsListe =
                listOf(
                    KlassifisertInntektMåned(
                        årMåned = YearMonth.parse("2020-01"),
                        klassifiserteInntekter =
                            listOf(
                                KlassifisertInntekt(beløp = BigDecimal.ONE, inntektKlasse = InntektKlasse.ARBEIDSINNTEKT),
                                KlassifisertInntekt(
                                    beløp = BigDecimal(19.11, mathContext),
                                    inntektKlasse = InntektKlasse.DAGPENGER,
                                ),
                            ),
                        harAvvik = false,
                    ),
                    KlassifisertInntektMåned(
                        årMåned = YearMonth.parse("2020-02"),
                        klassifiserteInntekter =
                            listOf(
                                KlassifisertInntekt(
                                    beløp = BigDecimal(1.111, mathContext),
                                    inntektKlasse = InntektKlasse.ARBEIDSINNTEKT,
                                ),
                            ),
                        harAvvik = true,
                    ),
                    KlassifisertInntektMåned(
                        årMåned = YearMonth.parse("2020-03"),
                        klassifiserteInntekter =
                            listOf(
                                KlassifisertInntekt(
                                    beløp = BigDecimal.TEN,
                                    inntektKlasse = InntektKlasse.DAGPENGER_FANGST_FISKE,
                                ),
                            ),
                        harAvvik = true,
                    ),
                ),
            manueltRedigert = null,
            sisteAvsluttendeKalenderMåned = YearMonth.parse("2020-01"),
        ).toMap() shouldBe
            mapOf(
                "inntektsId" to "123",
                "inntektsListe" to
                    listOf<Map<String, Any>>(
                        mapOf(
                            "årMåned" to "2020-01",
                            "klassifiserteInntekter" to
                                listOf<Map<String, Any>>(
                                    mapOf(
                                        "beløp" to "1",
                                        "inntektKlasse" to "ARBEIDSINNTEKT",
                                    ),
                                    mapOf(
                                        "beløp" to "19.11",
                                        "inntektKlasse" to "DAGPENGER",
                                    ),
                                ),
                            "harAvvik" to false,
                        ),
                        mapOf(
                            "årMåned" to "2020-02",
                            "klassifiserteInntekter" to
                                listOf<Map<String, Any>>(
                                    mapOf(
                                        "beløp" to "1.111",
                                        "inntektKlasse" to "ARBEIDSINNTEKT",
                                    ),
                                ),
                            "harAvvik" to true,
                        ),
                        mapOf(
                            "årMåned" to "2020-03",
                            "klassifiserteInntekter" to
                                listOf<Map<String, Any>>(
                                    mapOf(
                                        "beløp" to "10",
                                        "inntektKlasse" to "DAGPENGER_FANGST_FISKE",
                                    ),
                                ),
                            "harAvvik" to true,
                        ),
                    ),
                "manueltRedigert" to false,
                "sisteAvsluttendeKalenderMåned" to "2020-01",
            )
    }
}
