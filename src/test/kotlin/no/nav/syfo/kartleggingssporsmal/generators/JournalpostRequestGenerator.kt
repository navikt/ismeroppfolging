package no.nav.syfo.kartleggingssporsmal.generators

import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.AvsenderMottaker
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.BrevkodeType
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.Bruker
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.BrukerIdType
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.Dokument
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.Dokumentvariant
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.FiltypeType
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.JournalpostRequest
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.JournalpostType
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.OverstyrInnsynsregler
import no.nav.syfo.kartleggingssporsmal.infrastructure.clients.dokarkiv.dto.VariantformatType
import no.nav.syfo.shared.domain.Personident
import java.util.UUID

fun generateJournalpostRequest(
    tittel: String,
    brevkodeType: BrevkodeType,
    pdf: ByteArray,
    eksternReferanse: UUID,
    mottakerPersonident: Personident,
    mottakerNavn: String,
    brukerPersonident: Personident,
    overstyrInnsynsregler: OverstyrInnsynsregler? = null,
) = JournalpostRequest(
    avsenderMottaker = AvsenderMottaker.create(
        id = mottakerPersonident.value,
        idType = BrukerIdType.PERSON_IDENT,
        navn = mottakerNavn,
    ),
    bruker = Bruker.create(
        id = brukerPersonident.value,
        idType = BrukerIdType.PERSON_IDENT
    ),
    tittel = tittel,
    dokumenter = listOf(
        Dokument.create(
            brevkode = brevkodeType,
            tittel = tittel,
            dokumentvarianter = listOf(
                Dokumentvariant.create(
                    filnavn = tittel,
                    filtype = FiltypeType.PDFA,
                    fysiskDokument = pdf,
                    variantformat = VariantformatType.ARKIV,
                )
            ),
        )
    ),
    overstyrInnsynsregler = overstyrInnsynsregler?.name,
    journalpostType = JournalpostType.UTGAAENDE.name,
    eksternReferanseId = eksternReferanse.toString(),
)
