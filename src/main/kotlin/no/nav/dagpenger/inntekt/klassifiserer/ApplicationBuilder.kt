package no.nav.dagpenger.inntekt.klassifiserer

import mu.KotlinLogging
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

    private val inntektClient =
        InntektHttpClient(
            Config.inntektApiUrl,
            tokenProvider = { Config.oauth2Client.clientCredentials(Config.dpInntektApiScope).accessToken },
        )

    init {
        rapidsConnection.register(this)
        InntektBehovløser(rapidsConnection, inntektClient)
    }

    fun start() = rapidsConnection.start()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        logger.info { "Starter opp dp-inntekt-klassifiserer" }
    }
}
