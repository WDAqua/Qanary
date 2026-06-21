# Building Qanary

## Prerequisites

- **JDK 21+** (the modules compile to Java 21; the build *enforces* this via
  `maven-enforcer-plugin` and fails early on older JDKs).
- **Maven 3.6.3+**.
- Docker (only for the Testcontainers integration tests and the end-to-end example).

## Common commands

```bash
mvn clean install              # full reactor build + unit/Spring tests + install
mvn clean test                 # build + run the unit and Spring-context tests
mvn clean install -Dmaven.test.skip=true   # build the artifacts (jars) without tests
```

Quality tooling (see also the workflows under `.github/workflows/`):

```bash
# Coverage (JaCoCo) – report + per-class 80% advisory gate run with `mvn test`;
# print a summary / regression gate:
python3 service_config/jacoco-coverage-summary.py

# Static analysis (SpotBugs + FindSecBugs) – report, then the high-severity gate:
mvn com.github.spotbugs:spotbugs-maven-plugin:spotbugs
mvn com.github.spotbugs:spotbugs-maven-plugin:check \
    -Dspotbugs.includeFilterFile=service_config/spotbugs-enforced.xml -Dspotbugs.failOnError=true

# Formatting drift (Spotless, lightweight rules); adopt with :apply
mvn -pl qanary_commons,qanary_pipeline-template,qanary_component-template,qald-evaluator \
    com.diffplug.spotless:spotless-maven-plugin:check

# Mutation testing (PIT) on the core module (slow):
mvn -pl qanary_commons org.pitest:pitest-maven:mutationCoverage -Djacoco.skip=true

# Testcontainers integration tests (*IT) – require Docker:
mvn -pl qanary_commons test -Dtest='*IT' -Dsurefire.failIfNoSpecifiedTests=false
```

## Known build gotcha: AspectJ "key not found in wovenClassFile"

The pipeline and component modules weave the `qa.commons` dependency into their own
classes at compile time (`aspectj-maven-plugin` `weaveDependencies`, for the
explainability aspect). Because `qa.commons` is itself already woven by its own
build, the consumers *re-weave* already-woven classes. The AspectJ weaver
(`1.14.1` / `aspectjtools 1.9.25`) has an **intermittent** bug here that aborts the
build with:

```
abort ABORT -- (RuntimeException) key not found in wovenClassFile
```

It is non-deterministic and shows up mainly during `aspectj:test-compile` of
`qa.pipeline`, usually after interleaving partial/in-place builds. It is a
**build-tooling flake, not a code error** — the same sources build cleanly on a
clean pass.

**Reliable recipes:**

| Situation | Command |
| --- | --- |
| Normal build | `mvn clean install` (full reactor — most reliable) |
| Build the runnable jar only (skips the test-compile weave entirely) | `mvn clean install -Dmaven.test.skip=true` |
| Flake persists across clean builds | `mvn clean && rm -rf ~/.m2/repository/eu/wdaqua/qanary && mvn clean install` |

**Avoid** repeated in-place `mvn -pl <module> test` runs without `clean`; they tend
to corrupt the incremental weave state. Prefer a full-reactor `mvn clean test`, and
in CI always start from a clean checkout (which is unaffected).

A proper fix (tracked as a follow-up) is to evaluate a newer AspectJ
weaver/plugin or load-time weaving so the dependency is not re-woven at build time.
