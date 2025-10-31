package no.nav.syfo

import no.nav.syfo.shared.infrastructure.clients.ClientEnvironment
import no.nav.syfo.shared.infrastructure.clients.ClientsEnvironment
import no.nav.syfo.shared.infrastructure.clients.azuread.AzureEnvironment
import no.nav.syfo.shared.infrastructure.database.DatabaseEnvironment
import no.nav.syfo.shared.infrastructure.kafka.KafkaEnvironment

const val NAIS_DATABASE_ENV_PREFIX = "NAIS_DATABASE_ISMEROPPFOLGING_ISMEROPPFOLGING_DB"

data class Environment(
    val database: DatabaseEnvironment = DatabaseEnvironment(
        host = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_HOST"),
        port = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_PORT"),
        name = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_DATABASE"),
        username = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_USERNAME"),
        password = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_PASSWORD"),
        url = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_JDBC_URL")
    ),
    val kafka: KafkaEnvironment = KafkaEnvironment(
        aivenBootstrapServers = getEnvVar("KAFKA_BROKERS"),
        aivenCredstorePassword = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
        aivenKeystoreLocation = getEnvVar("KAFKA_KEYSTORE_PATH"),
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
        aivenSchemaRegistryUrl = getEnvVar("KAFKA_SCHEMA_REGISTRY"),
        aivenRegistryUser = getEnvVar("KAFKA_SCHEMA_REGISTRY_USER"),
        aivenRegistryPassword = getEnvVar("KAFKA_SCHEMA_REGISTRY_PASSWORD"),
    ),
    val azure: AzureEnvironment =
        AzureEnvironment(
            appClientId = getEnvVar("AZURE_APP_CLIENT_ID"),
            appClientSecret = getEnvVar("AZURE_APP_CLIENT_SECRET"),
            appWellKnownUrl = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),
            openidConfigTokenEndpoint = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")
        ),
    val electorPath: String = getEnvVar("ELECTOR_PATH"),
    val clients: ClientsEnvironment =
        ClientsEnvironment(
            istilgangskontroll = ClientEnvironment(
                baseUrl = getEnvVar("ISTILGANGSKONTROLL_URL"),
                clientId = getEnvVar("ISTILGANGSKONTROLL_CLIENT_ID"),
            ),
            syfobehandlendeenhet = ClientEnvironment(
                baseUrl = getEnvVar("SYFOBEHANDLENDEENHET_URL"),
                clientId = getEnvVar("SYFOBEHANDLENDEENHET_CLIENT_ID"),
            ),
            pdl = ClientEnvironment(
                baseUrl = getEnvVar("PDL_URL"),
                clientId = getEnvVar("PDL_CLIENT_ID"),
            ),
            dokarkiv = ClientEnvironment(
                baseUrl = getEnvVar("DOKARKIV_URL"),
                clientId = getEnvVar("DOKARKIV_CLIENT_ID"),
            ),
            veilarbvedtaksstotte = ClientEnvironment(
                baseUrl = getEnvVar("VEILARBVEDTAKSSTOTTE_URL"),
                clientId = getEnvVar("VEILARBVEDTAKSSTOTTE_CLIENT_ID"),
            ),
            isoppfolgingstilfelle = ClientEnvironment(
                baseUrl = getEnvVar("ISOPPFOLGINGSTILFELLE_URL"),
                clientId = getEnvVar("ISOPPFOLGINGSTILFELLE_CLIENT_ID"),
            ),
            esyfopdfgen = ClientEnvironment(
                baseUrl = getEnvVar("ESYFO_PDFGEN_URL"),
                clientId = getEnvVar("ESYFO_PDFGEN_CLIENT_ID"),
            ),
        ),
    val isJournalforingRetryEnabled: Boolean =
        getEnvVar("IS_JOURNALFORING_RETRY_ENABLED", "true").toBoolean(),
    val isKandidatPublishingEnabled: Boolean = getEnvVar("IS_KANDIDAT_PUBLISHING_ENABLED").toBoolean(),
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

fun isLocal() = getEnvVar("KTOR_ENV", "local") == "local"
