package no.nav.syfo.kartleggingssporsmal.domain

import no.nav.syfo.UserConstants
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import kotlin.test.assertTrue

class KartleggingssporsmalKandidatTest {

    @Test
    fun `new kandidat has correct status and values`() {
        val newKandidat = KartleggingssporsmalKandidat.create(
            personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
        )

        assert(newKandidat.status is KartleggingssporsmalKandidatStatusendring.Kandidat)
        assertTrue(newKandidat.personident == UserConstants.ARBEIDSTAKER_PERSONIDENT)
        assertNull(newKandidat.varsletAt)
        assertNull(newKandidat.journalpostId)
    }

    @Test
    fun `registrereSvarMottatt only works when status is Kandidat or SvarMottatt`() {
        val newKandidat = KartleggingssporsmalKandidat.create(
            personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
        )

        val svarMottattKandidat = newKandidat.registrerSvarMottatt(
            svarAt = OffsetDateTime.now(),
        )
        assert(svarMottattKandidat.status is KartleggingssporsmalKandidatStatusendring.SvarMottatt)

        val svarMottattKandidatAgain = svarMottattKandidat.registrerSvarMottatt(
            svarAt = OffsetDateTime.now(),
        )
        assert(svarMottattKandidatAgain.status is KartleggingssporsmalKandidatStatusendring.SvarMottatt)

        val ferdigbehandletKandidat = svarMottattKandidat.ferdigbehandleVurdering(UserConstants.VEILEDER_IDENT)

        assertThrows<IllegalArgumentException> {
            ferdigbehandletKandidat.registrerSvarMottatt(svarAt = OffsetDateTime.now())
        }
    }

    @Test
    fun `ferdigbehandleKandidat only works when status is SvarMottatt`() {
        val newKandidat = KartleggingssporsmalKandidat.create(
            personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
        )
        assertThrows<IllegalArgumentException> {
            newKandidat.ferdigbehandleVurdering(UserConstants.VEILEDER_IDENT)
        }
        val svarMottattKandidat = newKandidat.registrerSvarMottatt(
            svarAt = OffsetDateTime.now(),
        )
        val ferdigbehandletKandidat = svarMottattKandidat.ferdigbehandleVurdering(UserConstants.VEILEDER_IDENT)
        assert(ferdigbehandletKandidat.status is KartleggingssporsmalKandidatStatusendring.Ferdigbehandlet)

        assertThrows<IllegalArgumentException> {
            ferdigbehandletKandidat.ferdigbehandleVurdering(UserConstants.VEILEDER_IDENT)
        }
    }
}
