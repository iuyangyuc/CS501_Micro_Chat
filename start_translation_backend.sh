#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: ./start_translation_backend.sh

Builds (if needed) and starts the Java translation backend located in backend_java/.
Set OPENAI_API_KEY (and optional overrides) in backend_java/.env or your environment before running.
Pass -h or --help to show this message.
USAGE
}

if [[ ${1:-} == "-h" || ${1:-} == "--help" ]]; then
  usage
  exit 0
fi

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
backend_dir="$script_dir/backend_java"

if [ ! -f "$backend_dir/.env" ] && [ -z "${OPENAI_API_KEY:-}" ]; then
  echo "[start] Warning: backend_java/.env not found and OPENAI_API_KEY env var not set. Copy .env.example and add your key." >&2
fi

echo "[start] Building and launching Java translation backend..."
(
  cd "$backend_dir"
  ./mvnw exec:java
)
