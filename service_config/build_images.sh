#!/bin/bash
#
# Builds and pushes the public Qanary Docker images, then leaves the pipeline
# deployment config ready for the docker-service-updater step that runs next in
# the workflow (.github/workflows/docker-deployment.yml).
#
# SECRET HANDLING (task 1.6):
#   service_config/files/pipeline is committed with the placeholder
#   SECRETS_VIRTUOSO_PASSWORD. It is the .env that docker-service-updater
#   injects into the *running* qanary/qanary-pipeline container at DEPLOY time
#   (see service_config.json). It is NOT referenced by any Dockerfile, so the
#   credential is injected at runtime and never baked into an image layer.
#
#   The real value comes from the CI secret $VIRTUOSO_PASSWORD and is
#   substituted below. The substituted file must persist for the updater step
#   that follows, so it is intentionally left in place here. Therefore:
#     * run this only in ephemeral CI, never on a developer checkout, and
#     * never commit service_config/files/pipeline with a real password.
#   (A real password was committed once and lives in git history -> that
#   credential must be rotated; see task 1.6 notes.)

set -euo pipefail

ENV_FILE="./service_config/files/pipeline"

# replace secrets
if [ -z "${VIRTUOSO_PASSWORD:-}" ]; then
  echo "ERROR: VIRTUOSO_PASSWORD is not set. Check your CI secrets." >&2
  # fail loudly instead of silently exiting 0 (which would push images and run
  # the deploy step with the unreplaced placeholder password)
  exit 1
fi

sed -i "s/SECRETS_VIRTUOSO_PASSWORD/${VIRTUOSO_PASSWORD}/g" "$ENV_FILE"

# build and push Docker Images (set -e fails the script if Maven fails)
mvn -B --settings ./service_config/settings.xml clean install docker:build docker:push -DskipTests -Dgpg.skip=true

# build and push the static project-page image (qanary/qanary-webpage). The same
# builder powers the standalone webpage deployment workflow
# (.github/workflows/docker-deployment-webpage.yml).
bash ./service_config/build_webpage_image.sh
