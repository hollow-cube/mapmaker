FROM azul/zulu-openjdk-alpine:25-latest

WORKDIR /app
COPY ./bin/map/build/libs/map-all.jar /app/server.jar

EXPOSE 25565
CMD ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-Xms6000M", "-Xmx6000M", "-XX:ActiveProcessorCount=4", "-jar", "server.jar"]
