package no.nav.dagpenger.inntekt.klassifiserer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import no.nav.dagpenger.inntekt.klassifiserer.LøsningService.Companion.INNTEKT
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class LøsningServiceTest {

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        private val inntekt = Inntekt(
            inntektsId = "inntektsid",
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

    private val inntektKlassifiserer = mockk<InntektKlassifiserer>(relaxed = true).also {
        every { it.getInntekt(any(), any(), any(), any()) } returns inntekt
    }

    private val rapid = TestRapid().apply {
        LøsningService(rapidsConnection = this, inntektKlassifiserer = inntektKlassifiserer)
    }

    @BeforeEach
    fun setup() {
        rapid.reset()
    }

    @Test
    fun ` Skal innhente løsning for inntekstbehov`() {
        rapid.sendTestMessage(
            """
             {
                "@behov": ["$INNTEKT"],
                "@id" : "12345", 
                "aktørId" : "1234",
                "fødselsnummer" : "1234",
                "vedtakId" : "vedtakId122423231ljnds",
                "beregningsdato": "2020-04-21"
             }
            """.trimIndent()
        )

        assertSoftly {

            val inspektør = rapid.inspektør
            inspektør.size shouldBeExactly 1
            inspektør.field(0, "@behov").map(JsonNode::asText) shouldContain INNTEKT
            inspektør.field(0, "@løsning").hasNonNull(INNTEKT)
            inspektør.field(0, "@løsning")[INNTEKT].hasNonNull("inntektsId")
            inspektør.field(0, "@løsning")[INNTEKT].hasNonNull("inntektsListe")
            inspektør.field(0, "@løsning")[INNTEKT].hasNonNull("sisteAvsluttendeKalenderMåned")

            val inntektFraPacket: Inntekt =
                objectMapper.treeToValue(inspektør.field(0, "@løsning")[INNTEKT], Inntekt::class.java)

            inntekt.inntektsId shouldBe inntektFraPacket.inntektsId
            inntekt.inntektsListe shouldBe inntektFraPacket.inntektsListe
            inntekt.manueltRedigert shouldBe inntektFraPacket.manueltRedigert
            inntekt.sisteAvsluttendeKalenderMåned shouldBe inntektFraPacket.sisteAvsluttendeKalenderMåned
        }
    }
}
