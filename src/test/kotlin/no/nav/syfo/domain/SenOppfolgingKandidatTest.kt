package no.nav.syfo.domain

import no.nav.syfo.UserConstants
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.*

class SenOppfolgingKandidatTest {

    private val kandidat = SenOppfolgingKandidat(
        personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
        varselAt = null,
    )
    private val vurdering = SenOppfolgingVurdering(
        veilederident = UserConstants.VEILEDER_IDENT,
        begrunnelse = "Dette er en begrunnelse.",
        type = VurderingType.FERDIGBEHANDLET,
    )

    @Test
    fun `Should vurdere kandidat`() {
        val updatedKandidat = kandidat.vurder(vurdering)

        assertEquals(vurdering, updatedKandidat.vurdering)
    }

    @Test
    fun `Should fail when attempting multiple vurderinger`() {
        val updatedKandidat = kandidat.vurder(vurdering)

        assertThrows<IllegalStateException> {
            updatedKandidat.vurder(vurdering)
        }
    }
}
