FROM azul/zulu-openjdk-alpine:25-latest

WORKDIR /app
# Everything velocity runs on: proxy/ (config, via jars, icon), the pinned velocity jar and the
# plugin, as assembled by `./gradlew :bin:proxy-plugin:stageProxy`. The forwarding secret is not
# in here; the chart mounts it at /app/forwarding.secret.
COPY ./bin/proxy-plugin/build/proxy/stage /app

EXPOSE 25565 9124
CMD ["java", "-XX:+UseG1GC", "-XX:G1HeapRegionSize=4M", "-XX:+UnlockExperimentalVMOptions", "-XX:+ParallelRefProcEnabled", "-XX:+AlwaysPreTouch", "-XX:MaxInlineLevel=15", "-Xmx768m", "-Dvelocity.max-plugin-message-payload-size=1048576", "-jar", "velocity.jar"]
