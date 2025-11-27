FROM azul/zulu-openjdk-alpine:25-latest

RUN apk add --no-cache libstdc++

WORKDIR /app
COPY ./bin/hub/build/libs/hub-all.jar /app/server.jar

EXPOSE 25565
CMD ["java", "-XX:+UseSerialGC", "-Xms1000M", "-Xmx1000M", "-XX:ActiveProcessorCount=4", "-jar", "server.jar"]
