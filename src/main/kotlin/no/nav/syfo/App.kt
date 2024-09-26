package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.api.apiModule
import no.nav.syfo.application.SenOppfolgingService
import no.nav.syfo.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.infrastructure.clients.wellknown.getWellKnown
import no.nav.syfo.infrastructure.cronjob.launchCronjobs
import no.nav.syfo.infrastructure.database.applicationDatabase
import no.nav.syfo.infrastructure.database.databaseModule
import no.nav.syfo.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.infrastructure.kafka.KandidatStatusProducer
import no.nav.syfo.infrastructure.kafka.KandidatStatusRecordSerializer
import no.nav.syfo.infrastructure.kafka.kafkaAivenProducerConfig
import no.nav.syfo.infrastructure.kafka.senoppfolging.launchSenOppfolgingSvarConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

const val applicationPort = 8080

fun main() {
    val applicationState = ApplicationState()
    val environment = Environment()
    val logger = LoggerFactory.getLogger("ktor.application")

    val wellKnownInternalAzureAD = getWellKnown(
        wellKnownUrl = environment.azure.appWellKnownUrl,
    )
    val azureAdClient = AzureAdClient(
        azureEnvironment = environment.azure
    )
    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = azureAdClient,
        clientEnvironment = environment.clients.istilgangskontroll
    )
    val kandidatStatusProducer = KandidatStatusProducer(
        producer = KafkaProducer(
            kafkaAivenProducerConfig<KandidatStatusRecordSerializer>(kafkaEnvironment = environment.kafka)
        )
    )

    lateinit var senOppfolgingService: SenOppfolgingService

    val applicationEngineEnvironment =
        applicationEngineEnvironment {
            log = logger
            config = HoconApplicationConfig(ConfigFactory.load())
            connector {
                port = applicationPort
            }
            module {
                databaseModule(
                    databaseEnvironment = environment.database,
                )

                val senOppfolgingRepository = SenOppfolgingRepository(database = applicationDatabase)

                senOppfolgingService = SenOppfolgingService(
                    senOppfolgingRepository = senOppfolgingRepository,
                    kandidatStatusProducer = kandidatStatusProducer,
                )

                apiModule(
                    applicationState = applicationState,
                    environment = environment,
                    wellKnownInternalAzureAD = wellKnownInternalAzureAD,
                    database = applicationDatabase,
                    veilederTilgangskontrollClient = veilederTilgangskontrollClient,
                    senOppfolgingService = senOppfolgingService,
                )
            }
        }

    applicationEngineEnvironment.monitor.subscribe(ApplicationStarted) {
        applicationState.ready = true
        logger.info("Application is ready, running Java VM ${Runtime.version()}")

        launchCronjobs(
            applicationState = applicationState,
            environment = environment,
            senOppfolgingService = senOppfolgingService,
        )

        launchSenOppfolgingSvarConsumer(
            applicationState = applicationState,
            kafkaEnvironment = environment.kafka,
            senOppfolgingService = senOppfolgingService,
        )
        /*
        launchSenOppfolgingVarselConsumer(
            applicationState = applicationState,
            kafkaEnvironment = environment.kafka,
            senOppfolgingService = senOppfolgingService,
        )*/
    }

    val server = embeddedServer(
        factory = Netty,
        environment = applicationEngineEnvironment
    ) {
        connectionGroupSize = 8
        workerGroupSize = 8
        callGroupSize = 16
    }

    Runtime.getRuntime().addShutdownHook(
        Thread { server.stop(10, 10, TimeUnit.SECONDS) }
    )

    server.start(wait = true)
}
