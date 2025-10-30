#!/bin/bash
set -x

cd web-ui

ng build

cd ..

./gradlew bootJar

COMMIT_HASH=$(git rev-parse --short HEAD)

docker build -t alfy32/budget:$COMMIT_HASH .

docker stop budget
docker rm budget

docker run -d \
  --name budget \
  --restart=unless-stopped \
  --network=host \
  alfy32/budget:$COMMIT_HASH