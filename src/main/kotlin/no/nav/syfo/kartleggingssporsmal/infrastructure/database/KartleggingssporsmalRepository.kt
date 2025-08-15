package no.nav.syfo.kartleggingssporsmal.infrastructure.database

import no.nav.syfo.kartleggingssporsmal.application.IKartleggingssporsmalRepository
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.infrastructure.database.DatabaseInterface
import no.nav.syfo.shared.infrastructure.database.toList
import java.sql.ResultSet
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.jvm.java

class KartleggingssporsmalRepository(
    private val database: DatabaseInterface,
) : IKartleggingssporsmalRepository {

    override fun createStoppunkt(
        stoppunkt: KartleggingssporsmalStoppunkt,
    ): KartleggingssporsmalStoppunkt =
        database.connection.use { connection ->
            val pKartleggingssporsmalStoppunkt = connection.prepareStatement(CREATE_STOPPUNKT).use {
                it.setString(1, stoppunkt.uuid.toString())
                it.setObject(2, stoppunkt.createdAt)
                it.setString(3, stoppunkt.personident.value)
                it.setString(4, stoppunkt.tilfelleBitReferanseUuid.toString())
                it.setObject(5, stoppunkt.stoppunktAt)
                it.setObject(6, stoppunkt.processedAt)
                it.executeQuery().toList { toPKartleggingssporsmalStoppunkt() }.single()
            }
            connection.commit()
            pKartleggingssporsmalStoppunkt.toKartleggingssporsmalStoppunkt()
        }

    companion object {
        private const val CREATE_STOPPUNKT = """
            INSERT INTO KARTLEGGINGSSPORSMAL_STOPPUNKT (
                id,
                uuid,
                created_at,
                personident,
                tilfelle_bit_referanse_uuid,
                stoppunkt_at,
                processed_at
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)
            RETURNING *
        """
    }
}

internal fun ResultSet.toPKartleggingssporsmalStoppunkt(): PKartleggingssporsmalStoppunkt {
    return PKartleggingssporsmalStoppunkt(
        id = getInt("id"),
        uuid = getString("uuid").let { UUID.fromString(it) },
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        personident = Personident(getString("personident")),
        tilfelleBitReferanseUuid = getString("tilfelle_bit_referanse_uuid").let { UUID.fromString(it) },
        stoppunktAt = getObject("stoppunkt_at", LocalDate::class.java),
        processedAt = getObject("processed_at", OffsetDateTime::class.java),
    )
}

internal fun ResultSet.toPKartleggingssporsmalKandidat(): PKartleggingssporsmalKandidat {
    return PKartleggingssporsmalKandidat(
        id = getInt("id"),
        uuid = getString("uuid").let { UUID.fromString(it) },
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        personident = Personident(getString("personident")),
        generatedByStoppunktId = getInt("generated_by_stoppunkt_id"),
        status = getString("status"),
        varsletAt = getObject("varslet_at", OffsetDateTime::class.java),
    )
}
