#!/bin/bash
#
# Builds the static Qanary project page (the GitHub Pages content under docs/)
# into a small nginx image and pushes it to Docker Hub as qanary/qanary-webpage.
#
# Reused by both:
#   - service_config/build_images.sh        (the full deployment build), and
#   - .github/workflows/docker-deployment-webpage.yml  (standalone webpage deploy)
#
# The caller must have authenticated to Docker Hub first (docker/login-action).

set -euo pipefail

IMAGE="qanary/qanary-webpage"
TAG="${WEBPAGE_IMAGE_TAG:-latest}"

# build from the docs/ folder (its Dockerfile serves the static site via nginx)
docker build -t "${IMAGE}:${TAG}" ./docs

# push to Docker Hub
docker push "${IMAGE}:${TAG}"
