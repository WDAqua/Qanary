# Contributing to Qanary

Thanks for your interest in improving Qanary! This guide covers the build
prerequisites, how to run the tests, and the release process.

## Prerequisites

- **Java 17** (LTS). The whole project targets Java 17 — see `<java.version>` in
  the module POMs. Install e.g. from <https://adoptium.net/>.
- **Maven 3.6.3 or higher** (the CI builds on 3.9.x).
- **Docker** (only needed to build/run the Docker images and the end-to-end
  example).

## Project layout

This is a multi-module Maven reactor (`mvn.reactor` in the root `pom.xml`):

| Module | Artifact | Purpose |
| --- | --- | --- |
| `qanary_commons` | `qa.commons` | core library (triplestore connectors, messages, aspects) |
| `qanary_pipeline-template` | `qa.pipeline` | the pipeline (runnable Spring Boot app + Docker image) |
| `qanary_component-template` | `qa.component` | base for building a component |
| `qanary_component-parent` | `qa.qanarycomponent-parent` | parent POM for components |
| `qanary_component-archetype` | `qa.qanarycomponent-archetype` | Maven archetype to scaffold a component |
| `qald-evaluator` | `qald.evaluator` | QALD benchmark client |

The Qanary question-answering **components** live in a separate repository:
<https://github.com/WDAqua/Qanary-question-answering-components>.

## Build & test

```bash
# full build with unit tests, no Docker images, no GPG signing
mvn clean install -Ddockerfile.skip=true -Dgpg.skip

# run only the tests
mvn test

# build a single module (and what it needs)
mvn -pl qanary_pipeline-template -am clean install -Dgpg.skip
```

To build the pipeline Docker image locally and run the end-to-end example, use
the helper script in the workspace root: `build-and-test-qanary.sh`.

## Pull requests

- Branch off `master`; keep one logical change per PR.
- Make sure `mvn clean install` passes (CI runs the same via
  `.github/workflows/pull-request-tests.yml`).
- Dependency bumps are handled automatically by Dependabot
  (`.github/dependabot.yml`); please don't hand-roll routine version bumps.

## Release process

Releases are published to Maven Central via the Sonatype Central portal using the
`central-publishing-maven-plugin` configured in the module POMs.

1. Bump the artifact versions in the relevant module POMs.
2. `service_config/version_check.sh` reports which artifacts are not yet on
   Maven Central (i.e. still need releasing).
3. Publishing requires GPG signing (`maven-gpg-plugin`) and the Central portal
   credentials (`MAVEN_TOKEN_USERNAME` / `MAVEN_TOKEN_PASSWORD`); see
   `.github/workflows/maven.yml`.
4. The public Docker images are built and pushed by
   `service_config/build_images.sh` (run from `.github/workflows/docker-deployment.yml`).
