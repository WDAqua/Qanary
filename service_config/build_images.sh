#!/bin/bash

# replace secrets
if [ -z "$VIRTUOSO_PASSWORD" ]
then
  echo "VIRTUOSO_PASSWORD is not set. Check your secrets."
  exit
else
  sed -i "s/SECRETS_VIRTUOSO_PASSWORD/$VIRTUOSO_PASSWORD/g" ./service_config/files/pipeline
fi

# build Docker Images and store name and tag
if ! mvn clean install -DskipTests;
then
  # build failed
  exit 1
fi

echo "Docker images"
docker image ls
docker image ls | grep -oP "qanary.*\.[0-9] " > images.temp

echo "Locally available Docker images:"
cat images.temp

# read image list
images=$(cat images.temp)

i=0

# for each image
for row in $images
do
  # row contains the image name
  if [ $i -eq 0 ]
  then
    # store image name
    file_name=$row
    i=$((i + 1))
  # row contains tag
  else
    # generate version and latest tag
    latest_file_name="${file_name}:latest"
    file_name="${file_name}:${row}"

    i=0

    # tag images and push to Dockerhub
    docker tag "${file_name}" "${file_name}"
    docker tag "${file_name}" "${latest_file_name}"
    docker push "${file_name}"
    docker push "${latest_file_name}"
  fi
done

# delete temp results
rm images.temp
