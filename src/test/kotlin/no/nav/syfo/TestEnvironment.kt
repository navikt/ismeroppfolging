package no.nav.syfo

import no.nav.syfo.infrastructure.clients.ClientEnvironment
import no.nav.syfo.infrastructure.clients.ClientsEnvironment
import no.nav.syfo.infrastructure.clients.azuread.AzureEnvironment
import no.nav.syfo.infrastructure.database.DatabaseEnvironment
import no.nav.syfo.infrastructure.kafka.KafkaEnvironment

fun testEnvironment() = Environment(
    database = DatabaseEnvironment(
        host = "localhost",
        port = "5432",
        name = "ismeroppfolging_dev",
        username = "username",
        password = "password",
        url = "jdbc:postgresql://localhost:5432/ismeroppfolging_dev",
    ),
    kafka = KafkaEnvironment(
        aivenBootstrapServers = "kafkaBootstrapServers",
        aivenCredstorePassword = "credstorepassord",
        aivenKeystoreLocation = "keystore",
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = "truststore",
    ),
    azure = AzureEnvironment(
        appClientId = "ismeroppfolging-client-id",
        appClientSecret = "ismeroppfolging-secret",
        appWellKnownUrl = "wellknown",
        openidConfigTokenEndpoint = "azureOpenIdTokenEndpoint",
    ),
    clients = ClientsEnvironment(
        istilgangskontroll = ClientEnvironment(
            baseUrl = "isTilgangskontrollUrl",
            clientId = "dev-gcp.teamsykefravr.istilgangskontroll",
        ),
    ),
    electorPath = "electorPath",
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)
