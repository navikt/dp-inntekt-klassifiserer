package no.nav.dagpenger.inntekt.klassifiserer

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.inntekt.apikey.ApiKeyVerifier
import no.nav.dagpenger.inntekt.rpc.InntektHenterWrapper
import no.nav.dagpenger.streams.KafkaAivenCredentials
import no.nav.dagpenger.streams.River
import no.nav.dagpenger.streams.Topic
import no.nav.dagpenger.streams.streamConfigAiven
import org.apache.kafka.streams.kstream.Predicate
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

private val logger = KotlinLogging.logger { }
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class Application(
    private val configuration: Configuration = Configuration,
    private val inntektClient: InntektClient,
    topic: Topic<String, Packet> = configuration.kafka.regelTopic,
) : River(topic) {
    @Suppress("ktlint:standard:property-naming")
    override val SERVICE_APP_ID: String = configuration.applicationConfig.id

    companion object {
        const val INNTEKT = "inntektV1"
        const val AKTØRID = "aktørId"
        const val MANUELT_GRUNNLAG = "manueltGrunnlag"
        const val FORRIGE_GRUNNLAG = "forrigeGrunnlag"
        const val BEREGNINGSDATO = "beregningsDato"
        const val INNTEKTS_ID = "inntektsId"
        const val KONTEKST_ID = "kontekstId"
        const val KONTEKST_TYPE = "kontekstType"
    }

    override fun getConfig(): Properties {
        return streamConfigAiven(
            appId = SERVICE_APP_ID,
            bootStapServerUrl = configuration.kafka.aivenBrokers,
            aivenCredentials = KafkaAivenCredentials(),
        )
    }

    override fun filterPredicates(): List<Predicate<String, Packet>> {
        return listOf(
            Predicate { _, packet -> !packet.hasField(INNTEKT) },
            Predicate { _, packet -> !packet.hasField(MANUELT_GRUNNLAG) },
            Predicate { _, packet -> !packet.hasField(FORRIGE_GRUNNLAG) },
        )
    }

    override fun onPacket(packet: Packet): Packet {
        try {
            val behovId = packet.getNullableStringValue("behovId")
            val callId = (behovId ?: UUID.randomUUID()).toString()
            val started: LocalDateTime? =
                packet.getNullableStringValue("system_started")
                    ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
            val inntektsId = packet.getNullableStringValue(INNTEKTS_ID)
            val regelkontekst = runCatching { packet.hentRegelkontekst() }.getOrNull()
            val beregningsDato = packet.getLocalDate(BEREGNINGSDATO)

            withLoggingContext(
                "callId" to callId,
                "behovId" to behovId,
                "kontekstType" to regelkontekst?.type,
                "kontekstId" to regelkontekst?.id,
            ) {
                if (started?.isBefore(LocalDateTime.now().minusSeconds(30)) == true) {
                    throw RuntimeException("Denne pakka er for gammal!")
                }
                val klassifisertInntekt =
                    when (inntektsId) {
                        is String -> {
                            logger.info { "Henter inntekt basert på inntektsId: $inntektsId" }
                            runBlocking { inntektClient.getKlassifisertInntekt(inntektsId, callId) }
                        }

                        else -> {
                            val aktørId = packet.getStringValue(AKTØRID)
                            requireNotNull(regelkontekst) { "Må ha en kontekst for å hente inntekt" }

                            try {
                                runBlocking {
                                    inntektClient.getKlassifisertInntekt(
                                        aktørId,
                                        regelkontekst,
                                        beregningsDato,
                                        null,
                                        callId,
                                    )
                                }
                            } catch (e: InntektApiHttpClientException) {
                                logger.error(e) { "Kunne ikke hente inntekt fra dp-inntekt-api" }
                                packet.addProblem(e.problem)
                                return packet
                            }
                        }
                    }

                logger.info { "Hentet med inntektsId: ${klassifisertInntekt.inntektsId}" }
                packet.putValue(INNTEKT, klassifisertInntekt)
                return packet
            }
        } catch (e: Exception) {
            sikkerlogg.error("Denne feilen oppstod: ${e.message} med denne packeten ${packet.toJson()}")
            throw e
        }
    }
}

internal fun Packet.hentRegelkontekst() =
    RegelKontekst(
        id = this.hentKontekstId(),
        type = this.getStringValue(Application.KONTEKST_TYPE),
    )

private fun Packet.hentKontekstId(): String = getStringValue(Application.KONTEKST_ID)

fun main() {
    val apiKeyVerifier = ApiKeyVerifier(Configuration.applicationConfig.inntektApiSecret)
    val apiKey = apiKeyVerifier.generate(Configuration.applicationConfig.inntektApiKey)
    val inntektGrpcClient =
        InntektHenterWrapper(
            serveraddress = Configuration.applicationConfig.inntektGrpcAddress,
            apiKey = apiKey,
        )

    Runtime.getRuntime().addShutdownHook(
        Thread {
            inntektGrpcClient.close()
        },
    )
    val inntektHttpClient =
        InntektHttpClient(
            Configuration.applicationConfig.inntektApiUrl,
            tokenProvider = { Configuration.oauth2Client.clientCredentials(Configuration.dpInntektApiScope).accessToken },
        )

    val inntektClient =
        InntektClient(
            httpClient = inntektHttpClient,
            inntektHenter = inntektGrpcClient,
            unleash = Configuration.unleash,
        )
    Application(
        configuration = Configuration,
        inntektClient = inntektClient,
    ).start()
}
