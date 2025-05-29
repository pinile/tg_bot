#!/usr/bin/env bash
set -euo pipefail

echo "=== DEPLOY: rebuild bot image and restart container ==="
docker compose build bot
docker compose up -d --no-deps --force-recreate bot