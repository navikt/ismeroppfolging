package no.nav.syfo.shared.infrastructure.clients.veiledertilgang

data class Tilgang(
    val erGodkjent: Boolean,
    val erAvslatt: Boolean = false,
    val fullTilgang: Boolean = false,
)
