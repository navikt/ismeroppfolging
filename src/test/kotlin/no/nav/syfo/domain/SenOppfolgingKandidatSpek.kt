package no.nav.syfo.domain

import no.nav.syfo.UserConstants
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class SenOppfolgingKandidatSpek : Spek({
    describe(SenOppfolgingKandidat::class.java.simpleName) {
        val kandidat = SenOppfolgingKandidat(
            personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
            varselAt = null,
        )
        val vurdering = SenOppfolgingVurdering(
            veilederident = UserConstants.VEILEDER_IDENT,
            begrunnelse = "Dette er en begrunnelse.",
            type = VurderingType.FERDIGBEHANDLET,
        )

        it("Should vurdere kandidat") {
            val updatedKandidat = kandidat.vurder(vurdering)

            updatedKandidat.vurdering shouldBeEqualTo vurdering
        }

        it("Should fail when attempting multiple vurderinger") {
            val updatedKandidat = kandidat.vurder(vurdering)

            assertFailsWith<IllegalStateException> {
                updatedKandidat.vurder(vurdering)
            }
        }
    }
})
