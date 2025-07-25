package no.nav.syfo.senoppfolging.api.model

import no.nav.syfo.senoppfolging.domain.OnskerOppfolging
import no.nav.syfo.senoppfolging.domain.SenOppfolgingKandidat
import no.nav.syfo.senoppfolging.domain.SenOppfolgingStatus
import no.nav.syfo.senoppfolging.domain.VurderingType
import java.time.LocalDateTime
import java.util.*

data class SenOppfolgingKandidatResponseDTO(
    val uuid: UUID,
    val createdAt: LocalDateTime,
    val personident: String,
    val status: SenOppfolgingStatus,
    val varselAt: LocalDateTime?,
    val svar: SvarResponseDTO?,
    val vurderinger: List<SenOppfolgingVurderingResponseDTO>,
)

data class SenOppfolgingVurderingResponseDTO(
    val uuid: UUID,
    val begrunnelse: String?,
    val type: VurderingType,
    val veilederident: String,
    val createdAt: LocalDateTime,
)

data class SvarResponseDTO(
    val svarAt: LocalDateTime,
    val onskerOppfolging: OnskerOppfolging,
)

fun SenOppfolgingKandidat.toResponseDTO(): SenOppfolgingKandidatResponseDTO = SenOppfolgingKandidatResponseDTO(
    uuid = this.uuid,
    createdAt = this.createdAt.toLocalDateTime(),
    personident = this.personident.value,
    status = this.status,
    varselAt = this.varselAt?.toLocalDateTime(),
    svar = this.svar?.let {
        SvarResponseDTO(
            svarAt = it.svarAt.toLocalDateTime(),
            onskerOppfolging = it.onskerOppfolging,
        )
    },
    vurderinger = this.vurdering?.let {
        listOf(
            SenOppfolgingVurderingResponseDTO(
                uuid = it.uuid,
                begrunnelse = it.begrunnelse,
                type = it.type,
                veilederident = it.veilederident,
                createdAt = it.createdAt.toLocalDateTime(),
            )
        )
    } ?: emptyList()
)
