package no.nav.syfo.infrastructure.clients.pdfgen

import no.nav.syfo.shared.domain.Personident
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

sealed class PdfModel private constructor(
    val mottakerFodselsnummer: String?,
    val mottakerNavn: String,
    val datoSendt: String,
) {
    private constructor(
        mottakerFodselsnummer: String?,
        mottakerNavn: String,
        datoSendt: LocalDate
    ) : this(
        mottakerFodselsnummer = mottakerFodselsnummer,
        mottakerNavn = mottakerNavn,
        datoSendt = datoSendt.format(formatter),
    )

    class KartleggingPdfModel(
        mottakerFodselsnummer: Personident,
        mottakerNavn: String,
        datoSendt: LocalDate = LocalDate.now(),
    ) : PdfModel(
        mottakerFodselsnummer = mottakerFodselsnummer.value,
        mottakerNavn = mottakerNavn,
        datoSendt = datoSendt,
    )

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale("no", "NO"))
    }
}
