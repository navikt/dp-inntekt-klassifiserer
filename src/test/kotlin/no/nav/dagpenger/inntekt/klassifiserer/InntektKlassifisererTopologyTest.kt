package no.nav.dagpenger.inntekt.klassifiserer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.Properties
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.inntekt.v1.InntektKlasse
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.events.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.inntekt.rpc.InntektHenter
import no.nav.dagpenger.streams.Topics
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `Should add klassifisert inntekt by inntektsId to packet`() {
        val inntektHenter = mockk<InntektHenter>(relaxed = true)
        every { runBlocking { inntektHenter.hentKlassifisertInntekt("ULID") } } returns inntekt
        val inntektHttpClient = mockk<InntektHttpClient>()

        val app = Application(
            configuration = Configuration(),
            inntektHttpClient = inntektHttpClient,
            healthCheck = mockk(relaxed = true),
            inntektHenter = inntektHenter
        )

        val packetJson = """
            {
                "aktørId": "12345",
                "vedtakId": 123,
                "beregningsDato": 2019-01-25,
                "inntektsId": "ULID",
                "otherField": "should be unchanged"
            }
        """.trimIndent()

        TopologyTestDriver(app.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create(Packet(packetJson))
            topologyTestDriver.pipeInput(inputRecord)
            val ut = topologyTestDriver.readOutput(
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.name,
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.keySerde.deserializer(),
                Topics.DAGPENGER_BEHOV_PACKET_EVENT.valueSerde.deserializer()
            )

            assertTrue { ut != null }
            assertTrue(ut.value().hasField("inntektV1"))

            assertEquals("12345", ut.value().getStringValue("aktørId"))
            assertEquals(123, ut.value().getIntValue("vedtakId"))
            assertEquals(LocalDate.of(2019, 1, 25), ut.value().getLocalDate("beregningsDato"))
            assertEquals("ULID", ut.value().getStringValue("inntektsId"))
            assertEquals("should be unchanged", ut.value().getStringValue("otherField"))

            verify(exactly = 1) { runBlocking { inntektHenter.hentKlassifisertInntekt("ULID") } }
            verify(exactly = 0) { inntektHttpClient.getKlassifisertInntekt(any(), any(), any(), any()) }
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

        val app = Application(
            configuration = Configuration(),
            inntektHttpClient = mockk(),
            healthCheck = mockk(relaxed = true),
            inntektHenter = mockk(relaxed = true)
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

        val inntektHttpClient: InntektHttpClient = mockk()
        every {
            inntektHttpClient.getKlassifisertInntekt(
                "12345",
                "123",
                LocalDate.of(2019, 1, 25),
                null
            )
        } returns inntekt

        val app = Application(
            configuration = Configuration(),
            inntektHttpClient = inntektHttpClient,
            healthCheck = mockk(relaxed = true),
            inntektHenter = mockk(relaxed = true)
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

        val inntektHttpClient: InntektHttpClient = mockk()
        every {
            inntektHttpClient.getKlassifisertInntekt(
                "12345",
                "123",
                LocalDate.of(2019, 1, 25),
                null
            )
        } returns inntekt

        val app = Application(
            configuration = Configuration(),
            inntektHttpClient = inntektHttpClient,
            healthCheck = mockk(relaxed = true),
            inntektHenter = mockk(relaxed = true)
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
