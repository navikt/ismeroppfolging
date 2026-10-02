package no.nav.syfo.kartleggingssporsmal.domain

import java.util.UUID

data class PersonidentOppdatering(
    val kandidatUuids: List<UUID>,
    val stoppunktUuids: List<UUID>,
) {
    fun isEmpty() = kandidatUuids.isEmpty() && stoppunktUuids.isEmpty()
}
