package no.nav.dagpenger.inntekt.klassifiserer

import no.nav.dagpenger.events.inntekt.v1.InntektKlasse
import no.nav.dagpenger.events.inntekt.v1.Postering

data class KlassifisertPostering(val postering: Postering, val inntektKlasse: InntektKlasse)
