package no.nav.syfo.infrastructure.database

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import no.nav.syfo.infrastructure.database.repository.PSenOppfolgingKandidat
import no.nav.syfo.infrastructure.database.repository.PSenOppfolgingVurdering
import no.nav.syfo.infrastructure.database.repository.toPSenOppfolgingKandidat
import no.nav.syfo.infrastructure.database.repository.toPSenOppfolgingVurdering
import org.flywaydb.core.Flyway
import java.sql.Connection

class TestDatabase : DatabaseInterface {
    private val pg: EmbeddedPostgres = try {
        EmbeddedPostgres.start()
    } catch (e: Exception) {
        EmbeddedPostgres.builder().start()
    }

    override val connection: Connection
        get() = pg.postgresDatabase.connection.apply { autoCommit = false }

    init {

        Flyway.configure().run {
            dataSource(pg.postgresDatabase).validateMigrationNaming(true).load().migrate()
        }
    }

    fun stop() {
        pg.close()
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

class TestDatabaseNotResponding : DatabaseInterface {

    override val connection: Connection
        get() = throw Exception("Not working")
}
