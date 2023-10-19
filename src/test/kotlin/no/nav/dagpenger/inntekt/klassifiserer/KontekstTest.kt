package no.nav.dagpenger.inntekt.klassifiserer

import no.nav.dagpenger.events.Packet
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class KontekstTest {
    @Test
    fun `Skal hente riktig kontekstId og kontekstType eller feile`() {
        val packetWithKontekstTypeAndId =
            """
             {
                 "aktørId": "12345",
                 "kontekstId": "kontekstId",
                 "kontekstType": "vedtak",
                 "beregningsDato": 2019-01-25
            }
            """.trimIndent()

        val packetWithoutKontekstId =
            """
             {
                 "aktørId": "12345",
                 "beregningsDato": 2019-01-25
            }
            """.trimIndent()

        assertEquals(RegelKontekst("kontekstId", "vedtak"), Packet(packetWithKontekstTypeAndId).hentRegelkontekst())
        assertFails { Packet(packetWithoutKontekstId).hentRegelkontekst() }
    }
}
