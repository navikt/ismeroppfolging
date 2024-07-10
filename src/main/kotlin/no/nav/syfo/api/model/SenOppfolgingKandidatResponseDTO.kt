package no.nav.syfo.api.model

import no.nav.syfo.domain.SenOppfolgingKandidat
import no.nav.syfo.domain.SenOppfolgingStatus
import no.nav.syfo.domain.VurderingType
import java.time.LocalDateTime
import java.util.UUID

data class SenOppfolgingKandidatResponseDTO(
    val uuid: UUID,
    val createdAt: LocalDateTime,
    val personident: String,
    val status: SenOppfolgingStatus,
    val vurderinger: List<SenOppfolgingVurderingResponseDTO>,
)

data class SenOppfolgingVurderingResponseDTO(
    val uuid: UUID,
    val type: VurderingType,
    val veilederident: String,
    val createdAt: LocalDateTime,
)

fun SenOppfolgingKandidat.toResponseDTO(): SenOppfolgingKandidatResponseDTO = SenOppfolgingKandidatResponseDTO(
    uuid = this.uuid,
    createdAt = this.createdAt.toLocalDateTime(),
    personident = this.personident.value,
    status = this.status,
    vurderinger = this.vurderinger.map {
        SenOppfolgingVurderingResponseDTO(
            uuid = it.uuid,
            type = it.type,
            veilederident = it.veilederident,
            createdAt = it.createdAt.toLocalDateTime(),
        )
    }
)
