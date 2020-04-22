package no.nav.dagpenger.inntekt.klassifiserer

import com.squareup.moshi.JsonAdapter
import no.nav.dagpenger.events.inntekt.v1.SpesifisertInntekt
import no.nav.dagpenger.events.moshiInstance

val spesifisertInntektJsonAdapter: JsonAdapter<SpesifisertInntekt> = moshiInstance.adapter(SpesifisertInntekt::class.java)
