package no.nav.syfo

import no.nav.syfo.shared.domain.Personident

object UserConstants {
    val ARBEIDSTAKER_PERSONIDENT = Personident("12345678910")
    val ARBEIDSTAKER_PERSONIDENT_INACTIVE = Personident("12345678911")
    val ARBEIDSTAKER_PERSONIDENT_ERROR = Personident("12312312300")
    val ARBEIDSTAKER_PERSONIDENT_VEILEDER_NO_ACCESS = Personident("11111111111")
    val ARBEIDSTAKER_PERSONIDENT_ANNEN_ENHET = Personident("11111111333")
    val ARBEIDSTAKER_NO_FODSELSDATO = Personident("12311111111")
    const val VEILEDER_IDENT = "Z999999"
    const val VIRKSOMHETSNUMMER = "123456789"
}
