#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "=== 线上部署（Docker）==="
docker compose down 2>/dev/null || true
docker compose up -d --build

echo ""
echo "部署完成！"
echo "  前端 + API: http://服务器IP:3000"
echo "  数据库文件: Docker volume user-data"
echo ""
echo "常用命令:"
echo "  查看日志: docker compose logs -f"
echo "  停止服务: docker compose down"
