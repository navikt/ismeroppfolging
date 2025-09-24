package no.nav.syfo.shared.infrastructure.database

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.syfo.kartleggingssporsmal.domain.KartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.infrastructure.database.PKartleggingssporsmalStoppunkt
import no.nav.syfo.kartleggingssporsmal.infrastructure.database.toPKartleggingssporsmalStoppunkt
import no.nav.syfo.senoppfolging.infrastructure.database.repository.PSenOppfolgingKandidat
import no.nav.syfo.senoppfolging.infrastructure.database.repository.PSenOppfolgingVurdering
import no.nav.syfo.senoppfolging.infrastructure.database.repository.toPSenOppfolgingKandidat
import no.nav.syfo.senoppfolging.infrastructure.database.repository.toPSenOppfolgingVurdering
import no.nav.syfo.shared.domain.Personident
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.Date
import java.sql.SQLException
import java.time.LocalDate
import java.util.UUID
import kotlin.use

class TestDatabase : DatabaseInterface {
    private val pg: EmbeddedPostgres = try {
        EmbeddedPostgres.start()
    } catch (e: Exception) {
        EmbeddedPostgres.builder().start()
    }

    private var shouldSimulateError = false
    private val workingConnection: Connection
        get() = pg.postgresDatabase.connection.apply { autoCommit = false }

    override val connection: Connection
        get() = if (shouldSimulateError) {
            throw Exception("Simulated database connection failure")
        } else {
            workingConnection
        }

    init {

        Flyway.configure().run {
            dataSource(pg.postgresDatabase).validateMigrationNaming(true).load().migrate()
        }
    }

    fun stop() {
        pg.close()
    }

    fun simulateDatabaseError() {
        shouldSimulateError = true
    }

    fun restoreDatabase() {
        shouldSimulateError = false
    }

    fun resetDatabase() {
        restoreDatabase()
        dropData()
    }
}

fun TestDatabase.dropData() {
    val queryList = listOf(
        """
        DELETE FROM SEN_OPPFOLGING_KANDIDAT
        """.trimIndent(),
        """
        DELETE FROM SEN_OPPFOLGING_VURDERING
        """.trimIndent(),
        """
        DELETE FROM KARTLEGGINGSSPORSMAL_STOPPUNKT
        """.trimIndent(),
        """
        DELETE FROM KARTLEGGINGSSPORSMAL_KANDIDAT
        """.trimIndent(),
    )
    this.connection.use { connection ->
        queryList.forEach { query ->
            connection.prepareStatement(query).execute()
        }
        connection.commit()
    }
}

fun TestDatabase.getSenOppfolgingKandidater(): List<PSenOppfolgingKandidat> =
    this.connection.use { connection ->
        connection.prepareStatement("SELECT * FROM SEN_OPPFOLGING_KANDIDAT ORDER BY created_at DESC").use {
            it.executeQuery().toList { toPSenOppfolgingKandidat() }
        }
    }

fun TestDatabase.getSenOppfolgingVurderinger(): List<PSenOppfolgingVurdering> =
    this.connection.use { connection ->
        connection.prepareStatement("SELECT * FROM SEN_OPPFOLGING_VURDERING").use {
            it.executeQuery().toList { toPSenOppfolgingVurdering() }
        }
    }

fun TestDatabase.getKartleggingssporsmalStoppunkt(): List<PKartleggingssporsmalStoppunkt> =
    this.connection.use { connection ->
        connection.prepareStatement("SELECT * FROM KARTLEGGINGSSPORSMAL_STOPPUNKT").use {
            it.executeQuery().toList { toPKartleggingssporsmalStoppunkt() }
        }
    }

fun TestDatabase.setStoppunktDate(uuid: UUID, stoppunkt: LocalDate) {
    this.connection.use { connection ->
        connection.prepareStatement("UPDATE KARTLEGGINGSSPORSMAL_STOPPUNKT SET stoppunkt_at=? WHERE uuid=?").use {
            it.setDate(1, Date.valueOf(stoppunkt))
            it.setString(2, uuid.toString())
            it.executeUpdate()
        }
        connection.commit()
    }
}

fun TestDatabase.markStoppunktAsProcessed(stoppunkt: KartleggingssporsmalStoppunkt) {
    this.connection.use { connection ->
        connection.prepareStatement("UPDATE KARTLEGGINGSSPORSMAL_STOPPUNKT SET processed_at = now() WHERE uuid = ?")
            .use {
                it.setString(1, stoppunkt.uuid.toString())
                val updated = it.executeUpdate()
                if (updated != 1) {
                    throw SQLException("Expected a single row to be updated, got update count $updated")
                }
            }
        connection.commit()
    }
}

fun TestDatabase.createKartleggingssporsmalMottattTable() {
    this.connection.use { connection ->
        val query = """
            CREATE TABLE IF NOT EXISTS KARTLEGGINGSSPORSMAL_MOTTATT (
                personident                 CHAR(11)    NOT NULL UNIQUE,
                mottatt_at                  timestamptz NOT NULL
            );
        """.trimIndent()
        connection.prepareStatement(query).execute()
        connection.commit()
    }
}

fun TestDatabase.insertKartleggingssporsmalMottatt(personIdent: Personident) {
    this.connection.use { connection ->
        val query = """
            INSERT INTO KARTLEGGINGSSPORSMAL_MOTTATT (personident, mottatt_at)
            VALUES (?, now())
            RETURNING *
        """.trimMargin()
        connection.prepareStatement(query)
            .use {
                it.setString(1, personIdent.value)
                it.executeQuery()
            }
        connection.commit()
    }
}

fun TestDatabase.deleteKartleggingssporsmalMottattRows() {
    val query = """DELETE FROM KARTLEGGINGSSPORSMAL_MOTTATT"""
    this.connection.use { connection ->
        connection.prepareStatement(query).execute()
        connection.commit()
    }
}
