package no.nav.syfo.generators

import no.nav.syfo.UserConstants
import no.nav.syfo.domain.Personident
import no.nav.syfo.identhendelse.kafka.IdentType
import no.nav.syfo.identhendelse.kafka.Identifikator
import no.nav.syfo.identhendelse.kafka.KafkaIdenthendelseDTO

fun generateKafkaIdenthendelseDTO(
    personident: Personident = UserConstants.ARBEIDSTAKER_PERSONIDENT,
    hasOldPersonident: Boolean = true,
): KafkaIdenthendelseDTO {
    val identifikatorer = mutableListOf(
        Identifikator(
            idnummer = personident.value,
            type = IdentType.FOLKEREGISTERIDENT,
            gjeldende = true,
        ),
        Identifikator(
            idnummer = "10$personident",
            type = IdentType.AKTORID,
            gjeldende = true
        ),
    )
    if (hasOldPersonident) {
        identifikatorer.addAll(
            listOf(
                Identifikator(
                    idnummer = UserConstants.ARBEIDSTAKER_PERSONIDENT_INACTIVE.value,
                    type = IdentType.FOLKEREGISTERIDENT,
                    gjeldende = false,
                ),
            )
        )
    }
    return KafkaIdenthendelseDTO(identifikatorer)
}
