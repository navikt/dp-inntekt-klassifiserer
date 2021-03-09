package no.nav.dagpenger.inntekt.klassifiserer

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.events.Packet
import no.nav.dagpenger.inntekt.rpc.InntektHenter
import no.nav.dagpenger.inntekt.rpc.InntektHenterWrapper
import no.nav.dagpenger.ktor.auth.ApiKeyVerifier
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.River
import no.nav.dagpenger.streams.streamConfig
import org.apache.kafka.streams.kstream.Predicate
import java.time.LocalDateTime
import java.util.Properties

private val logger = KotlinLogging.logger { }

class Application(
    private val configuration: Configuration,
    private val inntektHttpClient: InntektHttpClient,
    private val inntektHenter: InntektHenter
) : River(configuration.kafka.behovTopic) {

    override val SERVICE_APP_ID: String = configuration.applicationConfig.id

    companion object {
        const val INNTEKT = "inntektV1"
        const val AKTØRID = "aktørId"
        const val MANUELT_GRUNNLAG = "manueltGrunnlag"
        const val BEREGNINGSDATO = "beregningsDato"
        const val INNTEKTS_ID = "inntektsId"
        const val KONTEKST_ID = "kontekstId"
        const val KONTEKST_TYPE = "kontekstType"
    }

    override fun getConfig(): Properties {
        return streamConfig(
            SERVICE_APP_ID,
            configuration.kafka.bootstrapServer,
            KafkaCredential(configuration.kafka.username, configuration.kafka.password)
        )
    }

    override fun filterPredicates(): List<Predicate<String, Packet>> {
        return listOf(
            Predicate { _, packet -> !packet.hasField(INNTEKT) },
            Predicate { _, packet -> !packet.hasField(MANUELT_GRUNNLAG) }
        )
    }

    override fun onPacket(packet: Packet): Packet {
        val started: LocalDateTime? =
            packet.getNullableStringValue("system_started")
                ?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

        val inntektsId = packet.getNullableStringValue(INNTEKTS_ID)
        if (started?.isBefore(LocalDateTime.now().minusSeconds(30)) == true) {
            throw RuntimeException("Denne pakka er for gammal!")
        } else {

            val klassifisertInntekt = when (inntektsId) {
                is String -> {
                    logger.info { "Henter inntekt basert på inntektsId: $inntektsId" }
                    runBlocking { inntektHenter.hentKlassifisertInntekt(inntektsId) }
                }
                else -> {
                    val aktørId = packet.getStringValue(AKTØRID)
                    val regelkontekst = packet.hentRegelkontekst()
                    val beregningsDato = packet.getLocalDate(BEREGNINGSDATO)
                    inntektHttpClient.getKlassifisertInntekt(aktørId, regelkontekst, beregningsDato, null)
                }
            }

            logger.info { "Hentet med inntektsId: ${klassifisertInntekt.inntektsId}" }
            packet.putValue(INNTEKT, klassifisertInntekt)
            return packet
        }
    }
}

internal fun Packet.hentRegelkontekst() =
    RegelKontekst(
        id = this.hentKontekstId(),
        type = this.getStringValue(Application.KONTEKST_TYPE)
    )

private fun Packet.hentKontekstId(): String = getStringValue(Application.KONTEKST_ID)

fun main() {
    val configuration = Configuration()

    val apiKeyVerifier = ApiKeyVerifier(configuration.applicationConfig.inntektApiSecret)
    val apiKey = apiKeyVerifier.generate(configuration.applicationConfig.inntektApiKey)

    val inntektGrpcClient = InntektHenterWrapper(
        serveraddress = configuration.applicationConfig.inntektGrpcAddress,
        apiKey = apiKey
    )

    Runtime.getRuntime().addShutdownHook(
        Thread {
            inntektGrpcClient.close()
        }
    )

    val inntektHttpClient = InntektHttpClient(
        configuration.applicationConfig.inntektApiUrl,
        apiKey
    )

    Application(
        configuration = configuration,
        inntektHttpClient = inntektHttpClient,
        inntektHenter = inntektGrpcClient
    ).start()
}
