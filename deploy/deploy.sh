#!/usr/bin/env bash

APP_NAME=api

#PODBEAN_CLIENT_SECRET=PODBEAN_CLIENT_SECRET_${BP_MODE}
#PODBEAN_CLIENT_SECRET=${!PODBEAN_CLIENT_SECRET}
#
#PODBEAN_CLIENT_ID=PODBEAN_CLIENT_ID_${BP_MODE}
#PODBEAN_CLIENT_ID=${!PODBEAN_CLIENT_ID}
#


RMQ_USER=$BP_RABBITMQ_MANAGEMENT_USERNAME
RMQ_PW=$BP_RABBITMQ_MANAGEMENT_PASSWORD

PSQL_USER=$BP_POSTGRES_USERNAME
PSQL_PW=$BP_POSTGRES_PASSWORD


ROOT_DIR=$(cd $(dirname $0)/.. && pwd)
API_YAML=${ROOT_DIR}/deploy/bp-api.yaml
API_SERVICE_YAML=${ROOT_DIR}/deploy/bp-api-service.yaml
SECRETS=${APP_NAME}-secrets

## TODO figure out how to get the test suite running in prod again
image_id=$(docker images -q api)
docker rmi -f $image_id || echo "there is not an existing image to delete..."

mvn -f ${ROOT_DIR}/pom.xml -DskipTests=true clean spring-javaformat:apply spring-boot:build-image
image_id=$(docker images -q api)
docker tag "${image_id}" gcr.io/${GCLOUD_PROJECT}/${APP_NAME}
docker push gcr.io/${GCLOUD_PROJECT}/${APP_NAME}

kubectl delete secrets ${SECRETS} || echo "could not delete ${SECRETS}."
kubectl delete -f $API_YAML || echo "could not delete the existing Kubernetes environment as described in ${API_YAML}."
kubectl apply -f <(echo "
---
apiVersion: v1
kind: Secret
metadata:
  name: ${SECRETS}
type: Opaque
stringData:
  SPRING_RABBITMQ_USERNAME: ${BP_RABBITMQ_MANAGEMENT_USERNAME}
  SPRING_RABBITMQ_PASSWORD: ${BP_RABBITMQ_MANAGEMENT_PASSWORD}
  SPRING_RABBITMQ_HOST: rabbitmq
  SPRING_PROFILES_ACTIVE: cloud
  SENDGRID_API_KEY: "${SENDGRID_API_KEY}"
  SPRING_DATASOURCE_USERNAME: ${PSQL_USER}
  SPRING_DATASOURCE_PASSWORD: ${PSQL_PW}
  SPRING_DATASOURCE_URL:  jdbc:postgresql://postgres:5432/bp
  AWS_ACCESS_KEY: "${AWS_ACCESS_KEY_ID}"
  AWS_ACCESS_KEY_SECRET: "${AWS_SECRET_ACCESS_KEY}"
  AWS_REGION: "${AWS_REGION}"
  PODBEAN_CLIENT_SECRET: "${PODBEAN_CLIENT_SECRET}"
  PODBEAN_CLIENT_ID: "${PODBEAN_CLIENT_ID}"
")

kubectl apply -f $API_YAML

kubectl get service | grep $APP_NAME || kubectl apply -f $API_SERVICE_YAML