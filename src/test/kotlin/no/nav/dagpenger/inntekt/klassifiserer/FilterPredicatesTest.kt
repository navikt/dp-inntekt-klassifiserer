package no.nav.dagpenger.inntekt.klassifiserer

import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.finn.unleash.FakeUnleash
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.inntekt.v1.Aktør
import no.nav.dagpenger.events.inntekt.v1.AktørType
import no.nav.dagpenger.events.inntekt.v1.InntektId
import no.nav.dagpenger.events.inntekt.v1.SpesifisertInntekt
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class OnPacketTest {

    @Test
    fun `Add klassifisert inntekt to behov`() {
        val spesifisertInntektMock: SpesifisertInntektHttpClient = mockk()
        every {
            spesifisertInntektMock.getSpesifisertInntekt(
                "123",
                12345,
                LocalDate.of(2020, 1, 1)
            )
        } returns SpesifisertInntekt(
            inntektId = InntektId("01DJ7VC8PHZ4D308MWX8TVDTDN"),
            avvik = emptyList(),
            posteringer = emptyList(),
            ident = Aktør(AktørType.AKTOER_ID, "123"),
            manueltRedigert = false,
            timestamp = LocalDateTime.of(2019, 4, 13, 1, 1),
            sisteAvsluttendeKalenderMåned = YearMonth.of(2019, 5)
        )

        val app = App(configuration = Configuration(), spesifisertInntektHttpClient = spesifisertInntektMock, unleash = FakeUnleash())

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
        val app = App(configuration = Configuration(), spesifisertInntektHttpClient = mockk(), unleash = FakeUnleash().apply { disableAll() })
        app.filterPredicates().all { it.test("", packet) } shouldBe false
    }

    @Test
    fun `Skal legge på inntekt der det er ikke er manuelt grunnlag`() {
        val packet = Packet()
        packet.putValue("vedtakId", 123)
        val app = App(configuration = Configuration(), spesifisertInntektHttpClient = mockk(), unleash = FakeUnleash().apply { enableAll() })
        app.filterPredicates().all { it.test("", packet) } shouldBe true
    }
}
