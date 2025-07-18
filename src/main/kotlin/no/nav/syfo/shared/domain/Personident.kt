package no.nav.syfo.shared.domain

@JvmInline
value class Personident(val value: String) {
    init {
        if (!Regex("^\\d{11}\$").matches(value)) {
            throw IllegalArgumentException("Value is not a valid PersonIdent")
        }
    }
}
