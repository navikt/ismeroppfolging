package no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto

enum class BrevkodeType(
    val value: String,
) {
    VARSEL_KARTLEGGINGSSPORSMAL("OPPF_VARSEL_KARTLEGGINGSSPORSMAL"),
}

data class Dokument private constructor(
    val brevkode: String,
    val dokumentKategori: String? = null,
    val dokumentvarianter: List<Dokumentvariant>,
    val tittel: String? = null,
) {
    companion object {
        fun create(
            brevkode: BrevkodeType,
            dokumentvarianter: List<Dokumentvariant>,
            tittel: String? = null,
        ) = Dokument(
            brevkode = brevkode.value,
            dokumentvarianter = dokumentvarianter,
            tittel = tittel,
        )
    }
}
