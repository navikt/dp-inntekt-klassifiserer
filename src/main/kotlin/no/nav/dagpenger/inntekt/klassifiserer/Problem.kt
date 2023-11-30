package no.nav.dagpenger.inntekt.klassifiserer

import no.nav.dagpenger.events.Problem

internal fun Problem.toMap(): Map<String, Any?> {
    return mapOf(
        "type" to this.type.toString(),
        "title" to this.title,
        "status" to this.status,
        "instance" to this.instance.toString(),
        "detail" to this.detail,
    )
}
