package no.nav.syfo.senoppfolging.domain

enum class SenOppfolgingStatus(val isActive: Boolean) {
    KANDIDAT(isActive = true),
    FERDIGBEHANDLET(isActive = false), ;

    companion object {
        fun from(type: VurderingType): SenOppfolgingStatus {
            when (type) {
                VurderingType.FERDIGBEHANDLET -> return FERDIGBEHANDLET
            }
        }
    }
}
