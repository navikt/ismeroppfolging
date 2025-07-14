package no.nav.syfo.senoppfolging.api.model

data class SenOppfolgingKandidaterRequestDTO(
    val personidenter: List<String>
)

data class SenOppfolgingKandidaterResponseDTO(
    val kandidater: Map<String, SenOppfolgingKandidatResponseDTO>
)
