package no.nav.dagpenger.inntekt.klassifiserer

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.AKTØRID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.BEHOV_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.BEREGNINGSDATO
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.FORRIGE_GRUNNLAG
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.FØDSELSNUMMER
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.INNTEKT
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.INNTEKT_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.KONTEKST_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.KONTEKST_TYPE
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.MANUELT_GRUNNLAG
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
    fun `Skal ikke behandle pakker uten required keys`() {
        val testListener = TestListener(testRapid)
        val packetUtenRequiredKey = JsonMessage.newMessage(emptyMap()).toJson()
        testRapid.sendTestMessage(packetUtenRequiredKey)
        testListener.onPacketCalled shouldBe false
    }

    @Test
    fun `Skal ikke behandle pakker med rejected keys`() {
        val testListener = TestListener(testRapid)
        testRapid.sendTestMessage(
            testMessageMedRequiredFelter(mapOf(INNTEKT to "finInntekt")),
        )
        testListener.onPacketCalled shouldBe false
        testRapid.sendTestMessage(
            testMessageMedRequiredFelter(mapOf(MANUELT_GRUNNLAG to "søtesteManuelleGrunnlaget")),
        )
        testListener.onPacketCalled shouldBe false
        testRapid.sendTestMessage(
            testMessageMedRequiredFelter(mapOf(FORRIGE_GRUNNLAG to "jeg er det forrige grunnlaget")),
        )
        testListener.onPacketCalled shouldBe false
    }

    @Test
    fun `Skal kunne hente ut interessante verdier fra pakka`() {
        val testListener = TestListener(testRapid)
        testRapid.sendTestMessage(testMessageMedRequiredFelter())
        shouldNotThrowAny {
            testListener.jsonMessage[BEREGNINGSDATO]
            testListener.jsonMessage[INNTEKT_ID]
            testListener.jsonMessage[AKTØRID]
            testListener.jsonMessage[FØDSELSNUMMER]
            testListener.jsonMessage[KONTEKST_ID]
            testListener.jsonMessage[KONTEKST_TYPE]
        }
        shouldThrow<IllegalArgumentException> {
            testListener.jsonMessage["HUBBABUBBA"]
        }
    }

    private fun testMessageMedRequiredFelter(ekstraFelter: Map<String, Any> = emptyMap()) =
        JsonMessage.newMessage(
            mapOf(BEHOV_ID to "behovId") + ekstraFelter,
        ).toJson()

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
