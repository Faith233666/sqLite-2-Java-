#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

bash deploy/build.sh

export DB_PATH="$ROOT/data/demo.db"
export WEB_ROOT="$ROOT/web"
export PORT="${PORT:-8080}"

echo "启动服务: http://0.0.0.0:$PORT"
exec java -cp "user-api.jar:lib/*" sqlite.UserApiServer "$PORT"
