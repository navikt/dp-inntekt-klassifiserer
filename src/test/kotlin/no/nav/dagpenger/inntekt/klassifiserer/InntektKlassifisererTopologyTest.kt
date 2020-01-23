package no.nav.dagpenger.inntekt.klassifiserer

import io.mockk.every
import io.mockk.mockk
import no.finn.unleash.FakeUnleash
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.inntekt.v1.Aktør
import no.nav.dagpenger.events.inntekt.v1.AktørType
import no.nav.dagpenger.events.inntekt.v1.InntektId
import no.nav.dagpenger.events.inntekt.v1.SpesifisertInntekt
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Properties

class InntektKlassifisererTopologyTest {

    companion object {

        val factory = ConsumerRecordFactory<String, Packet>(
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.serializer(),
            Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.serializer()
        )

        val config = Properties().apply {
            this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
            this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        }
    }

    @Test
    fun `Do not process packets with klassifisert inntekt`() {
        val packetWithKlassifisertInntekt = """
            {
                "spesifisertInntektV1" : "something",
                "inntektV1" : "something"
            }
        """.trimIndent()

        val app = App(configuration = Configuration(), spesifisertInntektHttpClient = mockk(), unleash = FakeUnleash())
        TopologyTestDriver(app.buildTopology(), config).use { topologyTestDriver ->
            val input = factory.create(Packet(packetWithKlassifisertInntekt))
            topologyTestDriver.pipeInput(input)

            val emptyResult = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            assertNull(emptyResult)
        }
    }

    @Test
    fun `Only process packets when feature toggle is on`() {
        val packetWithSpesifisertInntektJson = """
            {
                "aktørId": "12345",
                "vedtakId": 123,
                "beregningsDato": 2019-01-25
           }
        """.trimIndent()

        val packetWithoutSpesifisertInntekt = """
            {
                "something" : "something"
            }
        """.trimIndent()

        val spesifisertInntektMock: SpesifisertInntektHttpClient = mockk()
        every {
            spesifisertInntektMock.getSpesifisertInntekt(
                "12345",
                123,
                LocalDate.of(2019, 1, 25)
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

        val unleash = FakeUnleash()
        val app = App(
            configuration = Configuration(),
            spesifisertInntektHttpClient = spesifisertInntektMock,
            unleash = unleash
        )
        TopologyTestDriver(app.buildTopology(), config).use { topologyTestDriver ->
            unleash.enable("dp.ny-klassifisering")
            val inputWithSpesfisertInntekt = factory.create(Packet(packetWithSpesifisertInntektJson))
            topologyTestDriver.pipeInput(inputWithSpesfisertInntekt)

            val processedOutput = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            Assertions.assertTrue { processedOutput != null }
            Assertions.assertTrue(processedOutput.value().hasField("inntektV1"))

            unleash.disable("dp.ny-klassifisering")
            val inputWithoutSpesfisertInntekt = factory.create(Packet(packetWithoutSpesifisertInntekt))
            topologyTestDriver.pipeInput(inputWithoutSpesfisertInntekt)

            val emptyResult = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            assertNull(emptyResult)
        }
    }
}
