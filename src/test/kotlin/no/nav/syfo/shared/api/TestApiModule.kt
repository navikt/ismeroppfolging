package no.nav.syfo.shared.api

import io.ktor.server.application.*
import io.mockk.mockk
import no.nav.syfo.ExternalMockEnvironment
import no.nav.syfo.kartleggingssporsmal.application.KartleggingssporsmalService
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.EsyfovarselHendelse
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.EsyfovarselProducer
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.KartleggingssporsmalKandidatProducer
import no.nav.syfo.kartleggingssporsmal.infrastructure.kafka.KartleggingssporsmalKandidatStatusRecord
import no.nav.syfo.senoppfolging.application.SenOppfolgingService
import no.nav.syfo.senoppfolging.infrastructure.database.repository.SenOppfolgingRepository
import no.nav.syfo.shared.infrastructure.clients.veiledertilgang.VeilederTilgangskontrollClient
import org.apache.kafka.clients.producer.KafkaProducer

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
    kartleggingssporsmalServiceMock: KartleggingssporsmalService? = null,
) {
    val database = externalMockEnvironment.database
    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = externalMockEnvironment.azureAdClient,
        clientEnvironment = externalMockEnvironment.environment.clients.istilgangskontroll,
        httpClient = externalMockEnvironment.mockHttpClient,
    )
    val senOppfolgingService = SenOppfolgingService(
        senOppfolgingRepository = SenOppfolgingRepository(database),
        kandidatStatusProducer = mockk(relaxed = true),
    )

    val mockEsyfoVarselProducer = mockk<KafkaProducer<String, EsyfovarselHendelse>>()
    val esyfovarselProducer = EsyfovarselProducer(mockEsyfoVarselProducer)

    val mockKandidatProducer = mockk<KafkaProducer<String, KartleggingssporsmalKandidatStatusRecord>>()
    val kartleggingssporsmalKandidatProducer = KartleggingssporsmalKandidatProducer(mockKandidatProducer)

    val kartleggingssporsmalService = kartleggingssporsmalServiceMock ?: KartleggingssporsmalService(
        behandlendeEnhetClient = externalMockEnvironment.behandlendeEnhetClient,
        kartleggingssporsmalRepository = externalMockEnvironment.kartleggingssporsmalRepository,
        oppfolgingstilfelleClient = externalMockEnvironment.oppfolgingstilfelleClient,
        esyfoVarselProducer = esyfovarselProducer,
        kartleggingssporsmalKandidatProducer = kartleggingssporsmalKandidatProducer,
        pdlClient = externalMockEnvironment.pdlClient,
        vedtak14aClient = externalMockEnvironment.vedtak14aClient,
        isKandidatPublishingEnabled = true,
    )

    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        environment = externalMockEnvironment.environment,
        wellKnownInternalAzureAD = externalMockEnvironment.wellKnownInternalAzureAD,
        database = database,
        veilederTilgangskontrollClient = veilederTilgangskontrollClient,
        senOppfolgingService = senOppfolgingService,
        kartleggingssporsmalService = kartleggingssporsmalService,
    )
}
