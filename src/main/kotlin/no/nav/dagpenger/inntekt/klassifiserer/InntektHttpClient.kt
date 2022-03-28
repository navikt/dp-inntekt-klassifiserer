package no.nav.dagpenger.inntekt.klassifiserer

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.ResponseException
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.inntekt.v1.SpesifisertInntekt
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetrics
import java.net.URI
import java.time.Duration
import java.time.LocalDate

internal class InntektHttpClient(
    private val inntektApiUrl: String,
    private val httpKlient: HttpClient = httpClient(httpMetricsBasename = "ktor_client_inntekt_klassifiserer_metrics"),
    private val tokenProvider: () -> String,
) {

    suspend fun getSpesifisertInntekt(
        aktørId: String,
        regelkontekst: RegelKontekst,
        beregningsDato: LocalDate,
        fødselsnummer: String?
    ): SpesifisertInntekt {
        return getInntekt(
            aktørId,
            regelkontekst,
            beregningsDato,
            fødselsnummer,
            url = "${inntektApiUrl}v2/inntekt/spesifisert"
        )
    }

    suspend fun getKlassifisertInntekt(
        aktørId: String,
        regelkontekst: RegelKontekst,
        beregningsDato: LocalDate,
        fødselsnummer: String?
    ): Inntekt {
        return getInntekt(
            aktørId,
            regelkontekst,
            beregningsDato,
            fødselsnummer,
            url = "${inntektApiUrl}v2/inntekt/klassifisert"
        )
    }

    private suspend inline fun <reified T : Any> getInntekt(
        aktørId: String,
        regelkontekst: RegelKontekst,
        beregningsDato: LocalDate,
        fødselsnummer: String?,
        url: String,
    ): T {
        val requestBody = InntektRequest(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            regelkontekst = regelkontekst,
            beregningsDato = beregningsDato
        )

        return try {

            httpKlient.post(url) {
                header("Content-Type", "application/json")
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                body = requestBody
                accept(ContentType.Application.Json)
            }
        } catch (error: ResponseException) {
            val problem = kotlin.runCatching { objectMapper.readValue(error.response.readText(), Problem::class.java) }
                .getOrDefault(
                    Problem(
                        URI.create("urn:dp:error:inntektskomponenten"),
                        "Klarte ikke å hente inntekt"
                    )
                )

            throw InntektApiHttpClientException(
                "Failed to fetch inntekt. Problem: ${problem.title}. Response code: ${error.response.status}, message: ${error.response.readText()}",
                problem,
                error
            )
        }
    }
}

private data class InntektRequest(
    val aktørId: String,
    val fødselsnummer: String? = null,
    val regelkontekst: RegelKontekst,
    val beregningsDato: LocalDate
)

class InntektApiHttpClientException(override val message: String, val problem: Problem, override val cause: Throwable) :
    RuntimeException(message, cause)

internal fun httpClient(
    engine: HttpClientEngine = CIO.create { requestTimeout = Long.MAX_VALUE },
    httpMetricsBasename: String? = null
): HttpClient {
    return HttpClient(engine) {
        install(HttpTimeout) {
            connectTimeoutMillis = Duration.ofSeconds(30).toMillis()
            requestTimeoutMillis = Duration.ofSeconds(30).toMillis()
            socketTimeoutMillis = Duration.ofSeconds(30).toMillis()
        }

        install(JsonFeature) {
            accept(ContentType.Application.Json)
            serializer = JacksonSerializer(objectMapper)
        }

        install(PrometheusMetrics) {
            httpMetricsBasename?.let {
                baseName = it
            }
        }
    }
}

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
