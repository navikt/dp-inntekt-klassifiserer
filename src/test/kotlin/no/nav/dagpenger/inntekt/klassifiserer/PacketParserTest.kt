package no.nav.dagpenger.inntekt.klassifiserer

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.AKTØRID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.BEHOV_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.BEREGNINGSDATO
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.INNTEKT_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.KONTEKST_ID
import no.nav.dagpenger.inntekt.klassifiserer.InntektBehovløser.Companion.KONTEKST_TYPE
import no.nav.dagpenger.inntekt.klassifiserer.PacketParser.aktørId
import no.nav.dagpenger.inntekt.klassifiserer.PacketParser.behovId
import no.nav.dagpenger.inntekt.klassifiserer.PacketParser.beregningsdato
import no.nav.dagpenger.inntekt.klassifiserer.PacketParser.hentRegelkontekst
import no.nav.dagpenger.inntekt.klassifiserer.PacketParser.inntektsId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PacketParserTest {
    private val testRapid = TestRapid()

    @Test
    fun `Skal mappe behovId`() {
        val behovløser = OnPacketTestListener(testRapid)

        testRapid.sendTestMessage(testMessageMedRequiredFelter(mapOf(BEHOV_ID to "jeg har behov for id")))
        behovløser.packet.behovId() shouldBe "jeg har behov for id"
    }

    @Test
    fun `Skal mappe beregningsdato`() {
        val behovløser = OnPacketTestListener(testRapid)

        testRapid.sendTestMessage(testMessageMedRequiredFelter(mapOf(BEREGNINGSDATO to LocalDate.MAX)))
        behovløser.packet.beregningsdato() shouldBe LocalDate.MAX

        testRapid.sendTestMessage(testMessageMedRequiredFelter())
        behovløser.packet.beregningsdato() shouldBe null
    }

    @Test
    fun `Skal mappe innteksId`() {
        val behovløser = OnPacketTestListener(testRapid)

        testRapid.sendTestMessage(testMessageMedRequiredFelter(mapOf(INNTEKT_ID to "Supernice inntektsId")))
        behovløser.packet.inntektsId() shouldBe "Supernice inntektsId"

        testRapid.sendTestMessage(testMessageMedRequiredFelter())
        behovløser.packet.inntektsId() shouldBe null
    }

    @Test
    fun `Skal mappe aktørId`() {
        val behovløser = OnPacketTestListener(testRapid)

        testRapid.sendTestMessage(testMessageMedRequiredFelter(mapOf(AKTØRID to "aktørId")))
        behovløser.packet.aktørId() shouldBe "aktørId"

        testRapid.sendTestMessage(testMessageMedRequiredFelter())
        behovløser.packet.aktørId() shouldBe null
    }

    @Test
    fun `Skal mappe RegelKontekst`() {
        val behovløser = OnPacketTestListener(testRapid)
        val forventetRegelkontekst = RegelKontekst(id = "kontekstid", type = "kontekst type")
        testRapid.sendTestMessage(
            testMessageMedRequiredFelter(
                mapOf(
                    KONTEKST_TYPE to forventetRegelkontekst.type,
                    KONTEKST_ID to forventetRegelkontekst.id,
                ),
            ),
        )
        behovløser.packet.hentRegelkontekst() shouldBe forventetRegelkontekst

        testRapid.sendTestMessage(testMessageMedRequiredFelter(mapOf(KONTEKST_ID to "kontekstid")))
        behovløser.packet.hentRegelkontekst() shouldBe null

        testRapid.sendTestMessage(testMessageMedRequiredFelter(mapOf(KONTEKST_TYPE to "kontekst type")))
        behovløser.packet.hentRegelkontekst() shouldBe null

        testRapid.sendTestMessage(testMessageMedRequiredFelter())
        behovløser.packet.hentRegelkontekst() shouldBe null
    }

    private fun testMessageMedRequiredFelter(ekstraFelter: Map<String, Any> = emptyMap()) =
        JsonMessage
            .newMessage(
                mapOf(BEHOV_ID to "behovId") + ekstraFelter,
            ).toJson()

    private class OnPacketTestListener(
        rapidsConnection: RapidsConnection,
    ) : River.PacketListener {
        var problems: MessageProblems? = null
        lateinit var packet: JsonMessage

        init {
            River(rapidsConnection)
                .apply(
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
