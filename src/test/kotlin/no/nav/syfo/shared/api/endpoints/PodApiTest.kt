package no.nav.syfo.shared.api.endpoints

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.syfo.ApplicationState
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.shared.api.auth.podEndpoints
import no.nav.syfo.shared.infrastructure.database.DatabaseInterface
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PodApiTest {

    private val database = ExternalMockEnvironment.Companion.instance.database

    private fun ApplicationTestBuilder.setupPodApi(database: DatabaseInterface, applicationState: ApplicationState) {
        application {
            routing {
                podEndpoints(
                    applicationState = applicationState,
                    database = database,
                )
            }
        }
    }

    @AfterEach
    fun tearDown() {
        database.resetDatabase()
    }

    @Nested
    @DisplayName("Successful liveness and readiness checks")
    inner class SuccessfulChecks {

        @Test
        fun `Returns ok on is_alive`() {
            testApplication {
                setupPodApi(
                    database = database,
                    applicationState = ApplicationState(alive = true, ready = true)
                )

                val response = client.get("/internal/is_alive")
                Assertions.assertTrue(response.status.isSuccess())
                Assertions.assertNotNull(response.bodyAsText())
            }
        }

        @Test
        fun `Returns ok on is_ready`() {
            testApplication {
                setupPodApi(
                    database = database,
                    applicationState = ApplicationState(alive = true, ready = true)
                )

                val response = client.get("/internal/is_ready")
                Assertions.assertTrue(response.status.isSuccess())
                Assertions.assertNotNull(response.bodyAsText())
            }
        }
    }

    @Nested
    @DisplayName("Unsuccessful liveness and readiness checks")
    inner class UnsuccessfulChecks {

        @Test
        fun `Returns internal server error when liveness check fails`() {
            testApplication {
                setupPodApi(
                    database = database,
                    applicationState = ApplicationState(alive = false, ready = false)
                )

                val response = client.get("/internal/is_alive")
                Assertions.assertEquals(HttpStatusCode.Companion.InternalServerError, response.status)
                Assertions.assertNotNull(response.bodyAsText())
            }
        }

        @Test
        fun `Returns internal server error when readiness check fails`() {
            testApplication {
                setupPodApi(
                    database = database,
                    applicationState = ApplicationState(alive = false, ready = false)
                )

                val response = client.get("/internal/is_ready")
                Assertions.assertEquals(HttpStatusCode.Companion.InternalServerError, response.status)
                Assertions.assertNotNull(response.bodyAsText())
            }
        }
    }

    @Nested
    @DisplayName("Successful liveness and unsuccessful readiness checks when database not working")
    inner class DatabaseNotWorking {

        @Test
        fun `Returns ok on is_alive`() {
            testApplication {
                setupPodApi(
                    database = database,
                    applicationState = ApplicationState(alive = true, ready = true)
                )

                database.simulateDatabaseError()

                val response = client.get("/internal/is_alive")
                Assertions.assertTrue(response.status.isSuccess())
                Assertions.assertNotNull(response.bodyAsText())
            }
        }

        @Test
        fun `Returns internal server error when readiness check fails`() {
            testApplication {
                setupPodApi(
                    database = database,
                    applicationState = ApplicationState(alive = true, ready = true)
                )

                database.simulateDatabaseError()

                val response = client.get("/internal/is_ready")
                Assertions.assertEquals(HttpStatusCode.Companion.InternalServerError, response.status)
                Assertions.assertNotNull(response.bodyAsText())
            }
        }
    }
}
