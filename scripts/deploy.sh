#!/usr/bin/env bash
set -euo pipefail

MODE="${DEPLOY_MODE:-partial}"

echo "=== DEPLOY MODE: $MODE ==="


cat <<EOF > .env
BOT_TOKEN=${BOT_TOKEN}
MONGO_DATABASE_NAME=${MONGO_DATABASE_NAME}
MONGO_ROOT_USERNAME=${MONGO_ROOT_USERNAME}
MONGO_ROOT_PASSWORD=${MONGO_ROOT_PASSWORD}
EOF

if [[ "$MODE" == "full" ]]; then
  echo ">> Full stack deployment (excluding Jenkins)"
  docker compose down --remove-orphans
  docker compose build bot mongo mongo-express
  docker compose up -d bot mongo mongo-express
else
  echo ">> Bot-only deployment"
  docker rm -f compost-bot || true
  docker compose build bot
  docker compose up -d --no-deps bot
fi
