package no.nav.syfo.kartleggingssporsmal.infrastructure.clients.vedtak14a

import java.time.LocalDate

data class Vedtak14aRequestDTO(
    val fnr: String,
)

data class Vedtak14aResponseDTO(
    val fattetDato: LocalDate,
    val innsatsgruppe: Innsatsgruppe,
    val hovedmal: Hovedmal?,
)

enum class Innsatsgruppe {
    GODE_MULIGHETER,
    TRENGER_VEILEDNING,
    TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
    JOBBE_DELVIS,
    LITEN_MULIGHET_TIL_A_JOBBE,
}

enum class Hovedmal {
    SKAFFE_ARBEID,
    BEHOLDE_ARBEID,
    OKE_DELTAKELSE,
}
