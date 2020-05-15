package no.nav.dagpenger.inntekt.klassifiserer

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.moshi.moshiDeserializerOf
import com.squareup.moshi.JsonAdapter
import java.net.URI
import java.time.LocalDate
import no.nav.dagpenger.events.Problem
import no.nav.dagpenger.events.inntekt.v1.Inntekt
import no.nav.dagpenger.events.inntekt.v1.SpesifisertInntekt
import no.nav.dagpenger.events.moshiInstance

class InntektHttpClient(private val inntektApiUrl: String, private val apiKey: String) {

    companion object {
        val spesifisertInntektJsonAdapter: JsonAdapter<SpesifisertInntekt> = moshiInstance.adapter(SpesifisertInntekt::class.java)
        val klassifisertInntektJsonAdapter: JsonAdapter<Inntekt> = moshiInstance.adapter(Inntekt::class.java)
    }

    private val jsonRequestRequestAdapter = moshiInstance.adapter(InntektRequest::class.java)
    private val problemAdapter = moshiInstance.adapter(Problem::class.java)!!

    fun getSpesifisertInntekt(
        aktørId: String,
        vedtakId: String,
        beregningsDato: LocalDate,
        fødselsnummer: String?
    ): SpesifisertInntekt = getInntekt(
        aktørId = aktørId,
        vedtakId = vedtakId,
        beregningsDato = beregningsDato,
        fødselsnummer = fødselsnummer,
        url = "${inntektApiUrl}v1/inntekt/spesifisert",
        adapter = spesifisertInntektJsonAdapter
    )

    private inline fun <reified T : Any> getInntekt(
        aktørId: String,
        vedtakId: String,
        beregningsDato: LocalDate,
        fødselsnummer: String?,
        url: String,
        adapter: JsonAdapter<T>
    ): T {

        val requestBody = InntektRequest(
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
            responseObject(moshiDeserializerOf(adapter))
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

    fun getKlassifisertInntekt(
        aktørId: String,
        vedtakId: String,
        beregningsDato: LocalDate,
        fødselsnummer: String?
    ): Inntekt = getInntekt(
        aktørId = aktørId,
        vedtakId = vedtakId,
        beregningsDato = beregningsDato,
        fødselsnummer = fødselsnummer,
        url = "${inntektApiUrl}v1/inntekt/klassifisert",
        adapter = klassifisertInntektJsonAdapter
    )
}

private data class InntektRequest(
    val aktørId: String,
    val fødselsnummer: String? = null,
    val vedtakId: String,
    val beregningsDato: LocalDate
)

class InntektApiHttpClientException(override val message: String, val problem: Problem, override val cause: Throwable) :
    RuntimeException(message, cause)
