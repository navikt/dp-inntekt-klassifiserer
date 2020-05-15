package no.nav.dagpenger.inntekt.klassifiserer

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.YearMonth
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.inntekt.v1.InntektKlasse
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntektMåned
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class LøsningServiceTest {

    companion object {
        private val inntektsId = "221212"
        private val inntekt = Inntekt(
            inntektsId = inntektsId,
            manueltRedigert = false,
            inntektsListe = listOf(
                KlassifisertInntektMåned(
                    årMåned = YearMonth.now(),
                    klassifiserteInntekter = listOf(
                        KlassifisertInntekt(
                            beløp = BigDecimal("1000.12"),
                            inntektKlasse = InntektKlasse.ARBEIDSINNTEKT
                        )
                    ),
                    harAvvik = false
                )
            ),
            sisteAvsluttendeKalenderMåned = YearMonth.now()
        )
    }

    private val inntektHttpClient = mockk<InntektHttpClient>(relaxed = true).also {
        every { it.getKlassifisertInntekt(any(), any(), any(), any()) } returns inntekt
    }

    private val rapid = TestRapid().apply {
        LøsningService(rapidsConnection = this, inntektHttpClient = inntektHttpClient)
    }

    @BeforeEach
    fun setup() {
        rapid.reset()
    }

    @Test
    fun ` Skal innhente løsning for inntektsbehov`() {
        rapid.sendTestMessage(
            """
             {
                "@behov": ["InntektId"],
                "@id" : "12345", 
                "aktørId" : "1234",
                "fødselsnummer" : "1234",
                "vedtakId" : "12345",
                "beregningsdato": "2020-04-21"
             }
            """.trimIndent()
        )

        assertSoftly {

            val inspektør = rapid.inspektør
            inspektør.size shouldBeExactly 1
            inspektør.field(0, "@behov").map(JsonNode::asText) shouldContain "InntektId"
            inspektør.field(0, "@løsning").hasNonNull("InntektId")
            inspektør.field(0, "@løsning")["InntektId"].asText() shouldBe inntektsId
        }
    }
}
