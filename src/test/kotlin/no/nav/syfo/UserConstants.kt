package no.nav.syfo

import no.nav.syfo.domain.Personident

object UserConstants {
    val ARBEIDSTAKER_PERSONIDENT = Personident("12345678910")
    val ARBEIDSTAKER_PERSONIDENT_INACTIVE = Personident("12345678911")
    val ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS = Personident("11111111111")
    val ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET = Personident("11111111333")
    const val VEILEDER_IDENT = "Z999999"
}
