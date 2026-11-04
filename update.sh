#!/bin/bash
# Builds the app and replaces the running "budget" container with the new version.
set -euxo pipefail

DATA_DIR="/home/alan/budget-data"   # holds budget.db
mkdir -p "$DATA_DIR"

./gradlew clean bootJar

COMMIT_HASH=$(git rev-parse --short HEAD)

docker build -t alfy32/budget:$COMMIT_HASH .

docker stop budget || true
docker rm budget || true

# Runs as your user so budget.db stays owned by you (backup.sh and IntelliJ can open it).
docker run -d \
  --name budget \
  --restart=unless-stopped \
  --network=host \
  --user "$(id -u):$(id -g)" \
  -v "$DATA_DIR":/data \
  alfy32/budget:$COMMIT_HASH
