FROM azul/zulu-openjdk-alpine:25-latest

USER 65532:65532

WORKDIR /app
COPY ./bin/api-worker/build/libs/api-worker-all.jar /app/api-worker.jar

ENV LOG_FORMAT=json

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-XX:-UseBASE64Intrinsics", "-jar", "/app/api-worker.jar"]
