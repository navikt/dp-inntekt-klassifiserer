package no.nav.dagpenger.inntekt.klassifiserer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Properties
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.inntekt.v1.InntektKlasse
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

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

        private val inntekt = Inntekt(
            inntektsId = "inntektsid",
            manueltRedigert = false,
            inntektsListe = listOf(
                KlassifisertInntektMåned(
                    årMåned = YearMonth.now(),
                    klassifiserteInntekter = listOf(
                        KlassifisertInntekt(
                            beløp = BigDecimal("1000.12"),
                            inntektKlasse = InntektKlasse.ARBEIDSINNTEKT
                        )
                    ),
                    harAvvik = false
                )
            ),
            sisteAvsluttendeKalenderMåned = YearMonth.now()
        )
    }

    @Test
    fun `Do not process packets with klassifisert inntekt`() {
        val packetWithKlassifisertInntekt = """
            {
                "spesifisertInntektV1" : "something",
                "inntektV1" : "something"
            }
        """.trimIndent()

        val app = Application(
            configuration = Configuration(),
            inntektKlassifiserer = mockk(),
            healthCheck = mockk(relaxed = true)
        )

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
    fun `Skal legge på inntekt på pakka `() {
        val packetWithSpesifisertInntektJson = """
            {
                "aktørId": "12345",
                "vedtakId": 123,
                "beregningsDato": 2019-01-25
           }
        """.trimIndent()

        val inntektKlassifiserer: InntektKlassifiserer = mockk()
        every {
            inntektKlassifiserer.getInntekt(
                "12345",
                "123",
                LocalDate.of(2019, 1, 25),
                null
            )
        } returns inntekt

        val app = Application(
            configuration = Configuration(),
            inntektKlassifiserer = inntektKlassifiserer,
            healthCheck = mockk(relaxed = true)
        )
        TopologyTestDriver(app.buildTopology(), config).use { topologyTestDriver ->

            val inputWithSpesfisertInntekt = factory.create(Packet(packetWithSpesifisertInntektJson))
            topologyTestDriver.pipeInput(inputWithSpesfisertInntekt)

            val processedOutput = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            Assertions.assertTrue { processedOutput != null }
            Assertions.assertTrue(processedOutput.value().hasField("inntektV1"))
        }
    }

    @Test
    fun `Skal ikke behandle pakker over 30 sekunder`() {
        val packetWithSpesifisertInntektJson = """
            {
                "system_started": "2020-03-28T12:35:53.082955",
                "aktørId": "12345",
                "vedtakId": 123,
                "beregningsDato": 2019-01-25
           }
        """.trimIndent()

        val inntektKlassifiserer: InntektKlassifiserer = mockk()
        every {
            inntektKlassifiserer.getInntekt(
                "12345",
                "123",
                LocalDate.of(2019, 1, 25),
                null
            )
        } returns inntekt

        val app = Application(
            configuration = Configuration(),
            inntektKlassifiserer = inntektKlassifiserer,
            healthCheck = mockk(relaxed = true)
        )
        TopologyTestDriver(app.buildTopology(), config).use { topologyTestDriver ->

            val inputWithSpesfisertInntekt = factory.create(Packet(packetWithSpesifisertInntektJson))
            topologyTestDriver.pipeInput(inputWithSpesfisertInntekt)

            val processedOutput = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            Assertions.assertTrue { processedOutput != null }
            processedOutput.value().hasProblem() shouldBe true
        }
    }
}
