package no.nav.syfo.shared.infrastructure.cronjob

interface Cronjob {
    suspend fun run(): List<Result<Any>>
    val initialDelayMinutes: Long
    val intervalDelayMinutes: Long
}
