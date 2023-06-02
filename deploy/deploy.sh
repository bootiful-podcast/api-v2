#!/usr/bin/env bash
set -e
set -o pipefail


APP_NAME=api
SECRETS=${APP_NAME}-secrets
SECRETS_FN=${APP_NAME}-secrets
IMAGE_NAME=gcr.io/${GCLOUD_PROJECT}/${APP_NAME}
RESERVED_IP_NAME=bootiful-podcast-${APP_NAME}-ip

echo "BP_MODE_LOWERCASE=$BP_MODE_LOWERCASE"
echo "IMAGE_NAME=$IMAGE_NAME"
echo "IMAGE_TAG=$IMAGE_TAG"
echo "APP_NAME=$APP_NAME"

gcloud compute addresses list --format json | jq '.[].name' -r | grep $RESERVED_IP_NAME || gcloud compute addresses create $RESERVED_IP_NAME --global


docker images -q $IMAGE_NAME | while read  l ; do docker rmi $l -f ; done


./mvnw -DskipTests=true  clean package spring-boot:build-image -Dspring-boot.build-image.imageName=$IMAGE_NAME


echo "pushing ${IMAGE_ID}  "
docker push $IMAGE_NAME

rm -rf $SECRETS_FN
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


echo "SECRETS==========="
echo $SECRETS_FN
kubectl delete secrets $SECRETS || echo "no secrets to delete."
kubectl create secret generic $SECRETS --from-env-file $SECRETS_FN
kubectl delete -f $ROOT_DIR/deploy/k8s/deployment.yaml || echo "couldn't delete the deployment as there was nothing deployed."
kubectl apply -f $ROOT_DIR/deploy/k8s

