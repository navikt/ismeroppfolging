![Build status](https://github.com/navikt/ismeroppfolging/workflows/main/badge.svg?branch=main)

# ismeroppfolging

Applikasjon for prosessering av sykmeldte som har oppgitt at de trenger mer oppfølging i sen fase av sykepengeperioden, rundt 39 uker.

## Technologies used

* Docker
* Gradle
* Kafka
* Kotlin
* Ktor
* Postgres

##### Test Libraries:

* Kluent
* Mockk
* Spek

#### Requirements

* JDK 17

### Build

Run `./gradlew clean shadowJar`

### Lint (Ktlint)

##### Command line

Run checking: `./gradlew --continue ktlintCheck`

Run formatting: `./gradlew ktlintFormat`

##### Git Hooks

Apply checking: `./gradlew addKtlintCheckGitPreCommitHook`

Apply formatting: `./gradlew addKtlintFormatGitPreCommitHook`

## Contact

### For NAV employees

We are available at the Slack channel `#isyfo`.