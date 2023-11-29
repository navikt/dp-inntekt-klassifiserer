package no.nav.dagpenger.inntekt.klassifiserer

import io.kotest.matchers.shouldBe
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

class RapidFilterTest {
    private val testRapid = TestRapid()

    @Test
    fun `Skal ikke behandle pakker med rejected keys`() {
        val testListener = TestListener(testRapid)
        testRapid.sendTestMessage(
            JsonMessage.newMessage(mapOf(InntektBehovløser.INNTEKT to "finInntekt")).toJson(),
        )
        testListener.onPacketCalled shouldBe false
        testRapid.sendTestMessage(
            JsonMessage.newMessage(mapOf(InntektBehovløser.MANUELT_GRUNNLAG to "søtesteManuelleGrunnlaget")).toJson(),
        )
        testListener.onPacketCalled shouldBe false
        testRapid.sendTestMessage(
            JsonMessage.newMessage(mapOf(InntektBehovløser.FORRIGE_GRUNNLAG to "jeg er det forrige grunnlaget")).toJson(),
        )
        testListener.onPacketCalled shouldBe false
    }

    private class TestListener(rapidsConnection: RapidsConnection) : River.PacketListener {
        var onPacketCalled = false

        init {
            River(rapidsConnection).apply(
                InntektBehovløser.rapidFilter,
            ).register(this)
        }

        override fun onPacket(
            packet: JsonMessage,
            context: MessageContext,
        ) {
            this.onPacketCalled = true
        }

        override fun onError(
            problems: MessageProblems,
            context: MessageContext,
        ) {
        }
    }
}
