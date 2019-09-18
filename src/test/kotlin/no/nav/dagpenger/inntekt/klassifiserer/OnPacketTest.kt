package no.nav.dagpenger.inntekt.klassifiserer

import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.inntekt.v1.Aktør
import no.nav.dagpenger.events.inntekt.v1.AktørType
import no.nav.dagpenger.events.inntekt.v1.InntektId
import no.nav.dagpenger.events.inntekt.v1.SpesifisertInntekt
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth

class OnPacketTest {
    @Test
    fun `Add klassifisert inntekt to behov`() {
        val app = App()

        val spesifisertInntekt = SpesifisertInntekt(
            inntektId = InntektId("01DJ7VC8PHZ4D308MWX8TVDTDN"),
            avvik = emptyList(),
            posteringer = emptyList(),
            ident = Aktør(AktørType.AKTOER_ID, "123"),
            manueltRedigert = false,
            timestamp = LocalDateTime.of(2019, 4, 13, 1, 1),
            sisteAvsluttendeKalenderMåned = YearMonth.of(2019, 5)
        )

        val inputPacket = Packet()
        inputPacket.putValue("spesifisertInntektV1", spesifisertInntektJsonAdapter.toJsonValue(spesifisertInntekt)!!)

        val resultPacket = app.onPacket(inputPacket)

        assert(resultPacket.hasField("inntektV1"))
    }
}