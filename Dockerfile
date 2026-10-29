# ケーキ通販Webアプリケーション Dockerfile
# OpenJDK 21 を使用した軽量構成
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app
COPY src/ ./src/
COPY static/ ./static/
COPY build.sh ./

RUN chmod +x build.sh && ./build.sh

# 実行環境
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=builder /app/app.jar ./
COPY --from=builder /app/static/ ./static/
COPY run.sh ./
RUN chmod +x run.sh

# ポート 80 を公開
EXPOSE 80

# サーバー起動 (0.0.0.0:80)
ENTRYPOINT ["./run.sh", "80"]
