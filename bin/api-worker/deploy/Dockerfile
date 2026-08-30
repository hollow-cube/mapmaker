FROM azul/zulu-openjdk-alpine:25-latest

USER 65532:65532

WORKDIR /app
COPY ./bin/api-worker/build/libs/api-worker-all.jar /app/api-worker.jar

# Structured logs in the cluster; logback-local.xml (plain text) is what a local run picks up. The heap
# follows the container limit the chart sets. Nothing probes this process, so an out-of-memory has
# to end it for kubernetes to restart it.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-Dlogback.configurationFile=logback-prod.xml", "-jar", "/app/api-worker.jar"]
