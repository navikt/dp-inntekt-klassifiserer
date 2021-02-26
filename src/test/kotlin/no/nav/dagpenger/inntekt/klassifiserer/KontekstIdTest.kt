package no.nav.dagpenger.inntekt.klassifiserer

import no.nav.dagpenger.events.Packet
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class KontekstIdTest {
    @Test
    fun `Skal hente riktig KontekstId eller feile`() {
        val packetWithKontekstAndVedtakId =
            """
            {
                "aktørId": "12345",
                "kontekstId": "kontekstId",
                "vedtakId": 134,
                "beregningsDato": 2019-01-25
           }
            """.trimIndent()
        val packetWithoutVedtakId =
            """
            {
                "aktørId": "12345",
                "kontekstId": "kontekstId",
                "beregningsDato": 2019-01-25
           }
            """.trimIndent()
        val packetWithoutKontekstId =
            """
            {
                "aktørId": "12345",
                "vedtakId": 123,
                "beregningsDato": 2019-01-25
           }
            """.trimIndent()
        val packetWithoutAnyId =
            """
            {
                "aktørId": "12345",
                "beregningsDato": 2019-01-25
           }
            """.trimIndent()
        assertEquals("kontekstId", Packet(packetWithKontekstAndVedtakId).hentKontekstId())
        assertEquals("kontekstId", Packet(packetWithoutVedtakId).hentKontekstId())
        assertEquals("123", Packet(packetWithoutKontekstId).hentKontekstId())
        assertFails { Packet(packetWithoutAnyId).hentKontekstId() }
    }
}
