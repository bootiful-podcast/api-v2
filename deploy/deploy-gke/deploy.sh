#!/usr/bin/env bash

function read_kubernetes_secret() {
  kubectl get secret $1 -o jsonpath="{.data.$2}" | base64 --decode
}

echo "Deploying Bootiful Podcast API Service to GKE..."
APP_NAME=bp-api
PROJECT_ID=pgtm-jlong
ROOT_DIR=$(cd `dirname $0`/../.. && pwd )
echo 'the root dir is' $ROOT_DIR
## TODO figure out how to get the test suite running in prod again
image_id=$(docker images -q api)
docker rmi -f $image_id || echo "there is not an existing image to delete..."

mvn -f ${ROOT_DIR}/pom.xml -DskipTests=true clean spring-javaformat:apply spring-boot:build-image
image_id=$(docker images -q api)
# todo restore these next 2 lines
docker tag "${image_id}" gcr.io/${PROJECT_ID}/${APP_NAME}
docker push gcr.io/${PROJECT_ID}/${APP_NAME}
#$ROOT_DIR/deploy/deploy-gke/run-the-app-with-docker.sh $image_id

RMQ_USER=$(read_kubernetes_secret bp-rabbitmq-secrets RABBITMQ_DEFAULT_USER)
RMQ_PW=$(read_kubernetes_secret bp-rabbitmq-secrets RABBITMQ_DEFAULT_PASS)

PSQL_USER=$(read_kubernetes_secret bp-postgresql-secrets POSTGRES_USER)
PSQL_PW=$(read_kubernetes_secret bp-postgresql-secrets POSTGRES_PASSWORD)


kubectl apply -f <(echo "
---
apiVersion: v1
kind: Secret
metadata:
  name: ${APP_NAME}-secrets
type: Opaque
stringData:
  SPRING_RABBITMQ_USERNAME: ${RMQ_USER}
  SPRING_RABBITMQ_PASSWORD: ${RMQ_PW}
  SPRING_RABBITMQ_HOST: rabbitmq
  SPRING_PROFILES_ACTIVE: cloud
  SENDGRID_API_KEY: "${SENDGRID_API_KEY}"
  SPRING_DATASOURCE_USERNAME: ${PSQL_USER}
  SPRING_DATASOURCE_PASSWORD: ${PSQL_PW}
  SPRING_DATASOURCE_URL:  jdbc:postgresql://postgres:5432/bp
  AWS_ACCESS_KEY_ID: "${AWS_ACCESS_KEY_ID}"
  AWS_SECRET_ACCESS_KEY: "${AWS_SECRET_ACCESS_KEY}"
  AWS_REGION: "${AWS_REGION}"
  PODBEAN_CLIENT_SECRET: "${PODBEAN_CLIENT_SECRET}"
  PODBEAN_CLIENT_ID: "${PODBEAN_CLIENT_ID}"
")

kubectl apply -f ${ROOT_DIR}/deploy/deploy-gke/bp-api.yaml
