package no.nav.dagpenger.inntekt.klassifiserer

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.dagpenger.inntekt.v1.Inntekt
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetricsPlugin
import java.net.URI
import java.time.Duration
import java.time.LocalDate

internal class InntektHttpClient(
    private val inntektApiUrl: String,
    private val httpKlient: HttpClient = httpClient(httpMetricsBasename = "ktor_client_inntekt_klassifiserer_metrics"),
    private val tokenProvider: () -> String,
) {
    suspend fun getKlassifisertInntekt(
        aktørId: String,
        regelkontekst: RegelKontekst,
        beregningsDato: LocalDate,
        fødselsnummer: String?,
        callId: String? = null,
    ): Inntekt =
        getInntekt(
            aktørId,
            regelkontekst,
            beregningsDato,
            fødselsnummer,
            url = "${inntektApiUrl}v2/inntekt/klassifisert",
            callId,
        )

    suspend fun getKlassifisertInntekt(
        inntektId: String,
        callId: String,
    ): Inntekt =
        try {
            httpKlient
                .get("${inntektApiUrl}v2/inntekt/klassifisert/$inntektId") {
                    header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                    header(HttpHeaders.XRequestId, callId)
                    accept(ContentType.Application.Json)
                }.body<Inntekt>()
        } catch (error: ResponseException) {
            val problem = mapTilHttpProblem(error)
            throw InntektApiHttpClientException(
                """Failed to fetch inntekt. 
                    |Problem: ${problem.title}. 
                    |Response code: ${error.response.status}, 
                    |message: ${error.response.bodyAsText()}
                """.trimMargin(),
                problem,
                error,
            )
        }

    private suspend inline fun <reified T : Any> getInntekt(
        aktørId: String,
        regelkontekst: RegelKontekst,
        beregningsDato: LocalDate,
        fødselsnummer: String?,
        url: String,
        callId: String?,
    ): T {
        val requestBody =
            InntektRequest(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                regelkontekst = regelkontekst,
                beregningsDato = beregningsDato,
            )

        return try {
            httpKlient
                .post(url) {
                    header("Content-Type", "application/json")
                    header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                    header(HttpHeaders.XRequestId, callId)
                    setBody(requestBody)
                    accept(ContentType.Application.Json)
                }.body<T>()
        } catch (error: ResponseException) {
            val problem = mapTilHttpProblem(error)

            throw InntektApiHttpClientException(
                """Failed to fetch inntekt. 
                    |Problem: ${problem.title}. 
                    |Response code: ${error.response.status}, 
                    |message: ${error.response.bodyAsText()}
                """.trimMargin(),
                problem,
                error,
            )
        }
    }

    private suspend fun mapTilHttpProblem(error: ResponseException): Problem =
        kotlin
            .runCatching { objectMapper.readValue(error.response.bodyAsText(), Problem::class.java) }
            .getOrDefault<Problem, Problem>(
                Problem(
                    URI.create("urn:dp:error:inntektskomponenten"),
                    "Klarte ikke å hente inntekt",
                    detail = error.response.bodyAsText(),
                ),
            )
}

private data class InntektRequest(
    val aktørId: String,
    val fødselsnummer: String? = null,
    val regelkontekst: RegelKontekst,
    val beregningsDato: LocalDate,
)

class InntektApiHttpClientException(
    override val message: String,
    val problem: Problem,
    override val cause: Throwable,
) : RuntimeException(message, cause)

internal fun httpClient(
    engine: HttpClientEngine = CIO.create { requestTimeout = Long.MAX_VALUE },
    httpMetricsBasename: String? = null,
): HttpClient =
    HttpClient(engine) {
        expectSuccess = true
        install(HttpTimeout) {
            connectTimeoutMillis = Duration.ofSeconds(30).toMillis()
            requestTimeoutMillis = Duration.ofSeconds(30).toMillis()
            socketTimeoutMillis = Duration.ofSeconds(30).toMillis()
        }

        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }

        install(PrometheusMetricsPlugin) {
            httpMetricsBasename?.let {
                baseName = it
            }
        }
    }
