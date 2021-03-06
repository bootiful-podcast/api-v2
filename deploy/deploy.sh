#!/usr/bin/env bash
set -e
set -o pipefail

export PROJECT_ID=${GCLOUD_PROJECT}

APP_NAME=api

#RMQ_USER=$BP_RABBITMQ_MANAGEMENT_USERNAME
#RMQ_PW=$BP_RABBITMQ_MANAGEMENT_PASSWORD
#PSQL_USER=$BP_POSTGRES_USERNAME
#PSQL_PW=$BP_POSTGRES_PASSWORD
#RMQ_USER=$BP_RABBITMQ_MANAGEMENT_USERNAME
#RMQ_PW=$BP_RABBITMQ_MANAGEMENT_PASSWORD
#PSQL_HOST=$BP_POSTGRES_HOST
#PSQL_USER=$BP_POSTGRES_USERNAME
#PSQL_PW=$BP_POSTGRES_PASSWORD
#PSQL_DB=$BP_POSTGRES_DB

API_YAML=${ROOT_DIR}/deploy/bp-api.yaml
API_SERVICE_YAML=${ROOT_DIR}/deploy/bp-api-service.yaml
SECRETS=${APP_NAME}-secrets

ROOT_DIR=$(cd $(dirname $0) && pwd)
BP_MODE_LOWERCASE=${BP_MODE_LOWERCASE:-development}
OD=${ROOT_DIR}/overlays/${BP_MODE_LOWERCASE}
SECRETS_FN=${ROOT_DIR}/overlays/development/${APP_NAME}-secrets.env

export IMAGE_TAG="${BP_MODE_LOWERCASE}${GITHUB_SHA:-}"
export GCR_IMAGE_NAME=gcr.io/${PROJECT_ID}/${APP_NAME}
export IMAGE_NAME=${GCR_IMAGE_NAME}:${IMAGE_TAG}
echo "OD=$OD"
echo "BP_MODE_LOWERCASE=$BP_MODE_LOWERCASE"
echo "GCR_IMAGE_NAME=$GCR_IMAGE_NAME"
echo "IMAGE_NAME=$IMAGE_NAME"
echo "IMAGE_TAG=$IMAGE_TAG"


docker rmi $(docker images -a -q )
mvn -f ${ROOT_DIR}/../pom.xml -DskipTests=true clean spring-javaformat:apply spring-boot:build-image
image_id=$(docker images -q $APP_NAME)
docker tag "${image_id}" $IMAGE_NAME
docker push $IMAGE_NAME
echo "pushing ${image_id} to $IMAGE_NAME "
echo "tagging ${GCR_IMAGE_NAME}"


export RESERVED_IP_NAME=${APP_NAME}-${BP_MODE_LOWERCASE}-ip
gcloud compute addresses list --format json | jq '.[].name' -r | grep $RESERVED_IP_NAME ||
  gcloud compute addresses create $RESERVED_IP_NAME --global
touch $SECRETS_FN
echo writing to "$SECRETS_FN "
cat <<EOF >${SECRETS_FN}
BP_MODE=${BP_MODE_LOWERCASE}
SPRING_RABBITMQ_USERNAME=${BP_RABBITMQ_MANAGEMENT_USERNAME}
SPRING_RABBITMQ_PASSWORD=${BP_RABBITMQ_MANAGEMENT_PASSWORD}
SPRING_RABBITMQ_HOST=${BP_RABBITMQ_MANAGEMENT_HOST}
SPRING_RABBITMQ_VIRTUAL_HOST=${BP_RABBITMQ_MANAGEMENT_VHOST}
SPRING_RABBITMQ_VIRTUALHOST=${BP_RABBITMQ_MANAGEMENT_VHOST}
SPRING_PROFILES_ACTIVE=cloud
SENDGRID_API_KEY=${SENDGRID_API_KEY}
SPRING_DATASOURCE_PASSWORD=${BP_POSTGRES_PASSWORD}
SPRING_DATASOURCE_USERNAME=${BP_POSTGRES_USERNAME}
SPRING_DATASOURCE_URL=jdbc:postgresql://${BP_POSTGRES_HOST}:5432/${BP_POSTGRES_DB}
AWS_REGION=${AWS_REGION}
AWS_ACCESS_KEY=${AWS_ACCESS_KEY_ID}
AWS_ACCESS_KEY_SECRET=${AWS_SECRET_ACCESS_KEY}
PODBEAN_CLIENT_ID=${PODBEAN_CLIENT_ID}
PODBEAN_CLIENT_SECRET=${PODBEAN_CLIENT_SECRET}
PODCAST_PIPELINE_S3_INPUT_BUCKET_NAME=${PODCAST_PIPELINE_S3_INPUT_BUCKET_NAME}
PODCAST_PIPELINE_S3_OUTPUT_BUCKET_NAME=${PODCAST_PIPELINE_S3_OUTPUT_BUCKET_NAME}
EOF

cd $OD
kustomize edit set image $GCR_IMAGE_NAME=$IMAGE_NAME
kustomize build ${OD} | kubectl apply -f -

rm $SECRETS_FN
