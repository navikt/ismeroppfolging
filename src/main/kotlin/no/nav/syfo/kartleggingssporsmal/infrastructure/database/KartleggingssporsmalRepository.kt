package no.nav.syfo.kartleggingssporsmal.infrastructure.database

import no.nav.syfo.kartleggingssporsmal.application.IKartleggingssporsmalRepository
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.infrastructure.database.DatabaseInterface
import no.nav.syfo.shared.infrastructure.database.toList
import java.sql.Date
import java.sql.ResultSet
import java.sql.SQLException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.jvm.java

class KartleggingssporsmalRepository(
    private val database: DatabaseInterface,
) : IKartleggingssporsmalRepository {

    override suspend fun createStoppunkt(
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

    override suspend fun getKandidat(personident: Personident): KartleggingssporsmalKandidat? {
        return database.connection.use { connection ->
            connection.prepareStatement(GET_KANDIDAT).use {
                it.setString(1, personident.value)
                it.executeQuery().toList { toPKartleggingssporsmalKandidat() }
            }
                .maxByOrNull { it.createdAt }
                ?.let { kandidat ->
                    KartleggingssporsmalKandidat.createFromDatabase(
                        uuid = kandidat.uuid,
                        createdAt = kandidat.createdAt,
                        personident = kandidat.personident,
                        status = kandidat.status,
                        varsletAt = kandidat.varsletAt,
                    )
                }
        }
    }

    override suspend fun getUnprocessedStoppunkt(): List<KartleggingssporsmalStoppunkt> {
        return database.connection.use { connection ->
            connection.prepareStatement(GET_UNPROCESSED_STOPPUNKT).use {
                it.setDate(1, Date.valueOf(LocalDate.now()))
                it.setDate(2, Date.valueOf(LocalDate.now().minusDays(1)))
                it.executeQuery().toList { toPKartleggingssporsmalStoppunkt().toKartleggingssporsmalStoppunkt() }
            }
        }
    }

    override suspend fun markStoppunktAsProcessed(stoppunkt: KartleggingssporsmalStoppunkt) {
        database.connection.use { connection ->
            connection.prepareStatement(SET_STOPPUNKT_PROCESSED).use {
                it.setString(1, stoppunkt.uuid.toString())
                val updated = it.executeUpdate()
                if (updated != 1) {
                    throw SQLException("Expected a single row to be updated, got update count $updated")
                }
            }
            connection.commit()
        }
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

        private const val GET_KANDIDAT = """
            SELECT * FROM KARTLEGGINGSSPORSMAL_KANDIDAT
            WHERE personident = ? AND status = 'KANDIDAT'
            ORDER BY created_at DESC
        """

        private const val GET_UNPROCESSED_STOPPUNKT = """
            SELECT * FROM KARTLEGGINGSSPORSMAL_STOPPUNKT
            WHERE processed_at IS NULL AND (stoppunktAt = ? OR stoppunktAt = ?)
        """

        private const val SET_STOPPUNKT_PROCESSED = """
            UPDATE KARTLEGGINGSSPORSMAL_STOPPUNKT
            SET processed_at = now()
            WHERE uuid = ?
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
