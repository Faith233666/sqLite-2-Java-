#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

mkdir -p out data lib

SQLITE_JAR="lib/sqlite-jdbc-3.49.1.0.jar"
GSON_JAR="lib/gson-2.11.0.jar"

if [ ! -f "$SQLITE_JAR" ]; then
  echo "下载 SQLite JDBC..."
  curl -L -o "$SQLITE_JAR" https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.49.1.0/sqlite-jdbc-3.49.1.0.jar
fi

if [ ! -f "$GSON_JAR" ]; then
  echo "下载 Gson..."
  curl -L -o "$GSON_JAR" https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar
fi

echo "编译 Java..."
javac -encoding UTF-8 -cp "$SQLITE_JAR:$GSON_JAR" -d out \
  src/sqlite/User.java \
  src/sqlite/UserDao.java \
  src/sqlite/ApiResponse.java \
  src/sqlite/UserApiServer.java

jar cfe user-api.jar sqlite.UserApiServer -C out .

echo "构建完成: user-api.jar"
echo "启动命令: DB_PATH=./data/demo.db WEB_ROOT=./web java -cp user-api.jar:lib/* sqlite.UserApiServer 3000"
