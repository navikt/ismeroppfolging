package no.nav.syfo.domain

enum class SenOppfolgingStatus(val isActive: Boolean) {
    KANDIDAT(isActive = true),
    FERDIGBEHANDLET(isActive = false),
}
