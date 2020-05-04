package no.nav.dagpenger.inntekt.klassifiserer

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.moshi.moshiDeserializerOf
import java.net.URI
import java.time.LocalDate
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.events.inntekt.v1.SpesifisertInntekt
import no.nav.dagpenger.events.moshiInstance

class SpesifisertInntektHttpClient(private val inntektApiUrl: String, private val apiKey: String) {

    private val jsonRequestRequestAdapter = moshiInstance.adapter(SpesifisertInntektRequest::class.java)
    private val problemAdapter = moshiInstance.adapter(Problem::class.java)!!

    fun getSpesifisertInntekt(
        aktørId: String,
        vedtakId: String,
        beregningsDato: LocalDate,
        fødselsnummer: String?
    ): SpesifisertInntekt {

        val url = "${inntektApiUrl}v1/inntekt/spesifisert"

        val requestBody = SpesifisertInntektRequest(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            vedtakId = vedtakId,
            beregningsDato = beregningsDato
        )
        val jsonBody = jsonRequestRequestAdapter.toJson(requestBody)

        val (_, response, result) = with(url.httpPost()) {
            header("Content-Type" to "application/json")
            header("X-API-KEY", apiKey)
            body(jsonBody)
            responseObject(moshiDeserializerOf(spesifisertInntektJsonAdapter))
        }

        return result.fold(
            { spesifisertInntekt ->
                spesifisertInntekt
            },
            { error ->
                val problem = runCatching {
                    problemAdapter.fromJson(response.body().asString("application/json"))!!
                }.getOrDefault(
                    Problem(
                        URI.create("urn:dp:error:inntektskomponenten"),
                        "Klarte ikke å hente inntekt"
                    )
                )

                throw InntektApiHttpClientException(
                    "Failed to fetch inntekt. Problem: ${problem.title}. Response code: ${response.statusCode}, message: ${response.responseMessage}",
                    problem,
                    error.exception
                )
            })
    }
}

private data class SpesifisertInntektRequest(
    val aktørId: String,
    val fødselsnummer: String? = null,
    val vedtakId: String,
    val beregningsDato: LocalDate
)

private fun String.toBearerToken() = "Bearer $this"

class InntektApiHttpClientException(override val message: String, val problem: Problem, override val cause: Throwable) :
    RuntimeException(message, cause)
