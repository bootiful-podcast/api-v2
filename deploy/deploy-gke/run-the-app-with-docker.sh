#!/usr/bin/env bash

APP_NAME=bp-api
CONTAINER_NAME=${1:-$(docker images -q api)}
echo "the CONTAINER_NAME is ${CONTAINER_NAME}"
#CONTAINER_NAME=gcr.io/pgtm-jlong/${APP_NAME}

function read_kubernetes_secret() {
  kubectl get secret $1 -o jsonpath="{.data.$2}" | base64 --decode
}

RMQ_USER=$(read_kubernetes_secret bp-rabbitmq-secrets RABBITMQ_DEFAULT_USER)
RMQ_PW=$(read_kubernetes_secret bp-rabbitmq-secrets RABBITMQ_DEFAULT_PASS)

echo "----- RMQ -----"
echo $RMQ_USER
echo $RMQ_PW

PSQL_USER=$(read_kubernetes_secret bp-postgresql-secrets POSTGRES_USER)
echo $PSQL_USER

PSQL_PW=$(read_kubernetes_secret bp-postgresql-secrets POSTGRES_PASSWORD)
echo $PSQL_PW

LOCALHOST=host.docker.internal

echo $RMQ_PW
echo $RMQ_USER

docker run \
  -e SPRING_RABBITMQ_USERNAME="${RMQ_USER}" \
  -e SPRING_RABBITMQ_PASSWORD="${RMQ_PW}" \
  -e SPRING_RABBITMQ_HOST=$LOCALHOST \
  -e SPRING_PROFILES_ACTIVE=cloud \
  -e SENDGRID_API_KEY=$SENDGRID_API_KEY \
  -e SPRING_DATASOURCE_USERNAME="${PSQL_USER}" \
  -e SPRING_DATASOURCE_PASSWORD="${PSQL_PW}" \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://$LOCALHOST:5432/bp" \
  -e AWS_ACCESS_KEY_ID="$AWS_ACCESS_KEY_ID" \
  -e AWS_SECRET_ACCESS_KEY="$AWS_SECRET_ACCESS_KEY" \
  -e AWS_REGION=$AWS_REGION \
  -e PODBEAN_CLIENT_SECRET=$PODBEAN_CLIENT_SECRET \
  -e PODBEAN_CLIENT_ID=$PODBEAN_CLIENT_ID \
  -p 8080:8080 \
  $CONTAINER_NAME
