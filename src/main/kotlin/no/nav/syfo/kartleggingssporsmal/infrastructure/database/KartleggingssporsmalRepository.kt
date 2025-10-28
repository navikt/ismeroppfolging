package no.nav.syfo.kartleggingssporsmal.infrastructure.database

import no.nav.syfo.kartleggingssporsmal.application.IKartleggingssporsmalRepository
import no.nav.syfo.kartleggingssporsmal.domain.*
import no.nav.syfo.shared.domain.Personident
import no.nav.syfo.shared.infrastructure.database.DatabaseInterface
import no.nav.syfo.shared.infrastructure.database.toList
import java.sql.Connection
import java.sql.Date
import java.sql.ResultSet
import java.sql.SQLException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

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
            connection.getKandidat(personident)
                ?.let { kandidat ->
                    KartleggingssporsmalKandidat.createFromDatabase(
                        uuid = kandidat.uuid,
                        createdAt = kandidat.createdAt,
                        personident = kandidat.personident,
                        status = kandidat.status,
                        varsletAt = kandidat.varsletAt,
                        journalpostId = kandidat.journalpostId,
                    )
                }
        }
    }

    override suspend fun getKandidat(uuid: UUID): KartleggingssporsmalKandidat? {
        return database.connection.use { connection ->
            connection.prepareStatement(GET_KANDIDAT_BY_UUID).use {
                it.setString(1, uuid.toString())
                it.executeQuery().toList { toPKartleggingssporsmalKandidat() }
            }
                .firstOrNull()
                ?.let { kandidat ->
                    KartleggingssporsmalKandidat.createFromDatabase(
                        uuid = kandidat.uuid,
                        createdAt = kandidat.createdAt,
                        personident = kandidat.personident,
                        status = kandidat.status,
                        varsletAt = kandidat.varsletAt,
                        journalpostId = kandidat.journalpostId,
                    )
                }
        }
    }

    override suspend fun getKandidatStatusendringer(kandidatUuid: UUID): List<KartleggingssporsmalKandidatStatusendring> {
        return database.connection.use { connection ->
            connection.prepareStatement(GET_KANDIDAT_STATUSENDRINGER_BY_KANDIDAT_UUID).use {
                it.setString(1, kandidatUuid.toString())
                it.executeQuery()
                    .toList { toPKartleggingssporsmalKandidatStatusendring().toKartleggingssporsmalKandidatStatusendring() }
            }
        }
    }

    override suspend fun createKandidatAndMarkStoppunktAsProcessed(
        kandidat: KartleggingssporsmalKandidat,
        stoppunktId: Int,
    ): KartleggingssporsmalKandidat {
        return database.connection.use { connection ->
            val pKartleggingssporsmalKandidat = connection.createKandidat(kandidat, stoppunktId)
            connection.createStatusendring(
                kandidatStatusendring = KartleggingssporsmalKandidatStatusendring(
                    status = kandidat.status,
                ),
                kandidatId = pKartleggingssporsmalKandidat.id,
            )
            connection.markStoppunktAsProcessed(stoppunktId)
            connection.commit()
            pKartleggingssporsmalKandidat.toKartleggingssporsmalKandidat()
        }
    }

    override suspend fun markStoppunktAsProcessed(stoppunktId: Int) {
        return database.connection.use { connection ->
            connection.markStoppunktAsProcessed(stoppunktId)
            connection.commit()
        }
    }

    override suspend fun getUnprocessedStoppunkter(): List<Pair<Int, KartleggingssporsmalStoppunkt>> {
        return database.connection.use { connection ->
            connection.prepareStatement(GET_UNPROCESSED_STOPPUNKTER).use {
                it.setDate(1, Date.valueOf(LocalDate.now()))
                it.setDate(2, Date.valueOf(LocalDate.now().minusDays(1)))
                it.executeQuery()
                    .toList { toPKartleggingssporsmalStoppunkt() }
                    .map { stoppunkt ->
                        Pair(
                            stoppunkt.id,
                            stoppunkt.toKartleggingssporsmalStoppunkt()
                        )
                    }
            }
        }
    }

    override suspend fun updateVarsletAtForKandidat(kandidat: KartleggingssporsmalKandidat): KartleggingssporsmalKandidat {
        return database.connection.use { connection ->
            val updatedKandidat = connection.prepareStatement(UPDATE_KANDIDAT_VARSLET_AT).use {
                it.setString(1, kandidat.uuid.toString())
                it.executeQuery().toList { toPKartleggingssporsmalKandidat() }.single()
            }
            connection.commit()
            updatedKandidat.toKartleggingssporsmalKandidat()
        }
    }

    override suspend fun updatePublishedAtForKandidatStatusendring(kandidatStatus: KartleggingssporsmalKandidatStatusendring) {
        database.connection.use { connection ->
            connection.prepareStatement(UPDATE_KANDIDATSTATUSENDRING_PUBLISHED_AT).use {
                it.setString(1, kandidatStatus.uuid.toString())
                val rowCount = it.executeUpdate()
                if (rowCount != 1) {
                    throw SQLException("Expected a single row to be updated, got update count $rowCount")
                }
            }
            connection.commit()
        }
    }

    override fun getNotJournalforteKandidater(): List<KartleggingssporsmalKandidat> =
        database.connection.use { connection ->
            connection.prepareStatement(GET_NOT_JOURNALFORTE).use {
                it.executeQuery().toList { toPKartleggingssporsmalKandidat() }
            }.map {
                it.toKartleggingssporsmalKandidat()
            }
        }

    override fun updateJournalpostidForKandidat(kandidat: KartleggingssporsmalKandidat, journalpostId: JournalpostId) {
        database.connection.use { connection ->
            connection.prepareStatement(SET_JOURNALPOST_ID).use {
                it.setString(1, journalpostId.value)
                it.setString(2, kandidat.uuid.toString())
                val updated = it.executeUpdate()
                if (updated != 1) {
                    throw SQLException("Expected a single row to be updated, got update count $updated")
                }
            }
            connection.commit()
        }
    }

    override suspend fun createKandidatStatusendring(
        kandidat: KartleggingssporsmalKandidat,
        kandidatStatusendring: KartleggingssporsmalKandidatStatusendring,
    ): KartleggingssporsmalKandidatStatusendring =
        database.connection.use { connection ->
            val pKandidat = connection.getKandidat(kandidat.personident)
                ?: throw NoSuchElementException("Kandidat med UUID ${kandidat.uuid} finnes ikke i databasen")
            val pStatusendring = connection.createStatusendring(kandidatStatusendring = kandidatStatusendring, kandidatId = pKandidat.id)
                .toKartleggingssporsmalKandidatStatusendring()
            connection.updateKandidatStatus(
                kandidatUuid = kandidat.uuid,
                status = kandidatStatusendring.status,
            )
            connection.commit()
            pStatusendring
        }

    private fun Connection.createKandidat(kandidat: KartleggingssporsmalKandidat, stoppunktId: Int): PKartleggingssporsmalKandidat =
        this.prepareStatement(CREATE_KANDIDAT).use {
            it.setString(1, kandidat.uuid.toString())
            it.setObject(2, kandidat.createdAt)
            it.setObject(3, kandidat.createdAt)
            it.setString(4, kandidat.personident.value)
            it.setInt(5, stoppunktId)
            it.setString(6, kandidat.status.name)
            it.setObject(7, kandidat.varsletAt)
            it.executeQuery().toList { toPKartleggingssporsmalKandidat() }.single()
        }

    private fun Connection.getKandidat(personident: Personident): PKartleggingssporsmalKandidat? =
        this.prepareStatement(GET_KANDIDAT).use {
            it.setString(1, personident.value)
            it.executeQuery().toList { toPKartleggingssporsmalKandidat() }.firstOrNull()
        }

    private fun Connection.markStoppunktAsProcessed(stoppunktId: Int) {
        this.prepareStatement(SET_STOPPUNKT_PROCESSED).use {
            it.setInt(1, stoppunktId)
            val updated = it.executeUpdate()
            if (updated != 1) {
                throw SQLException("Expected a single row to be updated, got update count $updated")
            }
        }
    }

    private fun Connection.createStatusendring(
        kandidatStatusendring: KartleggingssporsmalKandidatStatusendring,
        kandidatId: Int,
    ): PKartleggingssporsmalKandidatStatusendring =
        this.prepareStatement(CREATE_KANDIDAT_STATUSENDRING).use {
            it.setString(1, kandidatStatusendring.uuid.toString())
            it.setInt(2, kandidatId)
            it.setObject(3, kandidatStatusendring.createdAt)
            it.setString(4, kandidatStatusendring.status.name)
            it.setObject(5, kandidatStatusendring.publishedAt)
            it.setObject(6, kandidatStatusendring.svarAt)
            it.setString(7, kandidatStatusendring.veilederident)
            it.executeQuery()
                .toList { toPKartleggingssporsmalKandidatStatusendring() }
                .single()
        }

    private fun Connection.updateKandidatStatus(
        kandidatUuid: UUID,
        status: KandidatStatus,
    ) {
        this.prepareStatement(UPDATE_KANDIDAT_STATUS).use {
            it.setString(1, status.name)
            it.setString(2, kandidatUuid.toString())
            val updated = it.executeUpdate()
            if (updated != 1) {
                throw SQLException("Expected a single row to be updated, got update count $updated")
            }
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
            WHERE personident = ?
            ORDER BY created_at DESC
        """

        private const val GET_KANDIDAT_BY_UUID = """
            SELECT * FROM KARTLEGGINGSSPORSMAL_KANDIDAT
            WHERE uuid = ?
        """

        private const val GET_KANDIDAT_STATUSENDRINGER_BY_KANDIDAT_UUID = """
            SELECT * FROM KARTLEGGINGSSPORSMAL_KANDIDAT_STATUSENDRING
            WHERE kandidat_id = (
                SELECT id FROM KARTLEGGINGSSPORSMAL_KANDIDAT WHERE uuid = ?
            )
            ORDER BY created_at DESC
        """

        private const val CREATE_KANDIDAT = """
            INSERT INTO KARTLEGGINGSSPORSMAL_KANDIDAT (
                id,
                uuid,
                created_at,
                updated_at,
                personident,
                generated_by_stoppunkt_id,
                status,
                varslet_at
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?)
            RETURNING *
        """

        private const val CREATE_KANDIDAT_STATUSENDRING = """
            INSERT INTO KARTLEGGINGSSPORSMAL_KANDIDAT_STATUSENDRING (
                id,
                uuid,
                kandidat_id,
                created_at,
                status,
                published_at,
                svar_at,
                veilederident
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?)
            RETURNING *
        """

        private const val GET_UNPROCESSED_STOPPUNKTER = """
            SELECT * FROM KARTLEGGINGSSPORSMAL_STOPPUNKT
            WHERE processed_at IS NULL AND (stoppunkt_at = ? OR stoppunkt_at = ?)
        """

        private const val SET_STOPPUNKT_PROCESSED = """
            UPDATE KARTLEGGINGSSPORSMAL_STOPPUNKT
            SET processed_at = now()
            WHERE id = ?
        """

        private const val UPDATE_KANDIDATSTATUSENDRING_PUBLISHED_AT = """
            UPDATE KARTLEGGINGSSPORSMAL_KANDIDAT_STATUSENDRING
            SET published_at = now()
            WHERE uuid = ?
        """

        private const val UPDATE_KANDIDAT_VARSLET_AT = """
            UPDATE KARTLEGGINGSSPORSMAL_KANDIDAT
            SET varslet_at = now(), updated_at = now()
            WHERE uuid = ?
            RETURNING *
        """

        private const val GET_NOT_JOURNALFORTE =
            """
                 SELECT *
                 FROM KARTLEGGINGSSPORSMAL_KANDIDAT
                 WHERE journalpost_id IS NULL AND varslet_at IS NOT NULL
                 ORDER BY created_at ASC
            """

        private const val SET_JOURNALPOST_ID =
            """
                UPDATE KARTLEGGINGSSPORSMAL_KANDIDAT
                SET journalpost_id = ?, updated_at = now()
                WHERE uuid = ?
            """

        private const val UPDATE_KANDIDAT_STATUS =
            """
                UPDATE KARTLEGGINGSSPORSMAL_KANDIDAT
                SET status = ?, updated_at = now()
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
        journalpostId = getString("journalpost_id")?.let { JournalpostId(it) },
    )
}

internal fun ResultSet.toPKartleggingssporsmalKandidatStatusendring(): PKartleggingssporsmalKandidatStatusendring {
    return PKartleggingssporsmalKandidatStatusendring(
        id = getInt("id"),
        uuid = getString("uuid").let { UUID.fromString(it) },
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        kandidatId = getInt("kandidat_id"),
        status = getString("status"),
        publishedAt = getObject("published_at", OffsetDateTime::class.java),
        svarAt = getObject("svar_at", OffsetDateTime::class.java),
        veilederident = getString("veilederident"),
    )
}
