package no.nav.dagpenger.inntekt.klassifiserer

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.AKTØRID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.BEHOV_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.BEREGNINGSDATO
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.INNTEKT_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.KONTEKST_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.KONTEKST_TYPE
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.SYSTEM_STARTED
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

    @Test
    fun `Skal kunne hente ut "interessante" verdier fra pakka`() {
        val testListener = TestListener(testRapid)
        testRapid.sendTestMessage(
            JsonMessage.newMessage(emptyMap()).toJson(),
        )
        shouldNotThrowAny {
            testListener.jsonMessage[BEHOV_ID]
            testListener.jsonMessage[SYSTEM_STARTED]
            testListener.jsonMessage[BEREGNINGSDATO]
            testListener.jsonMessage[INNTEKT_ID]
            testListener.jsonMessage[AKTØRID]
            testListener.jsonMessage[KONTEKST_ID]
            testListener.jsonMessage[KONTEKST_TYPE]
        }
        shouldThrow<IllegalArgumentException> {
            testListener.jsonMessage["HUBBABUBBA"]
        }
    }

    private class TestListener(rapidsConnection: RapidsConnection) : River.PacketListener {
        var onPacketCalled = false
        lateinit var jsonMessage: JsonMessage

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
            this.jsonMessage = packet
        }

        override fun onError(
            problems: MessageProblems,
            context: MessageContext,
        ) {
        }
    }
}
