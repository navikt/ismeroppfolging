package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.shared.api.apiModule
import no.nav.syfo.senoppfolging.application.SenOppfolgingService
import no.nav.syfo.shared.infrastructure.kafka.identhendelse.kafka.launchKafkaTaskIdenthendelse
import no.nav.syfo.senoppfolging.infrastructure.cronjob.PublishKandidatStatusCronjob
import no.nav.syfo.shared.infrastructure.clients.azuread.AzureAdClient
import no.nav.syfo.shared.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.shared.infrastructure.clients.wellknown.getWellKnown
import no.nav.syfo.shared.infrastructure.cronjob.launchCronjobs
import no.nav.syfo.shared.infrastructure.database.applicationDatabase
import no.nav.syfo.shared.infrastructure.database.databaseModule
import no.nav.syfo.senoppfolging.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.senoppfolging.infrastructure.kafka.producer.KandidatStatusProducer
import no.nav.syfo.senoppfolging.infrastructure.kafka.producer.KandidatStatusRecordSerializer
import no.nav.syfo.jobbforventning.infrastructure.kafka.launchOppfolgingstilfelleConsumer
import no.nav.syfo.shared.infrastructure.kafka.kafkaAivenProducerConfig
import no.nav.syfo.senoppfolging.infrastructure.kafka.consumer.launchSenOppfolgingSvarConsumer
import no.nav.syfo.senoppfolging.infrastructure.kafka.consumer.launchSenOppfolgingVarselConsumer
import no.nav.syfo.shared.infrastructure.cronjob.Cronjob
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
        applicationEnvironment {
            log = logger
            config = HoconApplicationConfig(ConfigFactory.load())
        }
    val server = embeddedServer(
        factory = Netty,
        environment = applicationEngineEnvironment,
        configure = {
            connector {
                port = applicationPort
            }
            connectionGroupSize = 8
            workerGroupSize = 8
            callGroupSize = 16
        },
        module = {
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
            monitor.subscribe(ApplicationStarted) {
                applicationState.ready = true
                logger.info("Application is ready, running Java VM ${Runtime.version()}")

                val cronjobs = listOf<Cronjob>(
                    PublishKandidatStatusCronjob(senOppfolgingService),
                )

                launchCronjobs(
                    applicationState = applicationState,
                    environment = environment,
                    cronjobs = cronjobs,
                )

                launchSenOppfolgingSvarConsumer(
                    applicationState = applicationState,
                    kafkaEnvironment = environment.kafka,
                    senOppfolgingService = senOppfolgingService,
                )
                launchSenOppfolgingVarselConsumer(
                    applicationState = applicationState,
                    kafkaEnvironment = environment.kafka,
                    senOppfolgingService = senOppfolgingService,
                )
                launchKafkaTaskIdenthendelse(
                    applicationState = applicationState,
                    kafkaEnvironment = environment.kafka,
                    senOppfolgingRepository = senOppfolgingRepository,
                )
                if (environment.isOppfolgingstilfelleConsumerEnabled) {
                    launchOppfolgingstilfelleConsumer(
                        applicationState = applicationState,
                        kafkaEnvironment = environment.kafka,
                    )
                }
            }
        }
    )

    Runtime.getRuntime().addShutdownHook(
        Thread { server.stop(10, 10, TimeUnit.SECONDS) }
    )

    server.start(wait = true)
}
