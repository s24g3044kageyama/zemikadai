#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -f "app.jar" ]; then
    echo "app.jar が見つかりません。先にビルドを実行します..."
    ./build.sh
fi

PORT="${1:-80}"
echo "ケーキ通販Webアプリを起動します (Port: $PORT)..."
exec java -jar app.jar "$PORT"
