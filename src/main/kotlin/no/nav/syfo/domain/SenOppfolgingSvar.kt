package no.nav.syfo.domain

import java.time.OffsetDateTime

data class SenOppfolgingSvar(
    val svarAt: OffsetDateTime,
    val onskerOppfolging: OnskerOppfolging,
) {
    companion object {
        fun createFromDatabase(
            svarAt: OffsetDateTime,
            onskerOppfolging: OnskerOppfolging,
        ): SenOppfolgingSvar = SenOppfolgingSvar(
            svarAt = svarAt,
            onskerOppfolging = onskerOppfolging,
        )
    }
}

enum class OnskerOppfolging {
    JA,
    NEI,
}
