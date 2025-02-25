package no.nav.syfo.infrastructure.database.repository

import no.nav.syfo.application.ISenOppfolgingRepository
import no.nav.syfo.domain.*
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
            pSenOppfolgingKandidat.toSenOppfolgingKandidat(vurdering = null)
        }

    override fun getKandidat(kandidatUuid: UUID): SenOppfolgingKandidat? = database.connection.use { connection ->
        connection.prepareStatement(GET_KANDIDAT_BY_UUID).use {
            it.setString(1, kandidatUuid.toString())
            it.executeQuery().toList { toPSenOppfolgingKandidat() }
        }.map {
            it.toSenOppfolgingKandidat(vurdering = connection.getVurdering(it.id))
        }.firstOrNull()
    }

    override fun updateKandidatPersonident(kandidater: List<SenOppfolgingKandidat>, newPersonident: Personident) {
        database.connection.use { connection ->
            connection.prepareStatement(UPDATE_KANDIDAT_PERSONIDENT).use {
                kandidater.forEach { kandidat ->
                    it.setString(1, newPersonident.value)
                    it.setString(2, kandidat.uuid.toString())
                    val updated = it.executeUpdate()
                    if (updated != 1) {
                        throw SQLException("Expected a single row to be updated, got update count $updated")
                    }
                }
            }
            connection.commit()
        }
    }

    override fun findKandidatFromVarselId(varselId: UUID): SenOppfolgingKandidat? =
        database.connection.use { connection ->
            connection.prepareStatement(FIND_KANDIDAT_BY_VARSEL_ID).use {
                it.setString(1, varselId.toString())
                it.executeQuery().toList { toPSenOppfolgingKandidat() }
            }.map {
                it.toSenOppfolgingKandidat(vurdering = connection.getVurdering(it.id))
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

    override fun vurderKandidat(
        senOppfolgingKandidat: SenOppfolgingKandidat,
        vurdering: SenOppfolgingVurdering,
    ): SenOppfolgingVurdering =
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
                it.toSenOppfolgingKandidat(vurdering = connection.getVurdering(it.id))
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

    override fun getKandidaterForPersoner(personidenter: List<Personident>): Map<Personident, SenOppfolgingKandidat> {
        return database.connection.use { connection ->
            connection.prepareStatement(GET_KANDIDATER).use { preparedStatement ->
                preparedStatement.setString(1, personidenter.joinToString(",") { it.value })
                preparedStatement.executeQuery().toList {
                    toPSenOppfolgingKandidat().toSenOppfolgingKandidat(
                        vurdering = if (getString("vurdering_uuid") != null) {
                            toPSenOppfolgingVurdering(colNamePrefix = "vurdering_")
                        } else null
                    )
                }
                    .associateBy { it.personident }
            }
        }
    }

    override fun getKandidater(personident: Personident): List<SenOppfolgingKandidat> =
        database.connection.use { connection ->
            connection.prepareStatement(GET_KANDIDAT_BY_PERSONIDENT).use {
                it.setString(1, personident.value)
                it.executeQuery().toList { toPSenOppfolgingKandidat() }
            }.map {
                it.toSenOppfolgingKandidat(vurdering = connection.getVurdering(it.id))
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

    private fun Connection.createVurdering(
        kandidatId: Int,
        senOppfolgingVurdering: SenOppfolgingVurdering,
    ): PSenOppfolgingVurdering =
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

    private fun Connection.getVurdering(kandidatId: Int) =
        prepareStatement(GET_VURDERINGER).use {
            it.setInt(1, kandidatId)
            it.executeQuery().toList { toPSenOppfolgingVurdering() }.firstOrNull()
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
        private const val UPDATE_KANDIDAT_PERSONIDENT = """
            UPDATE SEN_OPPFOLGING_KANDIDAT SET personident = ? WHERE uuid = ?
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
                WHERE kandidat.published_at IS NULL OR EXISTS (
                    SELECT 1 FROM SEN_OPPFOLGING_VURDERING vurdering WHERE vurdering.kandidat_id = kandidat.id AND vurdering.published_at IS NULL
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

        private const val GET_KANDIDATER =
            """
                SELECT k.*,
                   v.id as vurdering_id,
                   v.uuid as vurdering_uuid,
                   v.kandidat_id as vurdering_kandidat_id,
                   v.created_at as vurdering_created_at,
                   v.veilederident as vurdering_veilederident,
                   v.type as vurdering_type,
                   v.published_at as vurdering_published_at,
                   v.updated_at as vurdering_updated_at,
                   v.begrunnelse as vurdering_begrunnelse
                FROM (
                    SELECT DISTINCT ON (personident) *
                    FROM SEN_OPPFOLGING_KANDIDAT
                    WHERE personident = ANY (string_to_array(?, ','))
                    ORDER BY personident, created_at DESC
                ) k
                LEFT JOIN SEN_OPPFOLGING_VURDERING v ON k.id = v.kandidat_id
                ORDER BY k.created_at DESC;
                
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

internal fun ResultSet.toPSenOppfolgingVurdering(
    colNamePrefix: String = "",
): PSenOppfolgingVurdering {
    val vurderingId = getInt("${colNamePrefix}id")

    return PSenOppfolgingVurdering(
        id = vurderingId,
        uuid = UUID.fromString(getString("${colNamePrefix}uuid")),
        kandidatId = getInt("${colNamePrefix}kandidat_id"),
        createdAt = getObject("${colNamePrefix}created_at", OffsetDateTime::class.java),
        veilederident = getString("${colNamePrefix}veilederident"),
        publishedAt = getObject("${colNamePrefix}published_at", OffsetDateTime::class.java),
        begrunnelse = getString("${colNamePrefix}begrunnelse"),
        type = VurderingType.valueOf(getString("${colNamePrefix}type")),
    )
}
