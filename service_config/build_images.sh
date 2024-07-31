#!/bin/bash

# replace secrets
if [ -z "$VIRTUOSO_PASSWORD" ]
then
  echo "VIRTUOSO_PASSWORD is not set. Check your secrets."
  exit
else
  sed -i "s/SECRETS_VIRTUOSO_PASSWORD/$VIRTUOSO_PASSWORD/g" ./service_config/files/pipeline
fi

# build and push Docker Images
if ! mvn -B --settings ./service_config/settings.xml clean install docker:build docker:push -DskipTests -Dgpg.skip=true;
then
  # build failed
  exit 1
fi
