package no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto

enum class SaksType(val value: String) {
    GENERELL("GENERELL_SAK"),
}

data class Sak(
    val sakstype: String = SaksType.GENERELL.value,
)
