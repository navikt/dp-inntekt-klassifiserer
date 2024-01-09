package no.nav.dagpenger.inntekt.klassifiserer

import io.kotest.assertions.throwables.shouldNotThrowAnyUnit
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.AKTØRID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.BEHOV_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.BEREGNINGSDATO
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.INNTEKT_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.KONTEKST_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.KONTEKST_TYPE
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.PROBLEM
import no.nav.dagpenger.inntekt.v1.Inntekt
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.time.YearMonth

class InntektBehovløserTest {
    private val testRapid = TestRapid()

    @Test
    fun `Skal hente inntekter inntekts id er satt`() {
        val inntektHttpClient: InntektHttpClient =
            mockk<InntektHttpClient>().also {
                coEvery {
                    it.getKlassifisertInntekt(
                        inntektId = "inntektId", callId = any(),
                    )
                } returns inntekt
            }

        InntektBehovløser(
            testRapid,
            inntektHttpClient,
        )

        testRapid.sendTestMessage("""{ "$INNTEKT_ID":"inntektId", "$BEHOV_ID":"kaktus" }""")

        testRapid.inspektør.message(0).also {
            it["inntektV1"] shouldNotBe null
        }
    }

    @Test
    fun `Skal hente inntekter dersom beregningsdato, aktørId og regelkontekst er  satt`() {
        val inntektHttpClient: InntektHttpClient =
            mockk<InntektHttpClient>().also {
                coEvery {
                    it.getKlassifisertInntekt(
                        aktørId = "aktørId",
                        regelkontekst = RegelKontekst("kontekstId", "konteksType"),
                        beregningsDato = LocalDate.parse("2020-01-01"),
                        fødselsnummer = null,
                        callId = any(),
                    )
                } returns inntekt
            }

        InntektBehovløser(
            testRapid,
            inntektHttpClient,
        )

        testRapid.sendTestMessage(
            """
            {
               "$BEREGNINGSDATO":"2020-01-01",
               "$AKTØRID":"aktørId",
               "$KONTEKST_TYPE" : "konteksType",
               "$KONTEKST_ID" : "kontekstId", 
               "$BEHOV_ID":"kaktus"
            }
            
            """.trimIndent(),
        )

        testRapid.inspektør.message(0).also {
            it["inntektV1"] shouldNotBe null
        }
    }

    @Test
    fun `Skal ikke behandle pakker som har problem`() {
        val inntektHttpClient: InntektHttpClient = mockk<InntektHttpClient>()

        InntektBehovløser(
            testRapid,
            inntektHttpClient,
        )

        //language=JSON
        testRapid.sendTestMessage("""{ "$BEHOV_ID":"kaktus", "$PROBLEM": "problem" }""")
        verify { inntektHttpClient wasNot Called }
    }

    @Test
    fun `Kaster exception dersom beregningsdato ikke er satt for behov uten inntektId`() {
        InntektBehovløser(testRapid, mockk())

        shouldThrow<IllegalArgumentException> {
            testRapid.sendTestMessage(
                """
                {
                   "$AKTØRID":"aktørId",
                   "$KONTEKST_TYPE" : "konteksType",
                   "$KONTEKST_ID" : "kontekstId", 
                   "$BEHOV_ID":"kaktus"
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `Kaster exception dersom aktørId  eller fødselsnummer ikke er satt for behov uten inntektId`() {
        InntektBehovløser(testRapid, mockk())

        shouldThrow<IllegalArgumentException> {
            testRapid.sendTestMessage(
                """
                {
                   "$BEREGNINGSDATO":"2020-01-01",
                   "$KONTEKST_TYPE" : "konteksType",
                   "$KONTEKST_ID" : "kontekstId", 
                   "$BEHOV_ID":"kaktus"
                }
                
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `Kaster exception dersom konteks ikke er satt for behov uten inntektId`() {
        InntektBehovløser(testRapid, mockk())

        shouldThrow<IllegalArgumentException> {
            testRapid.sendTestMessage(
                """
                {
                   "$BEREGNINGSDATO":"2020-01-01",
                   "$AKTØRID":"aktørId", 
                   "$BEHOV_ID":"kaktus"
                }
                
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `Skal kaste exception dersom inntektId er satt og  kall mot inntekt api feiler`() {
        InntektBehovløser(
            testRapid,
            mockk<InntektHttpClient>().also {
                coEvery { it.getKlassifisertInntekt(any(), any()) } throws
                    InntektApiHttpClientException(
                        message = "Feilk",
                        problem =
                            Problem(
                                type = URI("type"),
                                title = "title",
                                status = 500,
                                detail = "detail",
                                instance = URI("instance"),
                            ),
                        cause = RuntimeException("cause"),
                    )
            },
        )

        shouldThrow<InntektApiHttpClientException> {
            testRapid.sendTestMessage("""{ "$INNTEKT_ID":"inntektId", "$BEHOV_ID":"kaktus" }""")
        }
    }

    @Test
    fun `Skal svelge exception men legge på Problem dersom kall mot inntekts api feiler for behov uten inntekts id`() {
        InntektBehovløser(
            testRapid,
            mockk<InntektHttpClient>().also {
                coEvery { it.getKlassifisertInntekt(any(), any(), any(), any(), any()) } throws
                    InntektApiHttpClientException(
                        message = "Feil",
                        problem =
                            Problem(
                                type = URI("type"),
                                title = "title",
                                status = 500,
                                detail = "detail",
                                instance = URI("instance"),
                            ),
                        cause = RuntimeException("cause"),
                    )
            },
        )

        shouldNotThrowAnyUnit {
            testRapid.sendTestMessage(
                """
                {
                   "$BEREGNINGSDATO":"2020-01-01",
                   "$AKTØRID":"aktørId",
                   "$KONTEKST_TYPE" : "konteksType",
                   "$KONTEKST_ID" : "kontekstId", 
                   "$BEHOV_ID":"kaktus"
            } """,
            )
        }

        val resultatPacket = testRapid.inspektør.message(0)
        resultatPacket[PROBLEM] shouldNotBe null
        resultatPacket[PROBLEM]["status"].asInt() shouldBe 500
    }

    private companion object {
        val inntekt =
            Inntekt(
                inntektsId = "inntektsid",
                manueltRedigert = false,
                inntektsListe =
                    listOf(
                        KlassifisertInntektMåned(
                            årMåned = YearMonth.now(),
                            klassifiserteInntekter =
                                listOf(
                                    KlassifisertInntekt(
                                        beløp = BigDecimal("1000.12"),
                                        inntektKlasse = InntektKlasse.ARBEIDSINNTEKT,
                                    ),
                                ),
                            harAvvik = false,
                        ),
                    ),
                sisteAvsluttendeKalenderMåned = YearMonth.now(),
            )
    }
}
