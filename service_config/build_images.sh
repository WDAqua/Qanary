#!/bin/bash

# replace secrets
if [ -z "$VIRTUOSO_PASSWORD" ]
then
  echo "VIRTUOSO_PASSWORD is not set. Check your secrets."
  exit
else
  sed -i "s/SECRETS_VIRTUOSO_PASSWORD/$VIRTUOSO_PASSWORD/g" ./service_config/files/pipeline
fi

# create settings.xml
export LocalMavenM2Dir="$env:USERPROFILE\.m2\settings.xml"
echo $LocalMavenM2Dir
touch $LocalMavenM2Dir
cat ./service_config/settings.xml>$LocalMavenM2Dir

# build and push Docker Images
if ! mvn -B clean install docker:build docker:push -DskipTests -Dgpg.skip=true;
then
  # build failed
  exit 1
fi
