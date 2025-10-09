package no.nav.syfo.kartleggingssporsmal.domain

import kotlin.text.all
import kotlin.text.isDigit

@JvmInline
value class JournalpostId(val value: String) {
    init {
        if (!value.all { it.isDigit() }) {
            throw kotlin.IllegalArgumentException("Value is not a valid JournalpostId")
        }
    }
}
