#!/usr/bin/env bash
set -euo pipefail

echo "=== DEPLOY: remove old bot container if exists ==="
docker rm -f compost-bot || true

echo "=== DEPLOY: rebuild bot image ==="
docker compose build bot

echo "=== DEPLOY: start bot ==="
docker compose up -d --no-deps bot
