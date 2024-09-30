package no.nav.syfo.infrastructure.database.repository

import no.nav.syfo.application.ISenOppfolgingRepository
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.SenOppfolgingKandidat
import no.nav.syfo.domain.SenOppfolgingStatus
import no.nav.syfo.domain.SenOppfolgingSvar
import no.nav.syfo.domain.SenOppfolgingVurdering
import no.nav.syfo.domain.VurderingType
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
            pSenOppfolgingKandidat.toSenOppfolgingKandidat(vurderinger = emptyList())
        }

    override fun getKandidat(kandidatUuid: UUID): SenOppfolgingKandidat? = database.connection.use { connection ->
        connection.prepareStatement(GET_KANDIDAT_BY_UUID).use {
            it.setString(1, kandidatUuid.toString())
            it.executeQuery().toList { toPSenOppfolgingKandidat() }
        }.map {
            it.toSenOppfolgingKandidat(connection.getVurderinger(it.id))
        }.firstOrNull()
    }

    override fun findKandidatFromVarselId(varselId: UUID): SenOppfolgingKandidat? =
        database.connection.use { connection ->
            connection.prepareStatement(FIND_KANDIDAT_BY_VARSEL_ID).use {
                it.setString(1, varselId.toString())
                it.executeQuery().toList { toPSenOppfolgingKandidat() }
            }.map {
                it.toSenOppfolgingKandidat(connection.getVurderinger(it.id))
            }.firstOrNull()
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

    override fun addVurdering(senOppfolgingKandidat: SenOppfolgingKandidat, vurdering: SenOppfolgingVurdering): SenOppfolgingVurdering =
        database.connection.use { connection ->
            val kandidatId = connection.updateKandidatStatus(senOppfolgingKandidat = senOppfolgingKandidat)
            val savedVurdering = connection.createVurdering(
                kandidatId = kandidatId,
                senOppfolgingVurdering = vurdering
            )
            connection.commit()
            savedVurdering.toSenOppfolgingVurdering()
        }

    override fun getUnpublishedKandidatStatuser(): List<SenOppfolgingKandidat> =
        database.connection.use { connection ->
            connection.prepareStatement(GET_UNPUBLISHED_KANDIDAT).use {
                it.executeQuery().toList { toPSenOppfolgingKandidat() }
            }.map {
                it.toSenOppfolgingKandidat(connection.getVurderinger(it.id))
            }
        }

    override fun setKandidatPublished(kandidatUuid: UUID) {
        val now = nowUTC()
        database.connection.use { connection ->
            connection.prepareStatement(UPDATE_KANDIDAT_PUBLISHED_AT).use {
                it.setObject(1, now)
                it.setObject(2, now)
                it.setString(3, kandidatUuid.toString())
                val updated = it.executeUpdate()
                if (updated != 1) {
                    throw SQLException("Expected a single row to be updated, got update count $updated")
                }
            }
            connection.commit()
        }
    }

    override fun setVurderingPublished(vurderingUuid: UUID) {
        val now = nowUTC()
        database.connection.use { connection ->
            connection.prepareStatement(UPDATE_VURDERING_PUBLISHED_AT).use {
                it.setObject(1, now)
                it.setObject(2, now)
                it.setString(3, vurderingUuid.toString())
                val updated = it.executeUpdate()
                if (updated != 1) {
                    throw SQLException("Expected a single row to be updated, got update count $updated")
                }
            }
            connection.commit()
        }
    }

    override fun getKandidater(personident: Personident): List<SenOppfolgingKandidat> = database.connection.use { connection ->
        connection.prepareStatement(GET_KANDIDAT_BY_PERSONIDENT).use {
            it.setString(1, personident.value)
            it.executeQuery().toList { toPSenOppfolgingKandidat() }
        }.map {
            it.toSenOppfolgingKandidat(connection.getVurderinger(it.id))
        }
    }

    private fun Connection.createKandidat(senOppfolgingKandidat: SenOppfolgingKandidat): PSenOppfolgingKandidat =
        prepareStatement(CREATE_KANDIDAT).use {
            it.setString(1, senOppfolgingKandidat.uuid.toString())
            it.setObject(2, senOppfolgingKandidat.createdAt)
            it.setObject(3, senOppfolgingKandidat.createdAt)
            it.setString(4, senOppfolgingKandidat.personident.value)
            it.setObject(5, senOppfolgingKandidat.varselAt)
            it.setString(6, senOppfolgingKandidat.varselId?.toString())
            it.executeQuery().toList { toPSenOppfolgingKandidat() }.single()
        }

    private fun Connection.updateKandidatStatus(senOppfolgingKandidat: SenOppfolgingKandidat): Int =
        prepareStatement(UPDATE_KANDIDAT_STATUS).use {
            it.setObject(1, senOppfolgingKandidat.status.name)
            it.setObject(2, senOppfolgingKandidat.uuid.toString())
            it.executeQuery().toList { getInt("id") }.single()
        }

    private fun Connection.createVurdering(kandidatId: Int, senOppfolgingVurdering: SenOppfolgingVurdering): PSenOppfolgingVurdering =
        prepareStatement(CREATE_VURDERING).use {
            it.setString(1, senOppfolgingVurdering.uuid.toString())
            it.setInt(2, kandidatId)
            it.setObject(3, senOppfolgingVurdering.createdAt)
            it.setString(4, senOppfolgingVurdering.veilederident)
            if (senOppfolgingVurdering.begrunnelse != null) {
                it.setString(5, senOppfolgingVurdering.begrunnelse)
            } else {
                it.setString(5, "")
            }
            it.setString(6, senOppfolgingVurdering.type.name)
            it.executeQuery().toList { toPSenOppfolgingVurdering() }.single()
        }

    private fun Connection.getVurderinger(kandidatId: Int) =
        prepareStatement(GET_VURDERINGER).use {
            it.setInt(1, kandidatId)
            it.executeQuery().toList { toPSenOppfolgingVurdering() }
        }

    companion object {
        private const val CREATE_KANDIDAT = """
            INSERT INTO SEN_OPPFOLGING_KANDIDAT (
                id,
                uuid,
                created_at,
                updated_at,
                personident,
                varsel_at,
                varsel_id
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)
            RETURNING *
        """

        private const val GET_KANDIDAT_BY_UUID = """
            SELECT * FROM SEN_OPPFOLGING_KANDIDAT WHERE uuid = ?
        """

        private const val FIND_KANDIDAT_BY_VARSEL_ID = """
            SELECT * FROM SEN_OPPFOLGING_KANDIDAT WHERE varsel_id = ? ORDER BY created_at DESC
        """

        private const val GET_KANDIDAT_BY_PERSONIDENT = """
            SELECT * FROM SEN_OPPFOLGING_KANDIDAT WHERE personident = ? ORDER BY created_at DESC
        """

        private const val CREATE_VURDERING = """
            INSERT INTO SEN_OPPFOLGING_VURDERING (
                id,
                uuid,
                kandidat_id,
                created_at,
                veilederident,
                begrunnelse,
                type
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)
            RETURNING *
        """

        private const val GET_VURDERINGER = """
                SELECT *
                FROM SEN_OPPFOLGING_VURDERING
                WHERE kandidat_id = ? ORDER BY created_at
            """

        private const val UPDATE_KANDIDAT_SVAR = """
            UPDATE SEN_OPPFOLGING_KANDIDAT SET
                svar_at = ?,
                onsker_oppfolging = ?,
                updated_at = NOW()
            WHERE uuid = ?
        """

        private const val UPDATE_KANDIDAT_STATUS = """
            UPDATE SEN_OPPFOLGING_KANDIDAT SET
                status = ?,
                updated_at = NOW()
            WHERE uuid = ? RETURNING id
        """

        private const val GET_UNPUBLISHED_KANDIDAT =
            """
                SELECT kandidat.* 
                FROM SEN_OPPFOLGING_KANDIDAT kandidat
                WHERE svar_at IS NOT NULL AND (
                    kandidat.published_at IS NULL OR EXISTS (
                        SELECT 1 FROM SEN_OPPFOLGING_VURDERING vurdering WHERE vurdering.kandidat_id = kandidat.id AND vurdering.published_at IS NULL
                    )
                )
                ORDER BY kandidat.created_at ASC
            """

        private const val UPDATE_KANDIDAT_PUBLISHED_AT =
            """
                UPDATE SEN_OPPFOLGING_KANDIDAT SET updated_at=?, published_at=? WHERE uuid=?
            """

        private const val UPDATE_VURDERING_PUBLISHED_AT =
            """
                UPDATE SEN_OPPFOLGING_VURDERING SET updated_at=?, published_at=? WHERE uuid=?
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
    varselId = getString("varsel_id")?.let { UUID.fromString(it) },
    svarAt = getObject("svar_at", OffsetDateTime::class.java),
    onskerOppfolging = getString("onsker_oppfolging"),
    publishedAt = getObject("published_at", OffsetDateTime::class.java),
    status = SenOppfolgingStatus.valueOf(getString("status")),
)

internal fun ResultSet.toPSenOppfolgingVurdering(): PSenOppfolgingVurdering = PSenOppfolgingVurdering(
    id = getInt("id"),
    uuid = UUID.fromString(getString("uuid")),
    kandidatId = getInt("kandidat_id"),
    createdAt = getObject("created_at", OffsetDateTime::class.java),
    veilederident = getString("veilederident"),
    publishedAt = getObject("published_at", OffsetDateTime::class.java),
    begrunnelse = getString("begrunnelse"),
    type = VurderingType.valueOf(getString("type")),
)
