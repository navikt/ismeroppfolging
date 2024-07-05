package no.nav.syfo.api.model

import no.nav.syfo.domain.SenOppfolgingKandidat
import java.time.LocalDateTime
import java.util.UUID

data class SenOppfolgingKandidatResponseDTO(
    val uuid: UUID,
    val createdAt: LocalDateTime,
    val personident: String,
    val ferdigbehandlet: FerdigbehandletResponseDTO?,
)

data class FerdigbehandletResponseDTO(
    val veilederident: String,
    val createdAt: LocalDateTime,
)

fun SenOppfolgingKandidat.toResponseDTO(): SenOppfolgingKandidatResponseDTO = SenOppfolgingKandidatResponseDTO(
    uuid = this.uuid,
    createdAt = this.createdAt.toLocalDateTime(),
    personident = this.personident.value,
    ferdigbehandlet = this.getFerdigbehandletVurdering()?.let {
        FerdigbehandletResponseDTO(
            veilederident = it.veilederident,
            createdAt = it.createdAt.toLocalDateTime(),
        )
    },
)
