package no.nav.syfo.infrastructure.clients.pdfgen

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class PdfModel(
    val brevdata: BrevData,
) {
    constructor(
        datoSendt: LocalDate
    ) : this(
        brevdata = BrevData(
            createdAt = datoSendt.format(formatter),
        ),
    )

    companion object {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}

data class BrevData(
    val createdAt: String,
)
