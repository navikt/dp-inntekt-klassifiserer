package no.nav.dagpenger.inntekt.klassifiserer

internal fun Problem.toMap(): Map<String, Any?> =
    mapOf(
        "type" to this.type.toString(),
        "title" to this.title,
        "status" to this.status,
        "instance" to this.instance.toString(),
        "detail" to this.detail,
    )
