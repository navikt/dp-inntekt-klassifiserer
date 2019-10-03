package no.nav.dagpenger.inntekt.klassifiserer

import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
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
    fun `Only process packets with spesifisert inntekt`() {
        val packetWithSpesifisertInntektJson = """
            {
                "aktørId": "12345",
                "vedtakId": 123,
                "beregningsDato": 2019-01-25,
                "spesifisertInntektV1" : {
                    "inntektId" : "",
                    "avvik" : {},
                    "posteringer" : {},
                    "ident": {
                        "identifikator": "-1",
                        "aktoerType": "NATURLIG_IDENT"
                    },
                    "manueltRedigert" : false,
                    "timestamp" : "2019-06-12T13:59:13.233+0200"
                }
            }
        """.trimIndent()

        val packetWithoutSpesifisertInntekt = """
            {
                "something" : "something"
            }
        """.trimIndent()

        val app = App(Configuration())
        TopologyTestDriver(app.buildTopology(), config).use { topologyTestDriver ->
            val inputWithSpesfisertInntekt = factory.create(Packet(packetWithSpesifisertInntektJson))
            topologyTestDriver.pipeInput(inputWithSpesfisertInntekt)

            val processedOutput = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            Assertions.assertTrue { processedOutput != null }
            Assertions.assertTrue(processedOutput.value().hasField("spesifisertInntektV1"))

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

    @Test
    fun `Do not process packets with klassifisert inntekt`() {
        val packetWithKlassifisertInntekt = """
            {
                "spesifisertInntektV1" : "something",
                "inntektV1" : "something"
            }
        """.trimIndent()

        val app = App(Configuration())
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
}
