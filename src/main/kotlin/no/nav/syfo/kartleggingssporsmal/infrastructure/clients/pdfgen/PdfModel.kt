package no.nav.syfo.infrastructure.clients.pdfgen

import no.nav.syfo.kartleggingssporsmal.domain.Skjemavariant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class PdfModel(
    val brevdata: BrevData,
) {
    constructor(
        skjemavariant: Skjemavariant,
        datoSendt: LocalDate
    ) : this(
        brevdata = BrevData(
            skjemavariant = skjemavariant.name,
            createdAt = datoSendt.format(formatter),
        ),
    )

    companion object {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}

data class BrevData(
    val skjemavariant: String,
    val createdAt: String,
)
