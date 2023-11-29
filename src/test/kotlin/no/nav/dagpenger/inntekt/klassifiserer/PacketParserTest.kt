package no.nav.dagpenger.inntekt.klassifiserer

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.BEREGNINGSDATO
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.INNTEKT_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.SYSTEM_STARTED
import no.nav.dagpenger.inntekt.klassifiserer.PacketParser.aktørId
import no.nav.dagpenger.inntekt.klassifiserer.PacketParser.behovId
import no.nav.dagpenger.inntekt.klassifiserer.PacketParser.beregningsdato
import no.nav.dagpenger.inntekt.klassifiserer.PacketParser.inntektsId
import no.nav.dagpenger.inntekt.klassifiserer.PacketParser.systemStarted
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PacketParserTest {
    private val testRapid = TestRapid()

    @Test
    fun `Skal mappe behovId`() {
        val behovløser = OnPacketTestListener(testRapid)

        val testMessage = JsonMessage.newMessage(mapOf(InntektBehovløser.BEHOV_ID to "behovId")).toJson()
        testRapid.sendTestMessage(testMessage)
        behovløser.packet.behovId() shouldBe "behovId"

        testRapid.sendTestMessage(JsonMessage.newMessage(emptyMap()).toJson())
        behovløser.packet.behovId() shouldBe null
    }

    @Test
    fun `Skal mappe beregningsdato`() {
        val behovløser = OnPacketTestListener(testRapid)

        val testMessage = JsonMessage.newMessage(mapOf(BEREGNINGSDATO to LocalDate.MAX)).toJson()
        testRapid.sendTestMessage(testMessage)
        behovløser.packet.beregningsdato() shouldBe LocalDate.MAX

        testRapid.sendTestMessage(JsonMessage.newMessage(emptyMap()).toJson())
        behovløser.packet.beregningsdato() shouldBe null
    }

    @Test
    fun `Skal mappe innteksId`() {
        val behovløser = OnPacketTestListener(testRapid)

        val testMessage = JsonMessage.newMessage(mapOf(INNTEKT_ID to "Supernice inntektsId")).toJson()
        testRapid.sendTestMessage(testMessage)
        behovløser.packet.inntektsId() shouldBe "Supernice inntektsId"

        testRapid.sendTestMessage(JsonMessage.newMessage(emptyMap()).toJson())
        behovløser.packet.inntektsId() shouldBe null
    }

    @Test
    fun `Skal mappe aktørId`() {
        val behovløser = OnPacketTestListener(testRapid)

        val testMessage = JsonMessage.newMessage(mapOf(InntektBehovløser.AKTØRID to "aktørId")).toJson()
        testRapid.sendTestMessage(testMessage)
        behovløser.packet.aktørId() shouldBe "aktørId"

        testRapid.sendTestMessage(JsonMessage.newMessage(emptyMap()).toJson())
        behovløser.packet.aktørId() shouldBe null
    }

    @Test
    fun `Skal mappe system_started`() {
        val behovløser = OnPacketTestListener(testRapid)

        val testMessage = JsonMessage.newMessage(mapOf(SYSTEM_STARTED to LocalDateTime.MAX)).toJson()
        testRapid.sendTestMessage(testMessage)
        behovløser.packet.systemStarted() shouldBe LocalDateTime.MAX

        val testMessage2 = JsonMessage.newMessage(mapOf(SYSTEM_STARTED to "FeilformatertDato")).toJson()
        testRapid.sendTestMessage(JsonMessage.newMessage(testMessage2).toJson())
        behovløser.packet.systemStarted() shouldBe null

        testRapid.sendTestMessage(JsonMessage.newMessage(emptyMap()).toJson())
        behovløser.packet.systemStarted() shouldBe null
    }

    private class OnPacketTestListener(rapidsConnection: RapidsConnection) : River.PacketListener {
        var problems: MessageProblems? = null
        lateinit var packet: JsonMessage

        init {
            River(rapidsConnection).apply(
                InntektBehovløser.rapidFilter,
            ).register(this)
        }

        override fun onPacket(
            packet: JsonMessage,
            context: MessageContext,
        ) {
            this.packet = packet
        }

        override fun onError(
            problems: MessageProblems,
            context: MessageContext,
        ) {
            this.problems = problems
        }
    }
}
