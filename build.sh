#!/usr/bin/env bash
set -e

echo "=== ケーキ通販Webアプリ ビルド開始 ==="
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

mkdir -p build

echo "Javaソースコードをコンパイル中..."
javac -d build $(find src/main/java -name "*.java")

echo "JARファイルの作成中..."
jar cfe app.jar com.sweets.cake.Main -C build .

echo "=== ビルドが正常に完了しました (app.jar を生成) ==="
