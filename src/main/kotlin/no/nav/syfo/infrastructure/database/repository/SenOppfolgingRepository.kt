package no.nav.syfo.infrastructure.database.repository

import no.nav.syfo.application.ISenOppfolgingRepository
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.SenOppfolgingKandidat
import no.nav.syfo.domain.SenOppfolgingSvar
import no.nav.syfo.infrastructure.database.DatabaseInterface
import no.nav.syfo.infrastructure.database.toList
import no.nav.syfo.util.nowUTC
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.time.OffsetDateTime
import java.util.*

class SenOppfolgingRepository(private val database: DatabaseInterface) : ISenOppfolgingRepository {

    override fun createKandidat(senOppfolgingKandidat: SenOppfolgingKandidat): SenOppfolgingKandidat =
        database.connection.use { connection ->
            val pSenOppfolgingKandidat = connection.createKandidat(senOppfolgingKandidat)
            connection.commit()
            pSenOppfolgingKandidat.toSenOppfolgingKandidat()
        }

    override fun updateKandidatSvar(senOppfolgingSvar: SenOppfolgingSvar, senOppfolgingKandidaUuid: UUID) =
        database.connection.use { connection ->
            connection.prepareStatement(UPDATE_KANDIDAT_SVAR).use {
                it.setObject(1, senOppfolgingSvar.svarAt)
                it.setString(2, senOppfolgingSvar.onskerOppfolging.name)
                it.setObject(3, senOppfolgingKandidaUuid.toString())
                val updated = it.executeUpdate()
                if (updated != 1) {
                    throw SQLException("Expected a single row to be updated, got update count $updated")
                }
            }
            connection.commit()
        }

    override fun getUnpublishedKandidater(): List<SenOppfolgingKandidat> =
        database.connection.use { connection ->
            connection.prepareStatement(GET_UNPUBLISHED).use {
                it.executeQuery().toList { toPSenOppfolgingKandidat() }
            }.map {
                it.toSenOppfolgingKandidat()
            }
        }

    override fun setPublished(kandidatUuid: UUID) =
        database.connection.use { connection ->
            connection.prepareStatement(UPDATE_PUBLISHED_AT).use {
                it.setObject(1, nowUTC())
                it.setObject(2, nowUTC())
                it.setString(3, kandidatUuid.toString())
                val updated = it.executeUpdate()
                if (updated != 1) {
                    throw SQLException("Expected a single row to be updated, got update count $updated")
                }
            }
            connection.commit()
        }

    private fun Connection.createKandidat(senOppfolgingKandidat: SenOppfolgingKandidat): PSenOppfolgingKandidat =
        prepareStatement(CREATE_KANDIDAT).use {
            it.setString(1, senOppfolgingKandidat.uuid.toString())
            it.setObject(2, senOppfolgingKandidat.createdAt)
            it.setObject(3, senOppfolgingKandidat.createdAt)
            it.setString(4, senOppfolgingKandidat.personident.value)
            it.setObject(5, senOppfolgingKandidat.varselAt)
            it.executeQuery().toList { toPSenOppfolgingKandidat() }.single()
        }

    companion object {
        private const val CREATE_KANDIDAT = """
            INSERT INTO SEN_OPPFOLGING_KANDIDAT (
                id,
                uuid,
                created_at,
                updated_at,
                personident,
                varsel_at
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?)
            RETURNING *
        """

        private const val UPDATE_KANDIDAT_SVAR = """
            UPDATE SEN_OPPFOLGING_KANDIDAT SET
                svar_at = ?,
                onsker_oppfolging = ?
            WHERE uuid = ?
        """

        private const val GET_UNPUBLISHED =
            """
                SELECT * FROM SEN_OPPFOLGING_KANDIDAT WHERE published_at IS NULL ORDER BY created_at ASC
            """

        private const val UPDATE_PUBLISHED_AT =
            """
                UPDATE SEN_OPPFOLGING_KANDIDAT SET updated_at=?, published_at=? WHERE uuid=?
            """
    }
}

internal fun ResultSet.toPSenOppfolgingKandidat(): PSenOppfolgingKandidat = PSenOppfolgingKandidat(
    id = getInt("id"),
    uuid = UUID.fromString(getString("uuid")),
    createdAt = getObject("created_at", OffsetDateTime::class.java),
    updatedAt = getObject("updated_at", OffsetDateTime::class.java),
    personident = Personident(getString("personident")),
    varselAt = getObject("varsel_at", OffsetDateTime::class.java),
    svarAt = getObject("svar_at", OffsetDateTime::class.java),
    onskerOppfolging = getString("onsker_oppfolging"),
    publishedAt = getObject("published_at", OffsetDateTime::class.java),
)
