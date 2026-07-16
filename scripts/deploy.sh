#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PULL_CODE=1
SHOW_LOGS=0

usage() {
  cat <<'EOF'
Usage: ./scripts/deploy.sh [--no-pull] [--logs]

Starts or restarts the full FRCS stack with Docker Compose.

Options:
  --no-pull   Do not run git pull before rebuilding containers.
  --logs      Tail backend logs after the stack is started.
  -h,--help   Show this help message.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-pull)
      PULL_CODE=0
      shift
      ;;
    --logs)
      SHOW_LOGS=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed. Install Docker first, then rerun this script." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose v2 is not available. Install the Docker Compose plugin first." >&2
  exit 1
fi

if [[ ! -f .env ]]; then
  cat >&2 <<'EOF'
Missing .env.

First-time setup:
  cp .env.example .env
  nano .env

Set your model API values in .env, then rerun:
  ./scripts/deploy.sh
EOF
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

if [[ "${CRAWLER_DETAIL_URL:-}" == "http://host.docker.internal:9001/crawl.json" || "${CRAWLER_DETAIL_URL:-}" == "http://127.0.0.1:9001/crawl.json" ]]; then
  echo "Ignoring legacy CRAWLER_DETAIL_URL=${CRAWLER_DETAIL_URL}; using built-in crawler bridge."
  unset CRAWLER_DETAIL_URL
fi

if [[ "${CRAWLER_SEARCH_US_URL:-}" == "http://host.docker.internal:9003/crawl.json" || "${CRAWLER_SEARCH_US_URL:-}" == "http://127.0.0.1:9003/crawl.json" ]]; then
  echo "Ignoring legacy CRAWLER_SEARCH_US_URL=${CRAWLER_SEARCH_US_URL}; using built-in US crawler bridge."
  unset CRAWLER_SEARCH_US_URL
fi

if [[ "$PULL_CODE" == "1" && -d .git ]]; then
  echo "Pulling latest code..."
  git pull --ff-only
fi

echo "Building and starting containers..."
docker compose up -d --build --remove-orphans

WEB_PORT="${WEB_PORT:-80}"
PUBLIC_URL="${SERVER_PUBLIC_URL:-http://120.26.60.104}"

if command -v curl >/dev/null 2>&1; then
  echo "Waiting for web service on local port ${WEB_PORT}..."
  for _ in $(seq 1 30); do
    if curl -fsS "http://127.0.0.1:${WEB_PORT}/" >/dev/null 2>&1; then
      break
    fi
    sleep 2
  done
fi

docker compose ps

if [[ "${SEED_DEMO_KB:-1}" == "1" ]]; then
  ./scripts/seed-kb.sh "http://127.0.0.1:${WEB_PORT}"
fi

cat <<EOF

FRCS stack is running.
Open: ${PUBLIC_URL}

Useful commands:
  ./scripts/deploy.sh              # pull latest code, rebuild, restart
  ./scripts/deploy.sh --no-pull    # rebuild and restart local code only
  ./scripts/deploy.sh --logs       # restart and follow backend logs
  docker compose logs -f backend
EOF

if [[ "$SHOW_LOGS" == "1" ]]; then
  docker compose logs -f backend
fi
