#!/bin/bash

GROUP_ID="eu.wdaqua.qanary"
GROUP_PATH=$(echo "$GROUP_ID" | tr '.' '/')

artifacts=(qanary_commons qanary_pipeline-template qanary_component-template qanary_component-parent)
artifacts_to_be_released=()

# The shared parent POM (mvn.reactor) must also be published to Maven Central so the
# released modules can resolve it as their parent. Check the reactor root pom and
# release it (as ".") when it is missing — deploy it first so it is in the bundle.
ROOT_ARTIFACT_ID=$(xmllint --xpath "/*[local-name()='project']/*[local-name()='artifactId']/text()" pom.xml)
ROOT_VERSION=$(xmllint --xpath "/*[local-name()='project']/*[local-name()='version']/text()" pom.xml)
ROOT_URL="https://repo1.maven.org/maven2/$GROUP_PATH/$ROOT_ARTIFACT_ID/$ROOT_VERSION/$ROOT_ARTIFACT_ID-$ROOT_VERSION.pom"
ROOT_HTTP_STATUS=$(curl --head --silent --output /dev/null --write-out "%{http_code}" "$ROOT_URL")
if [ "$ROOT_HTTP_STATUS" -eq 200 ]; then
    echo "Parent $ROOT_ARTIFACT_ID version $ROOT_VERSION exists in Maven Central."
else
    echo "Parent $ROOT_ARTIFACT_ID version $ROOT_VERSION does not exist in Maven Central; releasing the reactor root."
    artifacts_to_be_released+=(".")
fi

for artifact in "${artifacts[@]}"; do
    echo "Checking version in: $artifact"
    while read -r file; do
        echo "Found pom.xml in: $file"
        ARTIFACT_ID=$(xmllint --xpath "/*[local-name()='project']/*[local-name()='artifactId']/text()" "$file")
        VERSION=$(xmllint --xpath "/*[local-name()='project']/*[local-name()='version']/text()" "$file")
        echo "Artifact ID: $ARTIFACT_ID, Version: $VERSION"
        URL="https://repo1.maven.org/maven2/$GROUP_PATH/$ARTIFACT_ID/$VERSION/$ARTIFACT_ID-$VERSION.pom"
        HTTP_STATUS=$(curl --head --silent --output /dev/null --write-out "%{http_code}" "$URL")
        # Check the HTTP status code
        if [ "$HTTP_STATUS" -eq 200 ]; then
            echo "Artifact $ARTIFACT_ID version $VERSION exists in Maven Central."
        else
            echo "Artifact $ARTIFACT_ID version $VERSION does not exist in Maven Central."
            artifacts_to_be_released+=("$artifact")
        fi
    done < <(find "$artifact" -name "pom.xml")
done

# Join the array elements into a string with a comma as delimiter
ARTIFACTS_TO_BE_RELEASED_STR=$(IFS=","; echo "${artifacts_to_be_released[*]}")
echo "Releasable artifacts: $ARTIFACTS_TO_BE_RELEASED_STR"
# Use GitHub Actions command to set an environment variable
echo "ARTIFACTS_TO_BE_RELEASED=$ARTIFACTS_TO_BE_RELEASED_STR" >> "$GITHUB_ENV"