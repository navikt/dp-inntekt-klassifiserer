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
import no.nav.helse.rapids_rivers.InMemoryRapid
import no.nav.helse.rapids_rivers.inMemoryRapid
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
            sisteAvsluttendeKalenderMåned = YearMonth.now())
    }

    private val inntektKlassifiserer = mockk<InntektKlassifiserer>(relaxed = true).also {
        every { it.getInntekt(any(), any(), any()) } returns inntekt
    }

    private lateinit var rapid: InMemoryRapid

    @BeforeEach
    fun setUp() {
        rapid = createRapid {
            LøsningService(rapidsConnection = it, inntektKlassifiserer = inntektKlassifiserer)
        }
    }

    @Test
    fun ` Skal innhente løsning for inntekstbehov`() {
        rapid.sendToListeners(
            """
             {
                "@behov": ["$INNTEKT"],
                "@id" : "12345", 
                "aktørId" : "1234",
                "beregningsDato": "2020-04-21"
             }
            """.trimIndent()
        )

        assertSoftly {
            validateMessages(rapid) { messages ->
                messages.size shouldBeExactly 1

                messages.first().also { message ->
                    message["@behov"].map(JsonNode::asText) shouldContain INNTEKT
                    message["@løsning"].hasNonNull(INNTEKT)
                    message["@løsning"][INNTEKT].hasNonNull("inntektsId")
                    message["@løsning"][INNTEKT].hasNonNull("inntektsListe")
                    message["@løsning"][INNTEKT].hasNonNull("sisteAvsluttendeKalenderMåned")

                    val inntektFraPacket: Inntekt = objectMapper.treeToValue(message["@løsning"][INNTEKT], Inntekt::class.java)

                    inntekt.inntektsId shouldBe inntektFraPacket.inntektsId
                    inntekt.inntektsListe shouldBe inntektFraPacket.inntektsListe
                    inntekt.manueltRedigert shouldBe inntektFraPacket.manueltRedigert
                    inntekt.sisteAvsluttendeKalenderMåned shouldBe inntektFraPacket.sisteAvsluttendeKalenderMåned
                }
            }
        }
    }

    private fun validateMessages(rapid: InMemoryRapid, assertions: (messages: List<JsonNode>) -> Any) {
        rapid.outgoingMessages.map { jacksonObjectMapper().readTree(it.value) }.also { assertions(it) }
    }

    private fun createRapid(service: (InMemoryRapid) -> Any): InMemoryRapid {
        return inMemoryRapid { }.also { service(it) }
    }
}
