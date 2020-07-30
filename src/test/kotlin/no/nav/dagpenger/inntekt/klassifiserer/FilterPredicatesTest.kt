package no.nav.dagpenger.inntekt.klassifiserer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.inntekt.v1.InntektKlasse
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntektMåned
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class OnPacketTest {

    companion object {
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
    @Test
    fun `Add klassifisert inntekt to behov`() {
        val inntektHttpClient: InntektHttpClient = mockk()
        every {
            inntektHttpClient.getKlassifisertInntekt(
                "123",
                "12345",
                LocalDate.of(2020, 1, 1),
                null
            )
        } returns inntekt

        val app = Application(
            configuration = Configuration(),
            inntektHttpClient = inntektHttpClient,
            healthCheck = mockk(relaxed = true),
            inntektHenter = mockk(relaxed = true)
        )

        val inputPacket = Packet()
        inputPacket.putValue("aktørId", "123")
        inputPacket.putValue("vedtakId", 12345)
        inputPacket.putValue("beregningsDato", "2020-01-01")

        val resultPacket = app.onPacket(inputPacket)

        assert(resultPacket.hasField("inntektV1"))
    }
}

class FilterPredicatesTest {

    @Test
    fun `Skal ikke legge på inntekt der det er manuelt grunnlag`() {
        val packet = Packet().apply {
            putValue("manueltGrunnlag", 1000)
        }
        val app = Application(
            configuration = Configuration(),
            inntektHttpClient = mockk(),
            healthCheck = mockk(relaxed = true),
            inntektHenter = mockk(relaxed = true)
        )
        app.filterPredicates().all { it.test("", packet) } shouldBe false
    }

    @Test
    fun `Skal legge på inntekt der det er ikke er manuelt grunnlag`() {
        val packet = Packet()
        packet.putValue("vedtakId", 123)
        val app = Application(
            configuration = Configuration(),
            inntektHttpClient = mockk(),
            healthCheck = mockk(relaxed = true),
            inntektHenter = mockk(relaxed = true)
        )
        app.filterPredicates().all { it.test("", packet) } shouldBe true
    }
}
