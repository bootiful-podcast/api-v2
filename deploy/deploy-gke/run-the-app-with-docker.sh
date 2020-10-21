#!/usr/bin/env bash
APP_NAME=bp-api
CONTAINER_NAME=gcr.io/pgtm-jlong/${APP_NAME}

docker pull $CONTAINER_NAME

docker run \
 -e SENDGRID_API_KEY=$SENDGRID_API_KEY \
 -e SENDGRID_API_KEY=$SENDGRID_API_KEY \
 $CONTAINER_NAME