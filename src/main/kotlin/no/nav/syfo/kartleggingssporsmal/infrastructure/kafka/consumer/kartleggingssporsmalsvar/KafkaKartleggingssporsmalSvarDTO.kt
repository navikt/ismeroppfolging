package no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.consumer.kartleggingssporsmalsvar

import java.time.OffsetDateTime
import java.util.UUID

data class KafkaKartleggingssporsmalSvarDTO(
    val personident: String,
    val kandidatId: UUID,
    val svarId: UUID,
    val createdAt: OffsetDateTime,
)
