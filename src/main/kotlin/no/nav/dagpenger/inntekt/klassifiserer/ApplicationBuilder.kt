package no.nav.dagpenger.inntekt.klassifiserer

import mu.KotlinLogging
import no.nav.dagpenger.inntekt.apikey.ApiKeyVerifier
import no.nav.dagpenger.inntekt.rpc.InntektHenterWrapper
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

internal class ApplicationBuilder(config: Map<String, String>) : RapidsConnection.StatusListener {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val rapidsConnection =
        RapidApplication.Builder(
            RapidApplication.RapidApplicationConfig.fromEnv(config),
        ).build()

//    val apiKeyVerifier = ApiKeyVerifier(Configuration.applicationConfig.inntektApiSecret)
//    val apiKeyVerifier = ApiKeyVerifier(Config.innteckApiSecret())
    val apiKey = apiKeyVerifier.generate(Configuration.applicationConfig.inntektApiKey)
    val inntektGrpcClient =
        InntektHenterWrapper(
            serveraddress = Configuration.applicationConfig.inntektGrpcAddress,
            apiKey = apiKey,
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

    init {
        rapidsConnection.register(this)
        InntektBehovløser(rapidsConnection, inntektClient)
    }

    fun start() = rapidsConnection.start()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        logger.info { "Starter opp dp-regel-grunnlag" }
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        inntektGrpcClient.close()
    }
}
