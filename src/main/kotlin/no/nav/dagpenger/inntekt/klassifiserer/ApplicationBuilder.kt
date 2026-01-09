package no.nav.dagpenger.inntekt.klassifiserer

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.helse.rapids_rivers.RapidApplication

internal class ApplicationBuilder(
    config: Map<String, String>,
) : RapidsConnection.StatusListener {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val rapidsConnection = RapidApplication.create(env = config)

    private val inntektClient =
        InntektHttpClient(
            Config.inntektApiUrl,
            tokenProvider = {
                Config.oauth2Client.clientCredentials(Config.dpInntektApiScope).access_token
                    ?: throw RuntimeException("Fant ikke token")
            },
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
