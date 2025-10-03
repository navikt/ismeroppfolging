package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.kartleggingssporsmalsvar

import java.time.OffsetDateTime
import java.util.*

data class KafkaKartleggingssporsmalSvarDTO(
    val personident: String,
    val kandidatId: UUID,
    val svarId: UUID,
    val createdAt: OffsetDateTime,
)
