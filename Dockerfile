# 多阶段构建：Render / Docker 部署（仅 API，前端在 Netlify）
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

RUN apk add --no-cache curl

COPY src ./src

RUN mkdir -p lib out && \
    curl -L -o lib/sqlite-jdbc-3.49.1.0.jar \
      https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.49.1.0/sqlite-jdbc-3.49.1.0.jar && \
    curl -L -o lib/gson-2.11.0.jar \
      https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar && \
    javac -encoding UTF-8 \
      -cp lib/sqlite-jdbc-3.49.1.0.jar:lib/gson-2.11.0.jar \
      -d out \
      src/sqlite/User.java \
      src/sqlite/UserDao.java \
      src/sqlite/ApiResponse.java \
      src/sqlite/UserApiServer.java && \
    jar cfe user-api.jar sqlite.UserApiServer -C out .

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /build/user-api.jar /app/user-api.jar
COPY --from=builder /build/lib /app/lib

RUN mkdir -p /app/data

ENV DB_PATH=/app/data/demo.db
ENV ALLOWED_ORIGINS=https://gregarious-cendol-a566b0.netlify.app

EXPOSE 3000
VOLUME ["/app/data"]

CMD ["sh", "-c", "java -cp /app/user-api.jar:/app/lib/* sqlite.UserApiServer"]
