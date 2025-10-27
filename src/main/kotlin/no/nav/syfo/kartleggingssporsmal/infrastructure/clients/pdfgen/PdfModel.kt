package no.nav.syfo.infrastructure.clients.pdfgen

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PdfModel(
    val createdAt: String,
) {
    constructor(
        datoSendt: LocalDate
    ) : this(
        createdAt = datoSendt.format(formatter),
    )

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
