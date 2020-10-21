#!/usr/bin/env bash


echo "Deploying Bootiful Podcast API Service to GKE..."
APP_NAME=bp-api
PROJECT_ID=pgtm-jlong
ROOT_DIR=$(cd `dirname $0`/../.. && pwd )

echo 'the root dir is' $ROOT_DIR

## TODO figure out how to get the test suite running in prod again
mvn -f ${ROOT_DIR}/pom.xml -DskipTests=true spring-boot:build-image
image_id=$(docker images -q api)
docker tag "${image_id}" gcr.io/${PROJECT_ID}/${APP_NAME}
docker push gcr.io/${PROJECT_ID}/${APP_NAME}
#kubectl apply -f ${root_dir}/deploy/deployment.yaml
