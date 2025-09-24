package no.nav.syfo.kartleggingssporsmal.api.model

import no.nav.syfo.kartleggingssporsmal.domain.KandidatStatus
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat

data class PersonDTO private constructor(
    val kandidatStatus: KandidatStatus,
    val kandidat: KandidatDTO?,
) {
    constructor(
        kandidat: KartleggingssporsmalKandidat,
        hasReceivedQuestions: Boolean
    ) : this(
        kandidatStatus = kandidat.status,
        kandidat = KandidatDTO(
            hasReceivedQuestions = hasReceivedQuestions,
        ),
    )

    companion object {
        val IKKE_KANDIDAT = PersonDTO(KandidatStatus.IKKE_KANDIDAT, null)
    }
}

data class KandidatDTO(
    val hasReceivedQuestions: Boolean,
)
