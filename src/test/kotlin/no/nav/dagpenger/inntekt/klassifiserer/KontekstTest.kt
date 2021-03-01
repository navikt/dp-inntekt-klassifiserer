package no.nav.dagpenger.inntekt.klassifiserer

import no.nav.dagpenger.events.Packet
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class KontekstTest {
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
        assertEquals("kontekstId", Packet(packetWithKontekstAndVedtakId).hentRegelkontekst().id)
        assertEquals("kontekstId", Packet(packetWithoutVedtakId).hentRegelkontekst().id)
        assertEquals("123", Packet(packetWithoutKontekstId).hentRegelkontekst().id)
        assertFails { Packet(packetWithoutAnyId).hentRegelkontekst().id }
    }

    @Test
    fun `henter ut konteksttype`() {
        val packetWithKontekstType =
            """
            {
                "kontekstId": "kontekstId",
                "kontekstType": "VEDTAK"
           }
            """.trimIndent()
        val packetWithoutKontekstType =
            """
            {
                "kontekstId": "kontekstId"
           }
            """.trimIndent()
        assertEquals("VEDTAK", Packet(packetWithKontekstType).hentRegelkontekst().type)
        assertEquals("UKJENT", Packet(packetWithoutKontekstType).hentRegelkontekst().type)
    }
}
