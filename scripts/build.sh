#!/usr/bin/env bash
set -euo pipefail

echo "=== BUILD: Maven package ==="
mvn clean package -DskipTests

echo "=== BUILD: Docker image ==="
docker build \
  --tag tg_bot-bot:latest \
  --file Dockerfile \
  .
