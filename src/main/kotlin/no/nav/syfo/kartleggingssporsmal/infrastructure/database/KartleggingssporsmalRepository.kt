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

    override suspend fun getLatestKandidat(personident: Personident): KartleggingssporsmalKandidat? {
        return database.connection.use { connection ->
            connection.getKandidater(personident)
                .firstOrNull()
                ?.let { (kandidat, statusendring) ->
                    kandidat.toKartleggingssporsmalKandidat(statusendring)
                }
        }
    }

    override suspend fun getKandidat(uuid: UUID): KartleggingssporsmalKandidat? =
        database.connection.use { connection ->
            connection.getKandidat(uuid)
                ?.let { (kandidat, statusendring) ->
                    kandidat.toKartleggingssporsmalKandidat(statusendring)
                }
        }

    override suspend fun getKandidatur(personident: Personident): List<KartleggingssporsmalKandidat> {
        return database.connection.use { connection ->
            connection.getKandidater(personident)
                .map { (kandidat, statusendring) ->
                    kandidat.toKartleggingssporsmalKandidat(statusendring)
                }
        }
    }

    override suspend fun getKandidatStatusendringer(kandidatUuid: UUID): List<KartleggingssporsmalKandidatStatusendring> =
        database.connection.use { connection ->
            connection.getKandidatStatusendringer(kandidatUuid)
                .map { it.toKartleggingssporsmalKandidatStatusendring() }
        }

    override suspend fun createKandidatAndMarkStoppunktAsProcessed(
        kandidat: KartleggingssporsmalKandidat,
        stoppunktId: Int,
    ): KartleggingssporsmalKandidat {
        return database.connection.use { connection ->
            val pKartleggingssporsmalKandidat = connection.createKandidat(kandidat, stoppunktId)
            val statusendring = connection.createStatusendring(
                statusendring = kandidat.status,
                kandidatId = pKartleggingssporsmalKandidat.id,
            )
            connection.markStoppunktAsProcessed(stoppunktId)
            connection.commit()
            pKartleggingssporsmalKandidat.toKartleggingssporsmalKandidat(statusendring)
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
            val status = connection.getKandidatStatusendringer(kandidat.uuid).firstOrNull()
                ?: throw NoSuchElementException("KandidatStatusendring for kandidat med UUID ${kandidat.uuid} finnes ikke i databasen")
            connection.commit()
            updatedKandidat.toKartleggingssporsmalKandidat(status)
        }
    }

    override suspend fun updatePublishedAtForKandidatStatusendring(kandidat: KartleggingssporsmalKandidat) {
        database.connection.use { connection ->
            connection.prepareStatement(UPDATE_KANDIDATSTATUSENDRING_PUBLISHED_AT).use {
                it.setString(1, kandidat.status.uuid.toString())
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
                it.executeQuery().toList {
                    val pKandidat = toPKartleggingssporsmalKandidat()
                    val pStatusendring = toPKartleggingssporsmalKandidatStatusendring(prefix = STATUS_PREFIX)
                    Pair(pKandidat, pStatusendring)
                }
            }.map { (kandidat, statusendring) ->
                kandidat.toKartleggingssporsmalKandidat(statusendring)
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
    ): KartleggingssporsmalKandidat =
        database.connection.use { connection ->
            val (pKandidat, _) = connection.getKandidat(kandidat.uuid)
                ?: throw NoSuchElementException("Kandidat med UUID ${kandidat.uuid} finnes ikke i databasen")
            val pStatusendring = connection.createStatusendring(statusendring = kandidat.status, kandidatId = pKandidat.id)
            val pUpdatedKandidat = connection.updateKandidatStatus(
                kandidatUuid = kandidat.uuid,
                status = kandidat.status.kandidatStatus,
            )
            connection.commit()
            pUpdatedKandidat.toKartleggingssporsmalKandidat(pStatusendring)
        }

    private fun Connection.getKandidatStatusendringer(kandidatUuid: UUID): List<PKartleggingssporsmalKandidatStatusendring> =
        this.prepareStatement(GET_KANDIDAT_STATUSENDRINGER_BY_KANDIDAT_UUID).use {
            it.setString(1, kandidatUuid.toString())
            it.executeQuery()
                .toList { toPKartleggingssporsmalKandidatStatusendring() }
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

    private fun Connection.createKandidat(kandidat: KartleggingssporsmalKandidat, stoppunktId: Int): PKartleggingssporsmalKandidat =
        this.prepareStatement(CREATE_KANDIDAT).use {
            it.setString(1, kandidat.uuid.toString())
            it.setObject(2, kandidat.createdAt)
            it.setObject(3, kandidat.createdAt)
            it.setString(4, kandidat.personident.value)
            it.setInt(5, stoppunktId)
            it.setString(6, kandidat.status.kandidatStatus.name)
            it.setObject(7, kandidat.varsletAt)
            it.executeQuery().toList { toPKartleggingssporsmalKandidat() }.single()
        }

    private fun Connection.getKandidat(uuid: UUID): Pair<PKartleggingssporsmalKandidat, PKartleggingssporsmalKandidatStatusendring>? =
        this.prepareStatement(GET_KANDIDAT_BY_UUID).use {
            it.setString(1, uuid.toString())
            it.executeQuery().toList {
                val pKandidat = toPKartleggingssporsmalKandidat()
                val pStatusendring = toPKartleggingssporsmalKandidatStatusendring(prefix = STATUS_PREFIX)
                Pair(pKandidat, pStatusendring)
            }.firstOrNull()
        }

    private fun Connection.getKandidater(personident: Personident): List<Pair<PKartleggingssporsmalKandidat, PKartleggingssporsmalKandidatStatusendring>> =
        this.prepareStatement(GET_KANDIDAT).use {
            it.setString(1, personident.value)
            it.executeQuery().toList {
                val pKandidat = toPKartleggingssporsmalKandidat()
                val pStatusendring = toPKartleggingssporsmalKandidatStatusendring(prefix = STATUS_PREFIX)
                Pair(pKandidat, pStatusendring)
            }
        }

    private fun Connection.createStatusendring(
        statusendring: KartleggingssporsmalKandidatStatusendring,
        kandidatId: Int,
    ): PKartleggingssporsmalKandidatStatusendring =
        this.prepareStatement(CREATE_KANDIDAT_STATUSENDRING).use {
            it.setString(1, statusendring.uuid.toString())
            it.setInt(2, kandidatId)
            it.setObject(3, statusendring.createdAt)
            it.setString(4, statusendring.kandidatStatus.name)
            it.setObject(5, statusendring.publishedAt)
            it.setObject(6, if (statusendring is KartleggingssporsmalKandidatStatusendring.SvarMottatt) statusendring.svarAt else null)
            it.setString(
                7,
                if (statusendring is KartleggingssporsmalKandidatStatusendring.Ferdigbehandlet) statusendring.veilederident else null
            )
            it.executeQuery()
                .toList { toPKartleggingssporsmalKandidatStatusendring() }
                .single()
        }

    private fun Connection.updateKandidatStatus(
        kandidatUuid: UUID,
        status: KandidatStatus,
    ): PKartleggingssporsmalKandidat =
        this.prepareStatement(UPDATE_KANDIDAT_STATUS).use {
            it.setString(1, status.name)
            it.setString(2, kandidatUuid.toString())
            it.executeQuery().toList { toPKartleggingssporsmalKandidat() }.single()
        }

    companion object {

        private const val STATUS_PREFIX = "status_"

        private const val STATUS_ALIAS = """
            s.id as ${STATUS_PREFIX}id,
            s.uuid as ${STATUS_PREFIX}uuid,
            s.kandidat_id as ${STATUS_PREFIX}kandidat_id,
            s.created_at as ${STATUS_PREFIX}created_at,
            s.status as ${STATUS_PREFIX}status,
            s.published_at as ${STATUS_PREFIX}published_at,
            s.svar_at as ${STATUS_PREFIX}svar_at,
            s.veilederident as ${STATUS_PREFIX}veilederident
        """

        private const val JOIN_SELECT_NEWEST_STATUS_FROM_STATUSENDRINGER = """
            INNER JOIN KARTLEGGINGSSPORSMAL_KANDIDAT_STATUSENDRING s
            ON k.id = s.kandidat_id AND s.created_at = (
                SELECT MAX(created_at) FROM KARTLEGGINGSSPORSMAL_KANDIDAT_STATUSENDRING
                WHERE kandidat_id = k.id
            )
        """

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
            SELECT *,
            $STATUS_ALIAS
            FROM KARTLEGGINGSSPORSMAL_KANDIDAT k
            $JOIN_SELECT_NEWEST_STATUS_FROM_STATUSENDRINGER
            WHERE k.personident = ?
            ORDER BY k.created_at DESC
        """

        private const val GET_KANDIDAT_BY_UUID = """
            SELECT *,
            $STATUS_ALIAS
            FROM KARTLEGGINGSSPORSMAL_KANDIDAT k
            $JOIN_SELECT_NEWEST_STATUS_FROM_STATUSENDRINGER
            WHERE k.uuid = ?
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
                 SELECT *,
                 $STATUS_ALIAS
                 FROM KARTLEGGINGSSPORSMAL_KANDIDAT k
                 $JOIN_SELECT_NEWEST_STATUS_FROM_STATUSENDRINGER
                 WHERE journalpost_id IS NULL AND varslet_at IS NOT NULL
                 ORDER BY k.created_at ASC
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
        journalpostId = getString("journalpost_id")?.let { JournalpostId(it) },
    )
}

internal fun ResultSet.toPKartleggingssporsmalKandidatStatusendring(prefix: String = ""): PKartleggingssporsmalKandidatStatusendring {
    return PKartleggingssporsmalKandidatStatusendring(
        id = getInt("${prefix}id"),
        uuid = getString("${prefix}uuid").let { UUID.fromString(it) },
        createdAt = getObject("${prefix}created_at", OffsetDateTime::class.java),
        kandidatId = getInt("${prefix}kandidat_id"),
        status = getString("${prefix}status"),
        publishedAt = getObject("${prefix}published_at", OffsetDateTime::class.java),
        svarAt = getObject("${prefix}svar_at", OffsetDateTime::class.java),
        veilederident = getString("${prefix}veilederident"),
    )
}
