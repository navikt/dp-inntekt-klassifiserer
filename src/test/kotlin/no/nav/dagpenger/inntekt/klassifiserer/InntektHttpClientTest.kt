package no.nav.dagpenger.inntekt.klassifiserer

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.inntekt.v1.InntektKlasse
import no.nav.dagpenger.events.inntekt.v1.sumInntekt
import no.nav.dagpenger.events.moshiInstance
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InntektHttpClientTest {
    companion object {
        private val tokenProvider = { "token" }
        private val client = httpClient(httpMetricsBasename = "test")
        val klassifisertInntektJsonAdapter: JsonAdapter<Inntekt> = moshiInstance.adapter(Inntekt::class.java)
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
    }

    @Test
    fun `fetch klassifisert inntekt on 200 ok with fødselsnummer`() {
        val responseBodyJson =
            InntektHttpClientTest::class.java
                .getResource("/test-data/example-klassifisert-inntekt-payload.json")!!.readText()

        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/v2/inntekt/klassifisert"))
                .withHeader("Authorization", EqualToPattern("Bearer token"))
                .withRequestBody(
                    matchingJsonPath("aktørId", equalTo("45456")),
                )
                .withRequestBody(
                    matchingJsonPath("regelkontekst.id", equalTo("123")),
                )
                .withRequestBody(
                    matchingJsonPath("regelkontekst.type", equalTo("vedtak")),
                )
                .withRequestBody(
                    matchingJsonPath("beregningsDato", matching("^\\d{4}-\\d{2}-\\d{2}\$")),
                )
                .withRequestBody(
                    matchingJsonPath("fødselsnummer", equalTo("12345678901")),
                )
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBodyJson),
                ),
        )

        val inntektHttpClient = inntektHttpClient()
        val klassifisertInntekt: Inntekt =
            runBlocking {
                inntektHttpClient.getKlassifisertInntekt(
                    "45456",
                    RegelKontekst("123", "vedtak"),
                    LocalDate.now(),
                    "12345678901",
                )
            }
        val moshiSerialisertInntekt = klassifisertInntektJsonAdapter.fromJson(responseBodyJson)!!
        assertEquals(klassifisertInntekt.inntektsId, moshiSerialisertInntekt.inntektsId)
        assertEquals(klassifisertInntekt.inntektsListe, moshiSerialisertInntekt.inntektsListe)
        assertEquals(klassifisertInntekt.manueltRedigert, moshiSerialisertInntekt.manueltRedigert)
        assertEquals(
            klassifisertInntekt.sisteAvsluttendeKalenderMåned,
            moshiSerialisertInntekt.sisteAvsluttendeKalenderMåned,
        )
        assertEquals(
            klassifisertInntekt.inntektsListe.sumInntekt(enumValues<InntektKlasse>().toList()),
            moshiSerialisertInntekt.inntektsListe.sumInntekt(enumValues<InntektKlasse>().toList()),
        )

        assertEquals("12345", klassifisertInntekt.inntektsId)
        assertEquals(2, klassifisertInntekt.inntektsListe.size)
    }

    @Test
    fun `fetch klassifisert inntekt fails on 500 server error`() {
        //language=JSON
        val responseBodyJson =
            """
            { 
                "type": "urn:dp:error:inntektskomponenten",
                "title": "Klarte ikke å hente inntekt for beregningen",
                "status": 500,
                "detail": "Innhenting av inntekt mot inntektskomponenten feilet."
            }
            """.trimIndent()
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/v2/inntekt/klassifisert"))
                .withHeader("Authorization", EqualToPattern("Bearer token"))
                .willReturn(
                    WireMock.serverError()
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBodyJson),
                ),
        )
        val inntektHttpClient =
            inntektHttpClient()
        val inntektApiHttpClientException =
            assertFailsWith<InntektApiHttpClientException> {
                runBlocking {
                    inntektHttpClient.getKlassifisertInntekt(
                        "",
                        RegelKontekst("123", "vedtak"),
                        LocalDate.now(),
                        null,
                    )
                }
            }
        val problem = inntektApiHttpClientException.problem
        assertEquals("urn:dp:error:inntektskomponenten", problem.type.toASCIIString())
        assertEquals("Klarte ikke å hente inntekt for beregningen", problem.title)
        assertEquals(500, problem.status)
        assertEquals("Innhenting av inntekt mot inntektskomponenten feilet.", problem.detail)
    }

    @Test
    fun `hente inntekt basert på inntekt ID`() =
        runBlocking {
            val responseBodyJson =
                InntektHttpClientTest::class.java
                    .getResource("/test-data/example-klassifisert-inntekt-payload.json")!!.readText()

            WireMock.stubFor(
                WireMock.get(WireMock.urlEqualTo("/v2/inntekt/klassifisert/12345"))
                    .withHeader("Authorization", EqualToPattern("Bearer token"))
                    .willReturn(
                        WireMock.aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(responseBodyJson),
                    ),
            )
            val inntektHttpClient = inntektHttpClient()
            val klassifisertInntekt = inntektHttpClient.getKlassifisertInntekt("12345", "callid")
            assertEquals("12345", klassifisertInntekt.inntektsId)
            assertEquals(2, klassifisertInntekt.inntektsListe.size)
        }

    @Test
    fun `fetch spesifisert inntekt fails on error and no body`() {
        WireMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/v2/inntekt/klassifisert"))
                .willReturn(
                    WireMock.serviceUnavailable(),
                ),
        )
        val inntektHttpClient =
            inntektHttpClient()
        val inntektApiHttpClientException =
            assertFailsWith<InntektApiHttpClientException> {
                runBlocking {
                    inntektHttpClient.getKlassifisertInntekt(
                        "",
                        RegelKontekst("123", "vedtak"),
                        LocalDate.now(),
                        null,
                    )
                }
            }
        val problem = inntektApiHttpClientException.problem
        assertEquals("urn:dp:error:inntektskomponenten", problem.type.toASCIIString())
        assertEquals("Klarte ikke å hente inntekt", problem.title)
        assertEquals(500, problem.status)
    }

    private fun inntektHttpClient() =
        InntektHttpClient(
            inntektApiUrl = server.url(""),
            httpKlient = client,
            tokenProvider = tokenProvider,
        )
}
